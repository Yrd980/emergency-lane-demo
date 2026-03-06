# AGENTS.md

## 项目概览

这是一个用于毕设演示的“高速公路应急车道违章辅助举报原型”。

- 后端：`uv + FastAPI`
- 前端：`Vite + TypeScript`
- 架构：前后端分离
- AI 能力：统一通过智谱 API，由后端调用
- 演示模式：连续播放视频 + 关键帧抽样分析 + 时序融合 + 证据链 + 模拟举报

## 目录约定

以当前仓库根目录为准：

- 后端有效源码：`backend/src/app`
- 前端有效源码：`frontend/src`
- 已提交的演示素材：`data/`
- 运行时生成文件：`storage/`

注意：

- `backend/src/app` 是当前后端唯一可信源码目录。
- 不要把新逻辑写到 `backend/app`；该目录不是当前主入口。
- 不要修改 `backend/.venv`、`frontend/node_modules`。
- `storage/` 是运行期产物，不要手动编辑，也不要提交新增内容。

## 当前系统边界

当前原型已经实现的核心闭环：

1. 点击前端按钮触发演示视频分析；
2. 后端对演示视频按时间间隔抽取关键帧；
3. 后端调用智谱视觉模型判断是否占用应急车道，并读取车牌；
4. 本地用时序规则把多帧结果融合为单个违章事件；
5. 自动生成证据图；
6. 前端展示大屏概览、事件列表、移动端详情和模拟举报结果。

继续开发时，优先保持这个闭环，不要把需求扩到真实执法平台接入、复杂权限系统或原生 App。

## 后端约定

- 启动目录：`backend`
- 运行命令：
  - `uv run uvicorn src.app.main:app --host 127.0.0.1 --port 8000 --reload`
- 环境变量模板：`backend/.env.example`
- 智谱接入位置：`backend/src/app/services/zhipu_client.py`
- 视频分析主流程：`backend/src/app/services/analyzer.py`
- 演示视频生成逻辑：`backend/src/app/services/demo_assets.py`
- API 主入口：`backend/src/app/main.py`

实现时请遵守：

- 智谱相关能力统一从后端走，不要在前端直连智谱。
- 若需要调整演示素材，优先改 `demo_assets.py` 重新生成，不要直接手改二进制视频。
- 若本地没有真实智谱 Key，优先使用 `.env` 中的 `USE_MOCK_ZHIPU=true` 进行演示和验证。
- Windows 终端下若遇到中文输出乱码，优先设置 `PYTHONUTF8=1`。

## 前端约定

- 启动目录：`frontend`
- 开发命令：`npm run dev`
- 构建命令：`npm run build`
- API 基地址配置：`frontend/src/config.ts`
- 页面主逻辑：`frontend/src/main.ts`

实现时请遵守：

- 维持前后端分离，不要把后端模板直接塞进前端目录。
- 保留以下 4 个核心展示区：
  - 政府大屏风格概览
  - 事件列表
  - 事件详情 / 移动端预览
  - 触发演示分析按钮
- 接口字段如需调整，前后端一起改，避免只改一侧。

## API 约定

当前前端依赖以下接口：

- `GET /api/overview`
- `GET /api/events`
- `GET /api/events/{id}`
- `POST /api/events/{id}/report`
- `POST /api/tasks/analyze-demo`

若修改这些接口：

- 先确认 `frontend/src/types.ts`
- 再同步更新 `frontend/src/api.ts`
- 最后确认 `frontend/src/main.ts`

## 验证方式

优先做轻量验证：

- 后端健康检查：`GET /api/health`
- 后端烟测：用 `fastapi.testclient.TestClient` 调 `overview / events / report / analyze-demo`
- 前端验证：`npm run build`

如果只是改文案或样式，不必额外扩大测试范围。

## Git 约定

这个仓库适合按模块拆提交，优先保持以下粒度：

- 后端能力
- 前端界面
- 演示素材 / 文档

除非用户明确要求，否则不要自动改历史提交信息。

## 修改原则

- 优先做最小、明确、可演示的改动。
- 保持“毕设原型”定位，不要过度工程化。
- 修问题时优先修根因，不要堆临时补丁。
- 若需要增加工作量感，优先补强：
  - 时序分析说明
  - 证据链展示
  - 大屏统计维度
  - 报告与答辩材料

