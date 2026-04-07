# 高速公路应急车道违章辅助举报双端项目

这是一个面向毕设演示的“双端协同”项目：

- `backend/`：`uv + FastAPI`，负责视频分析、事件生成、案件归档、证据链与任务历史。
- `frontend/`：Vite + TypeScript，负责 run 总览、事件/案件筛选、证据链展示、案件人工复核编辑。
- `android/`：Kotlin + Compose，负责移动端 run 摘要、案件筛选、案件复核信息查看与更新。
- `data/`：演示视频、封面图与 manifest。
- `storage/`：运行时生成的关键帧、证据图、证据片段、报告文本与 SQLite 数据库。

## 当前项目能力

当前仓库已经具备完整的可演示闭环：

1. 选择本地演示视频 source；
2. 发起一次 analysis run；
3. 后端按时间间隔抽取关键帧；
4. 调用智谱视觉模型判断是否占用应急车道并读取车牌；
5. 通过时序融合生成事件；
6. 按车牌归档为案件；
7. 生成证据图、15 秒证据片段与正式举报文书；
8. Web 与 Android 共享同一套 run / event / case 数据；
9. 支持人工复核案件并联动举报状态流转。

## 毕设交付定位

当前仓库建议作为毕设主系统：

- **主线**：`backend + frontend + android` 的双端协同闭环；
- **参考来源**：`newnew` 作为 Android 原型 / 算法思路来源，用于说明项目演进；
- **当前重点**：补齐可操作功能，不优先做 README 卡片化介绍或 CameraX/JNI 并轨。

因此本阶段**不要求**把 `newnew` 的 CameraX / JNI / HyperLPR / Room 本地链路 1:1 并入主系统，而是保留其作为设计演进参考。

## 这次补齐后的重点功能

### 1. Analysis run 管理
- 支持多个 analysis run 历史保留
- 每个 run 记录 `running / done / failed`
- 每个 run 关联 source video
- 默认不清空历史结果

### 2. 数据源选择
- `GET /api/sources` 返回 `data/` 下可用视频源
- `POST /api/tasks/analyze-demo` 支持 `source_name`
- 当前仓库已内置两个 demo source：
  - `demo_highway.mp4`
  - `demo_highway_alt.mp4`

### 3. 案件人工复核工作流
- case 支持字段：
  - `corrected_plate_number`
  - `operator_note`
  - `review_status`
- case 状态流转：
  - `待复核`
  - `待举报`
  - `已举报`
- 报告/举报动作与 case 状态联动

### 4. 事件 / 案件筛选
- `/api/events`
- `/api/cases`

支持按以下字段过滤：
- `status`
- `plate`
- `run_id`
- `review_status`

### 5. Web / Android 协同
- Web：source selector、run panel、事件/案件筛选、case 编辑表单
- Android：run 摘要、案件状态筛选、case review 信息展示与更新

## 启动后端

```bash
cd backend
cp .env.example .env
```

没有真实智谱 Key 时，可保留：

```env
USE_MOCK_ZHIPU=true
```

启动：

```bash
cd backend
uv run uvicorn src.app.main:app --host 127.0.0.1 --port 8000 --reload
```

## 启动网页端

```bash
cd frontend
npm install
npm run dev
```

前端默认访问 `http://127.0.0.1:8000/api`。

## 启动 Android 端

```bash
cd android
./gradlew assembleDebug
```

说明：
- `BuildConfig.API_BASE_URL` 默认是 `http://10.0.2.2:8000/api/`，适合 Android 模拟器连接本机后端；
- 若用真机，请把 `android/app/build.gradle.kts` 中的地址改为局域网 IP；
- 本地构建时可创建 `android/local.properties` 指向 SDK，例如 `sdk.dir=/home/yrd/Android/Sdk`。

## 关键接口

### 基础与总览
- `GET /api/health`
- `GET /api/overview`
- `GET /api/sources`

### run
- `GET /api/runs`
- `GET /api/runs/{run_id}`

### events
- `GET /api/events`
- `GET /api/events/{id}`
- `POST /api/events/{id}/report`

### cases
- `GET /api/cases`
- `GET /api/cases/{id}`
- `PATCH /api/cases/{id}`
- `POST /api/cases/{id}/report`

### tasks
- `POST /api/tasks/analyze-demo`

## 验证

### 后端
```bash
cd backend
uv run python -m unittest discover -s tests -v
```

### 前端
```bash
cd frontend
npm run build
```

### Android
```bash
cd android
./gradlew --no-daemon assembleDebug
```

## 文档

- `docs/architecture-dual-end.md`：双端项目架构与职责分工
- `docs/project-walkthrough.md`：项目运行流程与演示路径
- `docs/source-newnew-digest.md`：参考工程 `newnew` 文档提炼
- `docs/implementation-scope.md`：当前项目实现范围与模块拆解
