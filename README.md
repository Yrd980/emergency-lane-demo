# 高速公路应急车道违章辅助举报 — 双端独立演示项目

本项目包含两个**独立**的演示路径，互不依赖，各自闭环。

## 项目结构

```
├── backend/    FastAPI Mock 后端 — 合成公路视频 + 纯 Mock 视觉推理
├── frontend/   Web 演示端 — 总览 / 事件 / 案件 + 证据链展示
├── android/    Android 端侧本地检测演示 — 真机 CameraX + YOLOv8 + HyperLPR3
├── data/       演示视频 manifest、封面图
└── storage/    运行时生成的关键帧、证据图、clip、报告、SQLite
```

## 两条独立演示路径

### 路径 A：Web 端纯 Mock 原型（`backend/` + `frontend/`）

从合成公路视频生成 → 纯 Mock 视觉推理 → 事件 → 案件 → 证据链 → 模拟举报。全程**零外部 API 依赖**，所有数据由 Pillow 合成渲染和基于 manifest 的确定性 mock 结果产生。

Web 端演示目的：**说明"如果有了检测结果，系统会如何流转"** — 展示从视频输入到案件输出、人工复核、证据链生成、模拟举报的完整产品闭环。

**启动：**

```bash
# Backend
cd backend
cp .env.example .env
uv run uvicorn src.app.main:app --host 127.0.0.1 --port 8000 --reload

# Frontend
cd frontend
npm install
npm run dev
```

前端默认访问 `http://127.0.0.1:8000/api`。

### 路径 B：Android 端侧本地检测演示（`android/`）

在手机上独立运行，包含两条链路：

- **本地检测链**：`CameraX` 实时采集 → `JNI + ncnn` 跑 `YOLOv8n` 目标检测 → `HyperLPR3` 车牌识别 → `Room` 本地数据库归档
- **本地演示 UI**（Jetpack Compose）：概览 / 案件 / 本地检测三个 Tab，使用 APK 内置的离线素材（`R.raw` 视频 + `assets/` 证据图 + `local_demo_samples`）进行独立演示

Android 端演示目的：**在真机上证明"能检测"** — 展示 CameraX 实时画面、YOLO 目标检测推理、HyperLPR3 车牌识别、本地 Room 案件归档的完整端侧闭环。

**构建与安装：**

```bash
cd android
JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew --no-daemon assembleDebug

# 真机安装
ANDROID_SERIAL=<adb-device-id> android/scripts/install-debug.sh
```

**本地素材：**
- `res/raw/`：`case_demo_source.mp4`、`case_demo_clip.mp4`、`newnew_demo_clip.mp4`
- `assets/case_demo_evidence/`：3 张案件证据图
- `assets/local_demo_samples/`：4 张端侧检测样例（正常/违规各 2 张）
- `assets/yolov8n.bin` + `yolov8n.param`：端侧 YOLOv8n 模型文件

### 两者关系

- Web 端和 Android 端**不需要打通** — 各自独立演示各自环节
- Web 端的 mock 案件数据和 Android 端的端侧检测结果**彼此无关**
- 两者共用相同的产品故事线：视频输入 → 检测识别 → 案件归档 → 复核 → 举报

## 关键接口

### 基础与总览
- `GET /api/health`
- `GET /api/overview`
- `GET /api/sources`

### Run
- `GET /api/runs`
- `GET /api/runs/{run_id}`

### Events
- `GET /api/events`
- `GET /api/events/{id}`
- `POST /api/events/{id}/report`

### Cases
- `GET /api/cases`
- `GET /api/cases/{id}`
- `PATCH /api/cases/{id}`
- `POST /api/cases/{id}/report`

### Tasks
- `POST /api/tasks/analyze-demo`

### Device Compatibility (端侧导入占位)
- `POST /api/device/cases/import`
- `GET /api/device/sync-snapshot`
- `GET /api/device/cases/{id}`

## 验证

```bash
# 后端
cd backend
uv run python -m unittest discover -s tests -v

# 前端
cd frontend
npm run build

# Android
cd android
JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew --no-daemon assembleDebug
```
