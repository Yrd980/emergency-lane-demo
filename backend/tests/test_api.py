from __future__ import annotations

import shutil
import unittest

from fastapi.testclient import TestClient

from src.app import db
from src.app.config import settings
from src.app.main import app


class ApiSmokeTest(unittest.TestCase):
    def setUp(self) -> None:
        db.init_db()
        db.reset_demo_data()
        for directory in (settings.frame_dir, settings.evidence_dir, settings.clip_dir, settings.report_dir):
            if directory.exists():
                shutil.rmtree(directory)
            directory.mkdir(parents=True, exist_ok=True)
        self.client = TestClient(app)

    def test_health_sources_runs_and_case_review_flow(self) -> None:
        health = self.client.get("/api/health")
        self.assertEqual(health.status_code, 200)
        self.assertEqual(health.json()["status"], "ok")

        sources = self.client.get("/api/sources")
        self.assertEqual(sources.status_code, 200)
        source_list = sources.json()
        self.assertGreaterEqual(len(source_list), 2)
        source_names = [item["name"] for item in source_list]
        self.assertIn("demo_highway.mp4", source_names)
        self.assertIn("demo_highway_alt.mp4", source_names)

        analyze_a = self.client.post("/api/tasks/analyze-demo", json={"source_name": "demo_highway.mp4"})
        self.assertEqual(analyze_a.status_code, 200)
        payload_a = analyze_a.json()
        self.assertGreaterEqual(payload_a["events_created"], 1)
        self.assertGreaterEqual(payload_a["cases_created"], 1)
        run_a = payload_a["run_id"]

        analyze_b = self.client.post("/api/tasks/analyze-demo", json={"source_name": "demo_highway_alt.mp4"})
        self.assertEqual(analyze_b.status_code, 200)
        payload_b = analyze_b.json()
        self.assertGreaterEqual(payload_b["events_created"], 1)
        self.assertGreaterEqual(payload_b["cases_created"], 1)
        run_b = payload_b["run_id"]
        self.assertNotEqual(run_a, run_b)

        runs = self.client.get("/api/runs")
        self.assertEqual(runs.status_code, 200)
        run_list = runs.json()
        self.assertGreaterEqual(len(run_list), 2)
        self.assertEqual({item["id"] for item in run_list[:2]}, {run_a, run_b})

        run_detail = self.client.get(f"/api/runs/{run_a}")
        self.assertEqual(run_detail.status_code, 200)
        self.assertEqual(run_detail.json()["id"], run_a)
        self.assertTrue(run_detail.json()["events"])
        self.assertTrue(run_detail.json()["cases"])

        overview = self.client.get("/api/overview")
        self.assertEqual(overview.status_code, 200)
        overview_payload = overview.json()
        self.assertGreaterEqual(len(overview_payload["runs"]), 2)
        self.assertGreaterEqual(len(overview_payload["sources"]), 2)

        cases_a = self.client.get("/api/cases", params={"run_id": run_a})
        self.assertEqual(cases_a.status_code, 200)
        case_list_a = cases_a.json()
        self.assertTrue(case_list_a)
        case_id = case_list_a[0]["id"]
        self.assertEqual(case_list_a[0]["status"], "待复核")
        self.assertEqual(case_list_a[0]["review_status"], "待复核")

        events_a = self.client.get("/api/events", params={"run_id": run_a, "review_status": "待复核"})
        self.assertEqual(events_a.status_code, 200)
        event_list_a = events_a.json()
        self.assertTrue(event_list_a)
        event_id = event_list_a[0]["id"]

        case_detail = self.client.get(f"/api/cases/{case_id}")
        self.assertEqual(case_detail.status_code, 200)
        case_payload = case_detail.json()
        self.assertTrue(case_payload["events"])
        self.assertTrue(case_payload["report_content"])
        self.assertEqual(case_payload["run_id"], run_a)

        patch_case = self.client.patch(
            f"/api/cases/{case_id}",
            json={
                "corrected_plate_number": "沪A12345-A",
                "operator_note": "人工复核通过，准备举报",
                "review_status": "复核通过",
            },
        )
        self.assertEqual(patch_case.status_code, 200)
        patched_case = patch_case.json()
        self.assertEqual(patched_case["corrected_plate_number"], "沪A12345-A")
        self.assertEqual(patched_case["operator_note"], "人工复核通过，准备举报")
        self.assertEqual(patched_case["review_status"], "复核通过")
        self.assertEqual(patched_case["status"], "待举报")

        ready_cases = self.client.get("/api/cases", params={"status": "待举报", "review_status": "复核通过"})
        self.assertEqual(ready_cases.status_code, 200)
        self.assertIn(case_id, {item["id"] for item in ready_cases.json()})

        plate_search = self.client.get("/api/cases", params={"plate": "12345-A"})
        self.assertEqual(plate_search.status_code, 200)
        self.assertIn(case_id, {item["id"] for item in plate_search.json()})

        report = self.client.post(f"/api/cases/{case_id}/report")
        self.assertEqual(report.status_code, 200)
        self.assertEqual(report.json()["status"], "已举报")

        reported_case = self.client.get(f"/api/cases/{case_id}")
        self.assertEqual(reported_case.status_code, 200)
        self.assertEqual(reported_case.json()["status"], "已举报")
        self.assertIsNotNone(reported_case.json()["reported_at"])

        reported_events = self.client.get("/api/events", params={"status": "已举报", "run_id": run_a})
        self.assertEqual(reported_events.status_code, 200)
        self.assertIn(event_id, {item["id"] for item in reported_events.json()})

    def test_report_requires_review_approval(self) -> None:
        analyze = self.client.post("/api/tasks/analyze-demo", json={"source_name": "demo_highway.mp4"})
        self.assertEqual(analyze.status_code, 200)
        case_id = self.client.get("/api/cases").json()[0]["id"]
        report = self.client.post(f"/api/cases/{case_id}/report")
        self.assertEqual(report.status_code, 409)
        self.assertIn("复核通过", report.text)

    def test_patch_case_rejects_inconsistent_review_and_status(self) -> None:
        analyze = self.client.post("/api/tasks/analyze-demo", json={"source_name": "demo_highway.mp4"})
        self.assertEqual(analyze.status_code, 200)
        case_id = self.client.get("/api/cases").json()[0]["id"]

        response = self.client.patch(
            f"/api/cases/{case_id}",
            json={"status": "待复核", "review_status": "复核通过"},
        )

        self.assertEqual(response.status_code, 400)
        self.assertIn("复核通过后状态不能仍为待复核", response.text)

    def test_analyze_demo_rejects_unknown_source(self) -> None:
        response = self.client.post("/api/tasks/analyze-demo", json={"source_name": "missing.mp4"})

        self.assertEqual(response.status_code, 404)
        self.assertIn("未找到视频源", response.text)

    def test_report_event_rejects_case_before_review_approval(self) -> None:
        analyze = self.client.post("/api/tasks/analyze-demo", json={"source_name": "demo_highway.mp4"})
        self.assertEqual(analyze.status_code, 200)
        event_id = self.client.get("/api/events").json()[0]["id"]

        response = self.client.post(f"/api/events/{event_id}/report")

        self.assertEqual(response.status_code, 409)
        self.assertIn("尚未复核通过", response.text)

    def test_device_case_import_and_snapshot(self) -> None:
        response = self.client.post(
            "/api/device/cases/import",
            json={
                "device_label": "pixel-local-mode",
                "items": [
                    {
                        "client_case_id": "local-case-001",
                        "plate_number": "沪A12345",
                        "corrected_plate_number": "沪A12345",
                        "review_status": "复核通过",
                        "status": "待举报",
                        "operator_note": "端侧相机抓拍导入",
                        "summary": "本地 CameraX + JNI 检测到疑似占用应急车道",
                        "location": "G2 京沪高速",
                        "clip_uri": "content://clips/local-case-001",
                        "report_text": "本地生成报告摘要",
                        "created_at": "2026-04-10T10:00:00+00:00",
                        "updated_at": "2026-04-10T10:05:00+00:00",
                        "evidence": [
                            {
                                "label": "抓拍 1",
                                "image_uri": "content://images/local-case-001-1",
                                "captured_at": "2026-04-10T10:00:10+00:00",
                                "note": "车牌已识别",
                            }
                        ],
                    }
                ],
            },
        )

        self.assertEqual(response.status_code, 200)
        payload = response.json()
        self.assertEqual(payload["imported_cases"], 1)
        self.assertEqual(payload["imported_evidence"], 1)

        snapshot = self.client.get("/api/device/sync-snapshot")
        self.assertEqual(snapshot.status_code, 200)
        snapshot_payload = snapshot.json()
        self.assertEqual(snapshot_payload["summary"]["device_case_count"], 1)
        self.assertEqual(snapshot_payload["summary"]["device_evidence_count"], 1)
        self.assertEqual(snapshot_payload["summary"]["device_labels"], ["pixel-local-mode"])
        case_id = snapshot_payload["cases"][0]["id"]
        self.assertEqual(snapshot_payload["cases"][0]["source_mode"], "local-device")

        detail = self.client.get(f"/api/device/cases/{case_id}")
        self.assertEqual(detail.status_code, 200)
        detail_payload = detail.json()
        self.assertEqual(detail_payload["device_label"], "pixel-local-mode")
        self.assertEqual(len(detail_payload["evidence"]), 1)

    def test_device_case_import_rejects_empty_items(self) -> None:
        response = self.client.post("/api/device/cases/import", json={"device_label": "empty", "items": []})

        self.assertEqual(response.status_code, 400)
        self.assertIn("至少导入一个端侧案件", response.text)


if __name__ == "__main__":
    unittest.main()
