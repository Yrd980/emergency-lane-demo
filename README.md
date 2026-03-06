# 高速公路应急车道违章辅助举报原型

一个面向毕设演示的最小可交付原型：

- `backend/`：`uv + FastAPI` 后端，负责生成演示视频、调用智谱多模态模型、形成证据链、提供举报接口。
- `frontend/`：Vite + TypeScript 前端，包含大屏概览、事件列表、移动端详情/举报预览。
- `data/`：已生成的演示视频、封面图与样例清单，可直接用于答辩演示。

## 方案说明

视频处理不是逐帧全量上云，而是：

1. 连续播放演示视频；
2. 后端按固定时间间隔抽取关键帧；
3. 使用智谱视觉模型识别“是否占用应急车道 + 车牌号”；
4. 再用本地时序融合规则把连续关键帧合成为一个完整违章事件；
5. 自动保存 3 张关键证据图，并生成模拟举报记录。

这个设计更像毕业设计原型，而不是简单“看一张图”。

## 后端启动

```powershell
cd backend
Copy-Item .env.example .env
```

把 `.env` 里的 `ZHIPU_API_KEY` 改成你自己的智谱 Key。

如果先只想本地演示，可保留：

```env
USE_MOCK_ZHIPU=true
```

启动后端：

```powershell
cd backend
uv run uvicorn src.app.main:app --host 127.0.0.1 --port 8000 --reload
```

## 前端启动

```powershell
cd frontend
npm install
npm run dev
```

默认前端会访问：

```text
http://127.0.0.1:8000/api
```

## 演示流程

1. 打开前端首页；
2. 点击“重新分析演示视频”；
3. 后端生成或读取演示视频；
4. 智谱/模拟模式识别占用应急车道车辆与车牌；
5. 前端展示：
   - 大屏概览；
   - 疑似违章列表；
   - 证据图片；
   - 模拟举报结果。

## 目录结构

```text
emergency-lane-demo/
├─ backend/
│  ├─ pyproject.toml
│  └─ src/app/
├─ frontend/
│  ├─ src/
│  └─ package.json
├─ data/
│  ├─ demo_highway.mp4
│  ├─ demo_cover.jpg
│  └─ demo_manifest.json
└─ storage/
```

