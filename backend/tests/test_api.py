from __future__ import annotations

import unittest

from fastapi.testclient import TestClient

from src.app.main import app


class ApiSmokeTest(unittest.TestCase):
    def setUp(self) -> None:
        self.client = TestClient(app)

    def test_health_and_analysis_case_flow(self) -> None:
        health = self.client.get("/api/health")
        assert health.status_code == 200
        assert health.json()["status"] == "ok"

        analyze = self.client.post("/api/tasks/analyze-demo")
        assert analyze.status_code == 200
        analyze_payload = analyze.json()
        assert analyze_payload["events_created"] >= 1
        assert analyze_payload["cases_created"] >= 1

        overview = self.client.get("/api/overview")
        assert overview.status_code == 200
        overview_payload = overview.json()
        assert overview_payload["summary"]["total_cases"] >= 1

        events = self.client.get("/api/events")
        assert events.status_code == 200
        event_list = events.json()
        assert event_list
        event_id = event_list[0]["id"]
        assert event_list[0]["case_id"]

        event_detail = self.client.get(f"/api/events/{event_id}")
        assert event_detail.status_code == 200
        assert event_detail.json()["evidence"]

        cases = self.client.get("/api/cases")
        assert cases.status_code == 200
        case_list = cases.json()
        assert case_list
        case_id = case_list[0]["id"]
        assert case_list[0]["clip_url"]

        case_detail = self.client.get(f"/api/cases/{case_id}")
        assert case_detail.status_code == 200
        case_payload = case_detail.json()
        assert case_payload["events"]
        assert case_payload["report_content"]

        report = self.client.post(f"/api/cases/{case_id}/report")
        assert report.status_code == 200
        assert report.json()["status"] == "已举报"


if __name__ == "__main__":
    unittest.main()
