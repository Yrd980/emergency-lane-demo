# 高速公路应急车道违章辅助举报双端项目

这是一个“双端协同”版本项目：

- `backend/`：`uv + FastAPI`，负责演示视频分析、关键帧抽样、事件生成、案件归档、15 秒证据片段与正式举报文书生成。
- `frontend/`：Vite + TypeScript 网页端，负责总览、事件列表、案件库、证据链与模拟举报展示。
- `android/`：Kotlin + Compose 移动端，直接连接当前后端，负责案件查看、片段播放、文书预览与移动端举报展示。
- `data/`：演示视频、封面图与清单。
- `storage/`：运行时生成的关键帧、证据图、证据片段、报告文本与 SQLite 数据库。

## 当前项目能力

当前仓库不是单纯“看一张图识别”，而是完整的项目闭环：

1. 连续播放演示视频；
2. 后端按时间间隔抽取关键帧；
3. 调用智谱视觉模型判断是否占用应急车道并读取车牌；
4. 通过本地时序融合生成事件；
5. 再按车牌/案件进行归档；
6. 自动生成 15 秒证据片段与正式举报文书；
7. 网页端与 Android 端展示同一套案件数据。

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

默认访问 `http://127.0.0.1:8000/api`。

## 启动 Android 端

```bash
cd android
./gradlew assembleDebug
```

说明：

- 当前 `BuildConfig.API_BASE_URL` 默认是 `http://10.0.2.2:8000/api/`，适合 Android 模拟器连接本机后端；
- 若用真机，请把 `android/app/build.gradle.kts` 中的地址改为你的局域网 IP；
- 本地构建时可创建 `android/local.properties` 指向 SDK，例如 `sdk.dir=/home/yrd/Android/Sdk`。

## 关键接口

- `GET /api/overview`
- `GET /api/events`
- `GET /api/events/{id}`
- `POST /api/events/{id}/report`
- `GET /api/cases`
- `GET /api/cases/{id}`
- `POST /api/cases/{id}/report`
- `POST /api/tasks/analyze-demo`

## 验证

- 后端烟测：

```bash
cd backend
uv run python -m unittest discover -s tests -v
```

- 前端构建：

```bash
cd frontend
npm run build
```

- Android 构建：

```bash
cd android
./gradlew --no-daemon assembleDebug
```

## 文档

- `docs/architecture-dual-end.md`：双端项目架构与职责分工
- `docs/project-walkthrough.md`：项目运行流程与串联说明
- `docs/source-newnew-digest.md`：参考工程 `newnew` 文档提炼
- `docs/implementation-scope.md`：当前项目实现范围与模块拆解