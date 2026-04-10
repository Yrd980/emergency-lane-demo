# AGENTS.md

## 项目概览

这是一个“高速公路应急车道违章辅助举报双端项目”。

当前仓库由三部分组成：

- 后端：`uv + FastAPI`
- 网页端：`Vite + TypeScript`
- Android 端：`Kotlin + Compose`

系统主链路围绕以下闭环展开：

1. 触发演示视频分析；
2. 后端按时间间隔抽取关键帧；
3. 后端统一调用视觉识别服务判断是否疑似占用应急车道，并尽量读取车牌；默认可走 mock 演示识别，不以第三方视觉服务为前提；
4. 本地用时序规则把多帧结果融合为事件；
5. 按车牌归档为案件；
6. 自动生成证据图、15 秒证据片段和正式举报文书；
7. 网页端与 Android 端共同展示同一套案件数据。

## 目录约定

以当前仓库根目录为准：

- 后端有效源码：`backend/src/app`
- 前端有效源码：`frontend/src`
- Android 有效源码：`android/app/src/main`
- 已提交的演示素材：`data/`
- 运行时生成文件：`storage/`
- 项目文档：`docs/`

注意：

- `backend/src/app` 是当前后端唯一可信源码目录。
- 不要把新逻辑写到 `backend/app`；该目录不是当前主入口。
- 不要修改 `backend/.venv`、`frontend/node_modules`。
- `storage/` 是运行期产物，不要手动编辑，也不要提交新增内容。
- `android/local.properties` 是本地环境文件，不要提交。
- `.localref/` 是本地参考软链目录，不要提交其内容。

## 当前系统边界

当前项目已经实现的核心闭环：

- 视频抽样分析
- 事件生成
- 案件归档
- 证据图生成
- 15 秒证据片段生成
- 正式举报文书生成
- 网页端案件展示
- Android 端案件查看与模拟举报

继续开发时，优先保持这个闭环，不要把需求扩到真实执法平台接入、复杂权限系统、账号体系或在线同步后台。

## 后端约定

- 启动目录：`backend`
- 运行命令：
  - `uv run uvicorn src.app.main:app --host 127.0.0.1 --port 8000 --reload`
- 环境变量模板：`backend/.env.example`
- 视觉识别服务入口：`backend/src/app/services/zhipu_client.py`（当前默认可走 mock 演示识别）
- 视频分析主流程：`backend/src/app/services/analyzer.py`
- 演示视频生成逻辑：`backend/src/app/services/demo_assets.py`
- API 主入口：`backend/src/app/main.py`

实现时请遵守：

- 视觉识别相关能力统一从后端走，不要在前端或 Android 端直连第三方识别服务。
- 若需要调整演示素材，优先改 `demo_assets.py` 重新生成，不要直接手改二进制视频。
- 默认开发与验证可直接使用 `.env` 中的 `USE_MOCK_ZHIPU=true`；只有在明确配置第三方视觉服务时才切换真实识别。
- Windows 终端下若遇到中文输出乱码，优先设置 `PYTHONUTF8=1`。

## 前端约定

- 启动目录：`frontend`
- 开发命令：`npm run dev`
- 构建命令：`npm run build`
- API 基地址配置：`frontend/src/config.ts`
- 页面主逻辑：`frontend/src/main.ts`

实现时请遵守：

- 维持前后端分离，不要把后端模板直接塞进前端目录。
- 保留以下核心展示区：
  - 项目总览
  - 事件列表
  - 案件库
  - 案件详情 / 证据链 / 片段 / 文书
  - 触发演示分析按钮
- 接口字段如需调整，前后端一起改，避免只改一侧。

## Android 约定

- 启动目录：`android`
- 构建命令：`./gradlew assembleDebug`
- 应用入口：`android/app/src/main/java/com/yrd/emergencylanemobile/MainActivity.kt`
- 默认后端地址：`BuildConfig.API_BASE_URL`

实现时请遵守：

- Android 端当前定位是“移动协同查看 / 举报辅助端”，主数据源来自当前 FastAPI。
- 如需真机联调，可调整 `API_BASE_URL` 到局域网地址。
- 不要在当前 Android 模块里引入与参考工程完全独立的另一套主业务模型。

## API 约定

当前客户端依赖以下接口：

- `GET /api/overview`
- `GET /api/events`
- `GET /api/events/{id}`
- `POST /api/events/{id}/report`
- `GET /api/cases`
- `GET /api/cases/{id}`
- `POST /api/cases/{id}/report`
- `POST /api/tasks/analyze-demo`

若修改这些接口：

- 先确认 `frontend/src/types.ts`
- 再同步更新 `frontend/src/api.ts`
- 最后确认 `frontend/src/main.ts` 与 Android DTO

## 验证方式

优先做轻量但完整的验证：

- 后端健康检查：`GET /api/health`
- 后端烟测：`uv run python -m unittest discover -s tests -v`
- 前端验证：`npm run build`
- Android 验证：`./gradlew --no-daemon assembleDebug`

如果只是改文案或文档，不必额外扩大测试范围。

## Git 约定

这个仓库适合按模块拆提交，优先保持以下粒度：

- 后端能力
- 前端界面
- Android 客户端
- 文档

除非用户明确要求，否则不要自动改历史提交信息。

## 修改原则

- 优先做最小、明确、可运行的改动。
- 保持“项目原型”定位，不要过度工程化。
- 修问题时优先修根因，不要堆临时补丁。
- 优先补强：
  - 时序分析说明
  - 案件归档表达
  - 证据链展示
  - 双端协同体验
  - 项目文档一致性