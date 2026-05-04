# 项目重建设计文档：算法类毕设方向

日期：2026-05-05 | 最后更新：2026-05-05

## 1. 定位与目标

本项目从"应用类"转向"算法类"，面向高速公路应急车道违规占用检测问题，构建基于移动端视觉感知的自动检测与证据生成方法。

### 论文四模块

1. **应急车道区域提取** — 车道线检测 + 几何约束
2. **车辆目标检测** — 轻量化目标检测模型（YOLOv8n）
3. **空间关系建模** — 车辆与车道区域的空间重叠计算
4. **时间一致性约束下的违规判定** — 滑动时间窗口 + 占用帧比例阈值

### 交付组合

- Python 算法核心（`algorithm/`）：完整 pipeline 实现 + 可视化
- 演示系统（`backend/` + `frontend/`）：合成视频生成 → 算法消费 → 单页结果展示
- Android 端侧验证（`android/`）：YOLOv8n + ncnn 推理性能基准测试

### 核心原则

- 不涉及真实数据集，评估以合成视频定性演示为主
- 算法代码可独立运行，不依赖后端/前端
- 所有改动聚焦"证明可行性"

---

## 2. 项目结构

```
emergency-lane-demo/
├── algorithm/                ← 新增：核心算法（论文四模块）
│   ├── lane_extraction/      ← 模块一：应急车道区域提取
│   ├── vehicle_detection/    ← 模块二：车辆目标检测
│   ├── spatial_reasoning/    ← 模块三：空间关系建模
│   ├── temporal_judgment/    ← 模块四：时间一致性违规判定
│   ├── pipeline.py           ← 编排器：串联四模块
│   └── visualize.py          ← 检测框/掩膜/时间轴可视化
│
├── backend/                  ← 重构：去掉 mock，只做数据层
│   └── app/
│       ├── synthesis/        ← 合成视频生成（Pillow）
│       ├── routes/           ← REST API（合成/分析/结果）
│       └── storage/          ← 视频+结果文件存储
│
├── frontend/                 ← 重写：单页 demo
│   └── src/
│       ├── main.ts           ← 单页应用入口
│       ├── api.ts            ← 后端通信
│       └── components/       ← 视频播放器 + 检测叠加 + 违规时间轴
│
├── android/                  ← 精简：仅 YOLO 推理基准
│   └── app/src/main/
│       ├── cpp/              ← 保留 JNI + ncnn YOLOv8n
│       └── java/.../         ← 替换为 benchmark-only UI
│
└── docs/                     ← 设计文档 + 论文相关
```

### 层次关系

```
algorithm/ ──(函数调用)──> backend/ ──(REST API)──> frontend/
    ↓ 独立可运行
    python -m algorithm.pipeline --video test.mp4

android/ ── 独立，仅跑 YOLO 基准
```

---

## 3. algorithm/ 设计

### 目录结构

```
algorithm/
├── __init__.py
├── pipeline.py                    # 编排器
├── visualize.py                   # 绘图工具
│
├── lane_extraction/               # 模块一
│   ├── __init__.py
│   ├── lane_detector.py           # 白色阈值 + Canny + HoughLinesP
│   ├── region_mask.py             # 固定几何掩膜（左1/4 + 斜线三角）
│   └── mask_visualizer.py         # 掩膜区域可视化（绿线 + 半透明覆盖）
│
├── vehicle_detection/             # 模块二
│   ├── __init__.py
│   ├── yolo_detector.py           # YOLOv8n 推理器（ultralytics/onnx）
│   └── postprocess.py             # NMS + 车辆类别过滤（car/bus/truck）
│
├── spatial_reasoning/             # 模块三
│   ├── __init__.py
│   └── overlap.py                 # 检测框与掩膜区域的空间重叠计算
│                                  # IoU + 中心点判定 + 重叠面积占比
│
└── temporal_judgment/             # 模块四
    ├── __init__.py
    └── violation_decider.py       # 滑动窗口 + 占用帧比例阈值
                                   # 窗口15s，占比>60%判定违规
```

### pipeline.py 执行流程

```python
def run(video_path, config):
    results = []
    for frame in read_frames(video_path):
        mask       = lane_extraction.apply(frame)          # 1. 区域提取
        detections = vehicle_detection.detect(mask)        # 2. 车辆检测
        violations = spatial_reasoning.check(detections, mask)  # 3. 空间判定
        results.append((frame_idx, mask, detections, violations))
    segments = temporal_judgment.decide(results)           # 4. 时间判定
    return results, segments
```

### 输入/输出

- 输入：合成视频路径 + 配置（置信度阈值、窗口大小、占用比例阈值）
- 输出：逐帧检测结果 + 违规时间段列表 `[(start_frame, end_frame, confidence), ...]`

### 设计要点

- 模块一采用 white threshold + Canny + HoughLinesP + 固定几何约束。在论文中定位为"先验知识引入"——高速公路应急车道的位置规律作为合理先验
- 模块四代码实现采用简单滑动窗口（15s 窗口，>60% 占用帧比例判定违规），论文中讨论更复杂方案（如 ByteTrack 跟踪 + 个体持续时长）的可行性和方向
- `finalize()` 处理视频总长不足窗口大小的边界情况，以可用帧比率判定
- pipeline 支持可选的 `ground_truth` 参数：对于合成视频（Pillow 绘制的矩形车辆无法被 YOLO 识别），使用合成时记录的 ground-truth 车辆位置作为检测输入；真实视频仍走 YOLO 检测路径
- 可视化负责：掩膜区域叠加、检测框绘制、时间轴图生成

---

## 4. backend/ 设计

### 目录结构

```
backend/
├── app/
│   ├── __init__.py
│   ├── main.py                     # FastAPI 应用入口
│   ├── config.py                   # 路径、阈值等配置
│   │
│   ├── synthesis/
│   │   ├── __init__.py
│   │   ├── highway_renderer.py     # Pillow 渲染高速路面
│   │   ├── vehicle_placer.py       # 车辆放置逻辑（正常车道 + 应急车道）
│   │   └── video_composer.py       # 帧合成 → mp4
│   │
│   ├── routes/
│   │   ├── __init__.py
│   │   ├── synthesis.py            # POST /api/synthesis
│   │   ├── analysis.py             # POST /api/analyze/{video_id}
│   │   └── results.py              # GET  /api/results/{video_id}
│   │
│   └── storage/
│       ├── __init__.py
│       └── store.py                # 文件管理（video/, results/ JSON）
│
└── tests/
```

### API 设计

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/synthesis` | 生成合成视频，参数：时长、违规场景数量、seed |
| POST | `/api/analyze/{video_id}` | 调用 algorithm pipeline 分析视频 |
| GET | `/api/results/{video_id}` | 返回分析结果 JSON（逐帧检测 + 违规段 + 可视化帧路径） |

### 数据流

```
frontend                    backend                      algorithm
  │                           │                            │
  ├─ POST /synthesis ────────>│  Pillow渲染 → mp4          │
  │<── {video_id, url} ──────┤                            │
  │                           │                            │
  ├─ POST /analyze/{id} ─────>│  ──调用──> pipeline.run() │
  │                           │  <──返回──  results       │
  │<── {processing} ─────────┤                            │
  │                           │                            │
  ├─ GET /results/{id} ──────>│  读取 storage/results/    │
  │<── {frames, segments} ───┤                            │
```

### 合成视频场景

- 路面：深灰车道 + 白色实线 + 应急车道在右侧
- 车辆：矩形色块代表车辆，按配置在正常车道/应急车道移动
- seed 保证可复现
- 违规场景：车辆驶入应急车道并持续 10-20 秒
- backend 的 synthesis/ 与 algorithm/ 各自职责明确：synthesis 生成数据，algorithm 消费数据
- **Ground-truth 记录**：合成时每帧的车辆位置同步写入 `{video_id}.gt.json`，供 pipeline 以 ground-truth 模式运行（替代 YOLO 检测，因为 Pillow 绘制的矩形色块无法被 YOLO 识别为车辆）

---

## 5. frontend/ 设计

### 目录结构

实际实现为单文件模式（`main.ts` 内联渲染所有 UI，无组件拆分）：

```
frontend/
├── src/
│   ├── main.ts              # 单页应用入口（内联 HTML + 状态 + 事件绑定 + Canvas 同步）
│   ├── style.css            # 暗色主题样式
│   ├── types.ts             # TypeScript 类型（SynthesisRequest/Response, AnalysisResult 等）
│   ├── api.ts               # fetch 封装（3 个端点）
│   ├── config.ts            # API_BASE_URL
│   └── utils/
│       └── draw.ts          # Canvas 绘图工具（检测框 + 时间轴）
│
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

### 单页布局

```
┌─────────────────────────────────────────┐
│  ControlBar                             │
│  [合成视频] [运行检测]  seed:42  阈值▲▼  │
├─────────────────────────────────────────┤
│                                         │
│  VideoStage （视频 + DetectionOverlay）  │
│  ┌─────────────────────────────────────┐│
│  │       [video frame]                 ││
│  │   绿色掩膜区域 + 红色检测框          ││
│  │   当前帧 # 标签：car 0.87           ││
│  └─────────────────────────────────────┘│
│                                         │
│  ViolationTimeline                      │
│  ├────○────●●●●●●●●●●●────○──────┤      │
│        正常    违规段(红)    正常         │
│                                         │
│  FrameStrip                             │
│  [帧1] [帧2] [帧3] [帧4]  ← 违规瞬间    │
└─────────────────────────────────────────┘
```

### 交互流程

1. 页面加载 → ControlBar 亮"合成视频"
2. 点击"合成视频" → POST /synthesis → 显示进度 → 视频就绪
3. 点击"运行检测" → POST /analyze → 轮询 GET /results
4. 结果返回 → VideoStage 播放 + DetectionOverlay 实时叠加检测信息 + Timeline 标记违规段
5. FrameStrip 展示违规关键帧缩略图

### 与当前前端的差异

当前前端为多页路由（总览/事件/案件/证据链），重写为单页 demo。当前未提交的前端改动（pipeline 可视化、系统信息卡片、趋势图等）废弃。

---

## 6. android/ 设计

### 目录结构

```
android/
├── app/src/main/
│   ├── cpp/                          ← 保留现有
│   │   ├── LaneDetector.cpp          # ncnn YOLOv8n 推理
│   │   ├── LaneDetector.h
│   │   ├── native-lib.cpp            # JNI 桥接
│   │   ├── stl_fix.cpp
│   │   └── CMakeLists.txt            # 不变
│   │
│   ├── assets/                       ← 保留现有
│   │   ├── yolov8n.param
│   │   └── yolov8n.bin
│   │
│   └── java/com/example/emergencylaneguard/
│       ├── LaneDetector.kt           ← 保留 JNI 封装
│       ├── OverlayView.kt            ← 保留检测框绘制
│       └── BenchmarkActivity.kt      ← 新增：基准测试 UI
```

### 删除项

- CaseDetailActivity.kt, VideoPlayerActivity.kt, RecordsActivity.kt, SettingsActivity.kt, MainActivity.kt
- HomeFragment.kt, PendingFragment.kt, CasesFragment.kt
- CasesAdapter.kt, ViolationAdapter.kt
- ViolationViewModel.kt（HyperLPR3 + Room + StepFun）
- VideoTrimmer.kt, utils/MediaStoreUtils.kt
- network/（StepFun API 客户端）
- database/（Room 实体和 DAO）
- com.yrd.emergencylanemobile/（Jetpack Compose 重写版本全部代码）
- 旧 layout/menu 资源文件
- build.gradle.kts 移除 Compose、Room、Retrofit、Coil、Glide、HyperLPR3 依赖

### BenchmarkActivity 单页

```
┌────────────────────────────────────┐
│  EmergencyLaneGuard - YOLO 基准     │
│                                    │
│  Model: YOLOv8n                    │
│  Backend: ncnn                     │
│                                    │
│  ┌──────────────────────────────┐  │
│  │  相机预览 (PreviewView)       │  │
│  │  + 检测框叠加 (OverlayView)   │  │
│  └──────────────────────────────┘  │
│                                    │
│  FPS: 12.3       延迟: 82ms/帧     │
│                                    │
│  [开始基准测试]  (跑100帧取平均)    │
│                                    │
│  结果:                             │
│  ├─ 平均推理时间: 78ms             │
│  ├─ 预处理时间:   12ms             │
│  ├─ 总帧处理时间: 90ms             │
│  ├─ FPS:          11.1             │
│  └─ 模型加载时间: 1.2s             │
└────────────────────────────────────┘
```

### 保留清单

- C++ 层（LaneDetector.cpp/h、native-lib.cpp、CMakeLists.txt）
- LaneDetector.kt JNI 封装
- OverlayView（检测框绘制）
- assets 模型文件（yolov8n.param、yolov8n.bin）

---

## 7. 当前未提交改动处理

当前工作区的未提交改动（`frontend/src/main.ts`、`frontend/src/style.css`、`frontend/src/types.ts`）与重设计方向不一致，执行 `git checkout --` 恢复后在新架构下从头编写。

---

## 8. 关键设计决策汇总

| 决策 | 选择 |
|------|------|
| 项目结构 | 分层重建（algorithm/ + backend/ + frontend/ + android/） |
| 算法位置 | `algorithm/` 独立目录，backend 通过函数调用消费 |
| 前端定位 | 单页 demo（视频 + 叠加 + 时间轴） |
| 评估方式 | 合成视频定性演示（Pillow seed-based 可复现） |
| 车道提取 | 保持现有方法（论文定位为"先验知识"） |
| 时间判定 | 代码实现简单滑动窗口（论文讨论高级方案方向） |
| Android | 仅 YOLO 推理基准测试 |
| 合成视频检测 | ground-truth 注入（YOLO 无法识别 Pillow 矩形色块），真实视频仍走 YOLO |
| 短视频时间判定 | `finalize()` 以可用帧比率判定，解决视频 < 窗口大小的边界情况 |
| 当前未提交改动 | 废弃，从新架构重写 |
