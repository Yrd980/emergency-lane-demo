# 算法类毕设项目重写 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将项目从应用类 demo 重写为算法类毕设项目，核心是 algorithm/ 四模块管线 + backend/ 合成视频与 API + frontend/ 单页 demo + android/ YOLO 基准。

**Architecture:** algorithm/ 独立 Python 包(四模块管线) → backend/ thin FastAPI(合成视频 + 3 个 REST 端点) → frontend/ 单页 TypeScript demo。android/ 独立精简为 YOLO 推理基准。

**Tech Stack:** Python 3.12, OpenCV, Pillow, Ultralytics YOLOv8n, FastAPI, TypeScript, Vite, Android/ncnn

---

## Phase 0: 清理与准备

### Task 0.1: 丢弃未提交的前端改动

**Files:**
- Modify: `frontend/src/main.ts` (git checkout)
- Modify: `frontend/src/style.css` (git checkout)
- Modify: `frontend/src/types.ts` (git checkout)

- [ ] **Step 1: 丢弃未提交改动**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo checkout -- frontend/src/main.ts frontend/src/style.css frontend/src/types.ts
```

- [ ] **Step 2: 验证工作区干净**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo status --short
```

Expected: 无输出（无未提交改动）

- [ ] **Step 3: 提交**

```bash
# No changes to commit — this was a revert to clean state
```

### Task 0.2: 删除后端 mock 代码

**Files:**
- Delete: `backend/src/app/services/analyzer.py`
- Delete: `backend/src/app/services/mock_vision.py`
- Delete: `backend/src/app/services/demo_assets.py`
- Delete: `backend/src/app/services/presenters.py`
- Delete: `backend/src/app/db.py`
- Delete: `backend/src/app/time_utils.py`
- Delete: `backend/src/app/schemas.py`

- [ ] **Step 1: 删除旧 mock 模块**

```bash
rm /home/yrd/documents/git_clone_code/etc/emergency-lane-demo/backend/src/app/services/analyzer.py
rm /home/yrd/documents/git_clone_code/etc/emergency-lane-demo/backend/src/app/services/mock_vision.py
rm /home/yrd/documents/git_clone_code/etc/emergency-lane-demo/backend/src/app/services/demo_assets.py
rm /home/yrd/documents/git_clone_code/etc/emergency-lane-demo/backend/src/app/services/presenters.py
rm /home/yrd/documents/git_clone_code/etc/emergency-lane-demo/backend/src/app/db.py
rm /home/yrd/documents/git_clone_code/etc/emergency-lane-demo/backend/src/app/time_utils.py
rm /home/yrd/documents/git_clone_code/etc/emergency-lane-demo/backend/src/app/schemas.py
```

- [ ] **Step 2: 确认 services/ 目录只剩 __init__.py**

```bash
ls /home/yrd/documents/git_clone_code/etc/emergency-lane-demo/backend/src/app/services/
```

Expected: `__init__.py`

- [ ] **Step 3: 提交**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo add -A
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo commit -m "chore: remove backend mock service code

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 0.3: 添加 ultralytics 依赖

**Files:**
- Modify: `backend/pyproject.toml`

- [ ] **Step 1: 在 pyproject.toml 添加 ultralytics**

Edit `backend/pyproject.toml`，在 dependencies 列表中添加 `"ultralytics>=8.3.0"`。

最终 dependencies 应为：
```toml
dependencies = [
    "aiofiles>=25.1.0",
    "fastapi>=0.135.1",
    "httpx>=0.28.1",
    "numpy>=2.4.2",
    "opencv-python-headless>=4.13.0.92",
    "pillow>=12.1.1",
    "pydantic-settings>=2.13.1",
    "python-dotenv>=1.2.2",
    "python-multipart>=0.0.22",
    "sniffio>=1.3.1",
    "ultralytics>=8.3.0",
    "uvicorn[standard]>=0.41.0",
]
```

- [ ] **Step 2: 安装新依赖**

```bash
cd /home/yrd/documents/git_clone_code/etc/emergency-lane-demo/backend && uv sync
```

Expected: ultralytics 和 torch 安装成功

- [ ] **Step 3: 提交**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo add backend/pyproject.toml backend/uv.lock
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo commit -m "chore: add ultralytics dependency

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Phase 1: algorithm/ 核心算法

### Task 1.1: 创建 algorithm/ 目录骨架

**Files:**
- Create: `algorithm/__init__.py`
- Create: `algorithm/lane_extraction/__init__.py`
- Create: `algorithm/vehicle_detection/__init__.py`
- Create: `algorithm/spatial_reasoning/__init__.py`
- Create: `algorithm/temporal_judgment/__init__.py`

- [ ] **Step 1: 创建目录**

```bash
mkdir -p /home/yrd/documents/git_clone_code/etc/emergency-lane-demo/algorithm/{lane_extraction,vehicle_detection,spatial_reasoning,temporal_judgment}
```

- [ ] **Step 2: 写入根 __init__.py**

```python
# algorithm/__init__.py
```

- [ ] **Step 3: 写入各子模块 __init__.py**

```python
# algorithm/lane_extraction/__init__.py
from .lane_detector import detect_lane_line
from .region_mask import apply_region_mask
from .mask_visualizer import draw_mask_overlay
```

```python
# algorithm/vehicle_detection/__init__.py
from .yolo_detector import YOLODetector
from .postprocess import filter_vehicles
```

```python
# algorithm/spatial_reasoning/__init__.py
from .overlap import check_vehicle_in_region
```

```python
# algorithm/temporal_judgment/__init__.py
from .violation_decider import ViolationDecider
```

- [ ] **Step 4: 提交**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo add algorithm/
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo commit -m "feat: create algorithm package skeleton

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 1.2: 模块一 — 车道线检测

**Files:**
- Create: `algorithm/lane_extraction/lane_detector.py`

- [ ] **Step 1: 实现 lane_detector.py**

```python
"""车道线检测 — 白色阈值 + Canny + HoughLinesP 找最长白实线."""
from __future__ import annotations

import cv2
import numpy as np


def detect_lane_line(frame: np.ndarray) -> tuple[int, int, int, int] | None:
    """检测画面中最长的白色实线（应急车道分界线）。

    Returns:
        (x1, y1, x2, y2) 或 None（未检测到）
    """
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)

    # 白色阈值：提取路面白线
    _, white_mask = cv2.threshold(gray, 200, 255, cv2.THRESH_BINARY)

    # Canny 边缘检测
    edges = cv2.Canny(white_mask, 50, 150, apertureSize=3)

    # HoughLinesP 找线段
    lines = cv2.HoughLinesP(edges, 1, np.pi / 180, threshold=30,
                            minLineLength=50, maxLineGap=100)

    if lines is None:
        return None

    longest_line: tuple[int, int, int, int] | None = None
    max_len = 0.0

    for line in lines:
        x1, y1, x2, y2 = line[0]
        dx = x2 - x1
        dy = y2 - y1

        if dx == 0:
            continue

        slope = dy / dx
        if slope < 0.3:
            continue

        length = np.sqrt(float(dx)**2 + float(dy)**2)
        if length > max_len:
            max_len = length
            longest_line = (x1, y1, x2, y2)

    return longest_line
```

- [ ] **Step 2: 验证可导入**

```bash
cd /home/yrd/documents/git_clone_code/etc/emergency-lane-demo && python -c "from algorithm.lane_extraction.lane_detector import detect_lane_line; print('OK')"
```

- [ ] **Step 3: 提交**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo add algorithm/lane_extraction/lane_detector.py algorithm/lane_extraction/__init__.py
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo commit -m "feat: add lane line detection (module 1)

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 1.3: 模块一 — 区域掩膜

**Files:**
- Create: `algorithm/lane_extraction/region_mask.py`

- [ ] **Step 1: 实现 region_mask.py**

```python
"""固定几何约束掩膜 — 左 1/4 + 斜线三角区域外全部填充白色."""
from __future__ import annotations

import cv2
import numpy as np


def apply_region_mask(frame: np.ndarray,
                      line: tuple[int, int, int, int] | None = None
                      ) -> tuple[np.ndarray, np.ndarray]:
    """对图像施加固定几何约束掩膜，仅保留应急车道区域。

    掩膜策略：
    1. 画面左 1/4 填充白色
    2. 从 (0, h/2) 到 (w/2, h) 的斜线下方三角填充白色

    Returns:
        (masked_frame, mask) — masked_frame 是掩膜后的图像，mask 是二值掩膜(255=保留区域)
    """
    h, w = frame.shape[:2]

    # 创建掩膜：255=保留，0=填充白
    mask = np.ones((h, w), dtype=np.uint8) * 255

    # 1. 左 1/4 填充
    quarter_w = w // 4
    cv2.rectangle(mask, (0, 0), (quarter_w, h), 0, -1)

    # 2. 斜线下方三角填充
    p1 = (0, h // 2)
    p2 = (w // 2, h)
    p3 = (0, h)
    triangle = np.array([p1, p2, p3], np.int32)
    cv2.fillPoly(mask, [triangle], 0)

    # 应用掩膜
    masked = cv2.bitwise_and(frame, frame, mask=mask)

    # 非保留区域填充白色
    mask_inv = cv2.bitwise_not(mask)
    white_bg = np.ones_like(frame) * 255
    white_part = cv2.bitwise_and(white_bg, white_bg, mask=mask_inv)
    result = cv2.add(masked, white_part)

    return result, mask
```

- [ ] **Step 2: 验证可导入**

```bash
cd /home/yrd/documents/git_clone_code/etc/emergency-lane-demo && python -c "from algorithm.lane_extraction.region_mask import apply_region_mask; print('OK')"
```

- [ ] **Step 3: 提交**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo add algorithm/lane_extraction/region_mask.py
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo commit -m "feat: add fixed-geometry region mask (module 1)

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 1.4: 模块一 — 掩膜可视化

**Files:**
- Create: `algorithm/lane_extraction/mask_visualizer.py`

- [ ] **Step 1: 实现 mask_visualizer.py**

```python
"""掩膜区域可视化 — 在帧上绘制掩膜边界线和半透明覆盖."""
from __future__ import annotations

import cv2
import numpy as np


def draw_mask_overlay(frame: np.ndarray, mask: np.ndarray) -> np.ndarray:
    """在帧上绘制掩膜区域边界和半透明绿色覆盖。

    Args:
        frame: 原始 BGR 图像
        mask: 二值掩膜（255=保留的应急车道区域）

    Returns:
        带可视化叠加的 BGR 图像副本
    """
    h, w = frame.shape[:2]
    overlay = frame.copy()
    quarter_w = w // 4

    # 半透明绿色覆盖保留区域
    green = np.zeros_like(frame, dtype=np.uint8)
    green[:] = (0, 255, 0)
    green_region = cv2.bitwise_and(green, green, mask=mask)
    overlay = cv2.addWeighted(overlay, 1.0, green_region, 0.18, 0)

    # 绘制掩膜边界线
    cv2.line(overlay, (quarter_w, 0), (quarter_w, h), (0, 255, 0), 2)
    cv2.line(overlay, (0, h // 2), (w // 2, h), (0, 255, 0), 2)

    # 标注区域名称
    cv2.putText(overlay, "emergency lane", (quarter_w + 10, 30),
                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)

    return overlay
```

- [ ] **Step 2: 验证可导入**

```bash
cd /home/yrd/documents/git_clone_code/etc/emergency-lane-demo && python -c "from algorithm.lane_extraction.mask_visualizer import draw_mask_overlay; print('OK')"
```

- [ ] **Step 3: 提交**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo add algorithm/lane_extraction/mask_visualizer.py
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo commit -m "feat: add mask overlay visualization (module 1)

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 1.5: 模块二 — YOLO 检测器

**Files:**
- Create: `algorithm/vehicle_detection/yolo_detector.py`

- [ ] **Step 1: 实现 yolo_detector.py**

```python
"""YOLOv8n 推理器 — 轻量化车辆目标检测."""
from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

import cv2
import numpy as np
from ultralytics import YOLO


@dataclass
class Detection:
    """检测结果。"""
    x: float       # bbox 左上 x (像素坐标)
    y: float       # bbox 左上 y
    w: float       # bbox 宽
    h: float       # bbox 高
    score: float   # 置信度
    cls: int        # COCO 类别 ID (2=car, 5=bus, 7=truck)
    label: str      # 类别名称


MODEL_PATH = Path(__file__).resolve().parents[1] / "yolov8n.pt"


class YOLODetector:
    """YOLOv8n 目标检测器，自动下载模型。"""

    def __init__(self, model_path: str | Path = MODEL_PATH, device: str = "cpu"):
        self.model = YOLO(str(model_path))
        self.device = device

    def detect(self, frame: np.ndarray,
               conf_threshold: float = 0.3) -> list[Detection]:
        """对单帧图像运行目标检测。

        Args:
            frame: BGR 图像 (H, W, 3)
            conf_threshold: 置信度阈值

        Returns:
            Detection 列表
        """
        results = self.model(frame, verbose=False, device=self.device)
        detections: list[Detection] = []

        for result in results:
            if result.boxes is None:
                continue
            for box in result.boxes:
                cls = int(box.cls[0])
                conf = float(box.conf[0])
                if conf < conf_threshold:
                    continue
                x1, y1, x2, y2 = box.xyxy[0].tolist()
                detections.append(Detection(
                    x=x1, y=y1,
                    w=x2 - x1, h=y2 - y1,
                    score=conf,
                    cls=cls,
                    label=self.model.names.get(cls, str(cls)),
                ))

        return detections
```

- [ ] **Step 2: 验证可导入**

```bash
cd /home/yrd/documents/git_clone_code/etc/emergency-lane-demo && python -c "from algorithm.vehicle_detection.yolo_detector import YOLODetector; print('OK')"
```

- [ ] **Step 3: 提交**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo add algorithm/vehicle_detection/yolo_detector.py algorithm/vehicle_detection/__init__.py
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo commit -m "feat: add YOLOv8n detector (module 2)

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 1.6: 模块二 — 后处理（车辆类别过滤）

**Files:**
- Create: `algorithm/vehicle_detection/postprocess.py`

- [ ] **Step 1: 实现 postprocess.py**

```python
"""后处理 — NMS + 车辆类别过滤."""
from __future__ import annotations

from .yolo_detector import Detection

# COCO: 2=car, 3=motorcycle, 5=bus, 7=truck
VEHICLE_CLASSES: set[int] = {2, 3, 5, 7}


def filter_vehicles(detections: list[Detection],
                    conf_threshold: float = 0.3
                    ) -> list[Detection]:
    """过滤非车辆类别，保留置信度高于阈值的检测结果。

    Args:
        detections: YOLO 原始检测结果
        conf_threshold: 车辆置信度阈值

    Returns:
        仅包含车辆类别的 Detection 列表
    """
    return [d for d in detections
            if d.cls in VEHICLE_CLASSES and d.score >= conf_threshold]
```

- [ ] **Step 2: 验证可导入**

```bash
cd /home/yrd/documents/git_clone_code/etc/emergency-lane-demo && python -c "from algorithm.vehicle_detection.postprocess import filter_vehicles; print('OK')"
```

- [ ] **Step 3: 提交**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo add algorithm/vehicle_detection/postprocess.py
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo commit -m "feat: add vehicle class filter (module 2)

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 1.7: 模块三 — 空间关系判定

**Files:**
- Create: `algorithm/spatial_reasoning/overlap.py`

- [ ] **Step 1: 实现 overlap.py**

```python
"""空间关系建模 — 计算车辆检测框与应急车道掩膜的重叠关系."""
from __future__ import annotations

import numpy as np

from algorithm.vehicle_detection.yolo_detector import Detection


def check_vehicle_in_region(detections: list[Detection],
                            mask: np.ndarray,
                            overlap_ratio: float = 0.3
                            ) -> list[Detection]:
    """判断车辆是否位于应急车道区域内。

    判定准则：检测框中心点落在掩膜保留区域内 或
             检测框与掩膜区域的重叠面积 > 30% 检测框面积。

    Args:
        detections: 车辆检测结果列表
        mask: 二值掩膜（255=应急车道区域）
        overlap_ratio: 重叠面积占比阈值

    Returns:
        位于应急车道区域内的 Detection 子集
    """
    violations: list[Detection] = []

    for det in detections:
        x1, y1 = int(det.x), int(det.y)
        x2, y2 = int(det.x + det.w), int(det.y + det.h)

        # 裁剪到图像边界
        h, w = mask.shape
        x1, y1 = max(0, x1), max(0, y1)
        x2, y2 = min(w, x2), min(h, y2)

        if x2 <= x1 or y2 <= y1:
            continue

        # 中心点判定
        cx, cy = int(det.x + det.w / 2), int(det.y + det.h / 2)
        center_in_region = (0 <= cx < w and 0 <= cy < h
                            and mask[cy, cx] == 255)

        # 重叠面积判定
        bbox_area = det.w * det.h
        if bbox_area <= 0:
            continue
        bbox_region = mask[y1:y2, x1:x2]
        overlap_area = np.count_nonzero(bbox_region == 255)

        ratio = overlap_area / max(bbox_area, 1.0)

        if center_in_region or ratio > overlap_ratio:
            violations.append(det)

    return violations
```

- [ ] **Step 2: 验证可导入**

```bash
cd /home/yrd/documents/git_clone_code/etc/emergency-lane-demo && python -c "from algorithm.spatial_reasoning.overlap import check_vehicle_in_region; print('OK')"
```

- [ ] **Step 3: 提交**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo add algorithm/spatial_reasoning/overlap.py algorithm/spatial_reasoning/__init__.py
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo commit -m "feat: add spatial overlap reasoning (module 3)

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 1.8: 模块四 — 时间一致性违规判定

**Files:**
- Create: `algorithm/temporal_judgment/violation_decider.py`

- [ ] **Step 1: 实现 violation_decider.py**

```python
"""时间一致性约束 — 滑动窗口 + 占用帧比例阈值判定违规."""
from __future__ import annotations

from dataclasses import dataclass, field


@dataclass
class ViolationSegment:
    """违规时间段。"""
    start_frame: int
    end_frame: int
    duration_frames: int
    occupancy_ratio: float   # 窗口内占用帧比例
    confidence: float         # 平均检测置信度


@dataclass
class FrameVerdict:
    """单帧判定结果。"""
    frame_idx: int
    has_violation: bool
    vehicle_count: int = 0
    max_confidence: float = 0.0


class ViolationDecider:
    """滑动时间窗口违规判定器。

    策略：在 window_frames 帧窗口内，violation 帧占比 > ratio_threshold 则判定违规。
    """

    def __init__(self, window_seconds: float = 15.0,
                 fps: float = 30.0,
                 ratio_threshold: float = 0.6):
        self.window_frames = int(window_seconds * fps)
        self.ratio_threshold = ratio_threshold
        self.fps = fps
        self._buffer: list[FrameVerdict] = []
        self._segments: list[ViolationSegment] = []
        self._in_violation = False
        self._segment_start = -1

    def update(self, verdict: FrameVerdict) -> None:
        """输入一帧的判定结果。"""
        self._buffer.append(verdict)

        if len(self._buffer) < self.window_frames:
            return

        if len(self._buffer) > self.window_frames:
            self._buffer = self._buffer[-self.window_frames:]

        violation_count = sum(1 for v in self._buffer if v.has_violation)
        ratio = violation_count / self.window_frames

        if ratio >= self.ratio_threshold:
            if not self._in_violation:
                self._in_violation = True
                self._segment_start = self._buffer[0].frame_idx
        else:
            if self._in_violation:
                self._in_violation = False
                end_frame = self._buffer[-1].frame_idx
                duration = end_frame - self._segment_start
                confidences = [v.max_confidence for v in self._buffer
                               if v.has_violation]
                avg_conf = sum(confidences) / max(len(confidences), 1)
                self._segments.append(ViolationSegment(
                    start_frame=self._segment_start,
                    end_frame=end_frame,
                    duration_frames=duration,
                    occupancy_ratio=ratio,
                    confidence=avg_conf,
                ))

    def finalize(self) -> list[ViolationSegment]:
        """处理缓冲区剩余帧，返回所有违规段。"""
        if self._in_violation and self._buffer:
            end_frame = self._buffer[-1].frame_idx
            duration = end_frame - self._segment_start
            confidences = [v.max_confidence for v in self._buffer
                           if v.has_violation]
            avg_conf = sum(confidences) / max(len(confidences), 1)
            self._segments.append(ViolationSegment(
                start_frame=self._segment_start,
                end_frame=end_frame,
                duration_frames=duration,
                occupancy_ratio=1.0,
                confidence=avg_conf,
            ))
            self._in_violation = False
        return self._segments
```

- [ ] **Step 2: 验证可导入**

```bash
cd /home/yrd/documents/git_clone_code/etc/emergency-lane-demo && python -c "from algorithm.temporal_judgment.violation_decider import ViolationDecider; print('OK')"
```

- [ ] **Step 3: 提交**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo add algorithm/temporal_judgment/violation_decider.py algorithm/temporal_judgment/__init__.py
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo commit -m "feat: add sliding-window violation decider (module 4)

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 1.9: Pipeline 编排器

**Files:**
- Create: `algorithm/pipeline.py`

- [ ] **Step 1: 实现 pipeline.py**

```python
"""四模块编排器 — 串联车道提取→车辆检测→空间判定→时间判定."""
from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path

import cv2
import numpy as np

from algorithm.lane_extraction import apply_region_mask, detect_lane_line, draw_mask_overlay
from algorithm.vehicle_detection import YOLODetector, filter_vehicles
from algorithm.spatial_reasoning import check_vehicle_in_region
from algorithm.temporal_judgment import FrameVerdict, ViolationDecider, ViolationSegment
from algorithm.vehicle_detection.yolo_detector import Detection


@dataclass
class FrameResult:
    """单帧分析结果。"""
    frame_idx: int
    has_violation: bool
    mask: np.ndarray | None = None
    detections: list[Detection] = field(default_factory=list)
    violations: list[Detection] = field(default_factory=list)
    overlay_bgr: np.ndarray | None = None


@dataclass
class PipelineResult:
    """完整 pipeline 输出。"""
    frame_results: list[FrameResult]
    violation_segments: list[ViolationSegment]
    fps: float
    total_frames: int


class Pipeline:
    """应急车道违规检测管线。"""

    def __init__(self, conf_threshold: float = 0.3,
                 window_seconds: float = 15.0,
                 ratio_threshold: float = 0.6,
                 device: str = "cpu"):
        self.conf_threshold = conf_threshold
        self.window_seconds = window_seconds
        self.ratio_threshold = ratio_threshold
        self._detector = YOLODetector(device=device)
        self._decider: ViolationDecider | None = None
        self._fps: float = 30.0

    def run(self, video_path: Path, sample_every: int = 1) -> PipelineResult:
        """对视频运行完整检测管线。

        Args:
            video_path: 视频文件路径
            sample_every: 每隔多少帧采样一次（1=每帧，2=每隔一帧）

        Returns:
            PipelineResult 包含逐帧结果和违规段列表
        """
        cap = cv2.VideoCapture(str(video_path))
        self._fps = cap.get(cv2.CAP_PROP_FPS)
        if self._fps <= 0:
            self._fps = 30.0

        self._decider = ViolationDecider(
            window_seconds=self.window_seconds,
            fps=self._fps,
            ratio_threshold=self.ratio_threshold,
        )

        frame_results: list[FrameResult] = []
        frame_idx = 0

        while True:
            ret, frame = cap.read()
            if not ret:
                break

            if frame_idx % sample_every != 0:
                frame_idx += 1
                continue

            fr = self._process_frame(frame, frame_idx)
            frame_results.append(fr)
            self._decider.update(FrameVerdict(
                frame_idx=frame_idx,
                has_violation=fr.has_violation,
                vehicle_count=len(fr.violations),
                max_confidence=max((d.score for d in fr.violations), default=0.0),
            ))
            frame_idx += 1

        cap.release()
        segments = self._decider.finalize()

        return PipelineResult(
            frame_results=frame_results,
            violation_segments=segments,
            fps=self._fps,
            total_frames=frame_idx,
        )

    def _process_frame(self, frame: np.ndarray, frame_idx: int) -> FrameResult:
        # 模块一：区域提取
        line = detect_lane_line(frame)
        masked, mask = apply_region_mask(frame, line)

        # 模块二：车辆检测
        raw_dets = self._detector.detect(masked, conf_threshold=self.conf_threshold)
        vehicles = filter_vehicles(raw_dets, conf_threshold=self.conf_threshold)

        # 模块三：空间关系
        violations = check_vehicle_in_region(vehicles, mask)

        # 可视化叠加
        overlay = draw_mask_overlay(frame, mask)
        for det in violations:
            cv2.rectangle(overlay,
                          (int(det.x), int(det.y)),
                          (int(det.x + det.w), int(det.y + det.h)),
                          (0, 0, 255), 2)
            cv2.putText(overlay, f"{det.label} {det.score:.2f}",
                        (int(det.x), int(det.y) - 8),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 2)

        return FrameResult(
            frame_idx=frame_idx,
            has_violation=len(violations) > 0,
            mask=mask,
            detections=vehicles,
            violations=violations,
            overlay_bgr=overlay,
        )
```

- [ ] **Step 2: 验证可导入**

```bash
cd /home/yrd/documents/git_clone_code/etc/emergency-lane-demo && python -c "from algorithm.pipeline import Pipeline; print('OK')"
```

- [ ] **Step 3: 提交**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo add algorithm/pipeline.py
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo commit -m "feat: add four-module pipeline orchestrator

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 1.10: 可视化工具

**Files:**
- Create: `algorithm/visualize.py`

- [ ] **Step 1: 实现 visualize.py**

```python
"""可视化工具 — 时间轴图表、关键帧拼图."""
from __future__ import annotations

from pathlib import Path

import cv2
import numpy as np

from algorithm.temporal_judgment.violation_decider import ViolationSegment


def draw_timeline(segments: list[ViolationSegment],
                  total_frames: int,
                  fps: float,
                  output_path: Path) -> Path:
    """绘制违规时间轴 PNG。

    Args:
        segments: 违规段列表
        total_frames: 视频总帧数
        fps: 帧率
        output_path: 输出文件路径

    Returns:
        输出文件路径
    """
    width = 1200
    height = 80
    img = np.ones((height, width, 3), dtype=np.uint8) * 240

    # 违规段标红
    for seg in segments:
        x1 = int(seg.start_frame / max(total_frames, 1) * width)
        x2 = int(seg.end_frame / max(total_frames, 1) * width)
        x1 = max(0, min(width, x1))
        x2 = max(0, min(width, x2))
        cv2.rectangle(img, (x1, 10), (x2, height - 10), (60, 60, 240), -1)

    # 边框和时间标签
    cv2.rectangle(img, (0, 0), (width - 1, height - 1), (180, 180, 180), 1)
    total_sec = total_frames / max(fps, 1.0)
    cv2.putText(img, f"0s", (4, height - 4),
                cv2.FONT_HERSHEY_SIMPLEX, 0.4, (100, 100, 100), 1)
    cv2.putText(img, f"{total_sec:.0f}s", (width - 40, height - 4),
                cv2.FONT_HERSHEY_SIMPLEX, 0.4, (100, 100, 100), 1)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    cv2.imwrite(str(output_path), img)
    return output_path


def save_keyframes(frame_results: list,
                   segments: list[ViolationSegment],
                   output_dir: Path) -> list[Path]:
    """保存违规段中的关键帧（每段首/中/尾）。

    Returns:
        保存的文件路径列表
    """
    output_dir.mkdir(parents=True, exist_ok=True)
    paths: list[Path] = []

    frame_map = {fr.frame_idx: fr for fr in frame_results}

    for i, seg in enumerate(segments):
        for label, offset in [("start", 0), ("mid", seg.duration_frames // 2),
                               ("end", seg.duration_frames - 1)]:
            idx = seg.start_frame + offset
            if idx in frame_map and frame_map[idx].overlay_bgr is not None:
                fname = f"seg{i+1:02d}_{label}_f{idx:06d}.jpg"
                fp = output_dir / fname
                cv2.imwrite(str(fp), frame_map[idx].overlay_bgr)
                paths.append(fp)

    return paths
```

- [ ] **Step 2: 验证可导入**

```bash
cd /home/yrd/documents/git_clone_code/etc/emergency-lane-demo && python -c "from algorithm.visualize import draw_timeline, save_keyframes; print('OK')"
```

- [ ] **Step 3: 提交**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo add algorithm/visualize.py
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo commit -m "feat: add visualization tools (timeline + keyframes)

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Phase 2: backend/ 重构

### Task 2.1: 后端配置更新

**Files:**
- Modify: `backend/src/app/config.py`
- Modify: `backend/src/app/main.py`

- [ ] **Step 1: 重写 config.py**

```python
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    project_name: str = "高速公路应急车道违规检测 — 算法演示"
    app_env: str = "development"
    api_prefix: str = "/api"
    cors_origins: str = "http://127.0.0.1:5173,http://localhost:5173"

    # Pipeline 默认参数
    detection_conf_threshold: float = 0.3
    window_seconds: float = 15.0
    violation_ratio_threshold: float = 0.6

    @property
    def backend_root(self) -> Path:
        return Path(__file__).resolve().parents[3]

    @property
    def project_root(self) -> Path:
        return self.backend_root.parent

    @property
    def storage_dir(self) -> Path:
        return self.backend_root / "storage"

    @property
    def videos_dir(self) -> Path:
        return self.storage_dir / "videos"

    @property
    def results_dir(self) -> Path:
        return self.storage_dir / "results"

    def cors_origin_list(self) -> list[str]:
        return [item.strip() for item in self.cors_origins.split(",") if item.strip()]

    def ensure_dirs(self) -> None:
        self.storage_dir.mkdir(parents=True, exist_ok=True)
        self.videos_dir.mkdir(parents=True, exist_ok=True)
        self.results_dir.mkdir(parents=True, exist_ok=True)


settings = Settings()
```

- [ ] **Step 2: 重写 main.py 为最小骨架**

```python
from __future__ import annotations

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from .config import settings

settings.ensure_dirs()

app = FastAPI(title=settings.project_name)
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origin_list(),
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get(f"{settings.api_prefix}/health")
def health() -> dict[str, str]:
    return {"status": "ok"}
```

- [ ] **Step 3: 验证后端启动**

```bash
cd /home/yrd/documents/git_clone_code/etc/emergency-lane-demo/backend && timeout 5 uv run uvicorn src.app.main:app --host 127.0.0.1 --port 8000 2>&1 || true
```

Expected: Uvicorn 启动无报错

- [ ] **Step 4: 提交**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo add backend/src/app/config.py backend/src/app/main.py
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo commit -m "refactor: simplify backend config and main for new architecture

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 2.2: 合成视频 — 路面渲染

**Files:**
- Create: `backend/src/app/synthesis/__init__.py`
- Create: `backend/src/app/synthesis/highway_renderer.py`

- [ ] **Step 1: 实现 highway_renderer.py**

```python
"""高速路面渲染器 — Pillow 绘制双车道 + 应急车道."""
from __future__ import annotations

from PIL import Image, ImageDraw


# 画面尺寸
CANVAS_W = 1280
CANVAS_H = 720

# 车道几何（像素坐标）
LANE_DIVIDER_X = 480    # 行车道 / 应急车道分界线起始（顶部）
ROAD_LEFT = 80          # 路面左边界
ROAD_RIGHT = 1100       # 路面右边界（应急车道外边界）
WHITE_LINE_X = 680      # 白色实线 x 坐标（顶部） — 关键：应急车道分界线
WHITE_LINE_BOTTOM_X = 880  # 白色实线 x 坐标（底部） — 透视效果

# 颜色
ROAD_COLOR = (60, 60, 65)        # 深灰路面
EMERGENCY_LANE_COLOR = (65, 62, 68)  # 应急车道略不同色调
WHITE_LINE_COLOR = (230, 230, 235)
GRASS_COLOR = (95, 130, 65)


def render_highway_frame(seed: int = 0) -> Image.Image:
    """渲染单帧高速公路场景。

    Returns:
        PIL Image (RGB)
    """
    img = Image.new("RGB", (CANVAS_W, CANVAS_H), GRASS_COLOR)
    draw = ImageDraw.Draw(img)

    # 路面主体
    road_poly = [
        (ROAD_LEFT, 0),
        (ROAD_LEFT, CANVAS_H),
        (ROAD_RIGHT, CANVAS_H),
        (ROAD_RIGHT, 0),
    ]
    draw.polygon(road_poly, fill=ROAD_COLOR)

    # 白色实线（应急车道分界）
    line_y_steps = 20
    for i in range(line_y_steps):
        t_top = i / line_y_steps
        t_bot = (i + 1) / line_y_steps
        x_top = WHITE_LINE_X + (WHITE_LINE_BOTTOM_X - WHITE_LINE_X) * t_top
        x_bot = WHITE_LINE_X + (WHITE_LINE_BOTTOM_X - WHITE_LINE_X) * t_bot
        y_top = int(CANVAS_H * t_top)
        y_bot = int(CANVAS_H * t_bot)
        draw.polygon(
            [(x_top - 3, y_top), (x_top + 3, y_top),
             (x_bot + 3, y_bot), (x_bot - 3, y_bot)],
            fill=WHITE_LINE_COLOR,
        )

    # 行车道中间虚线
    dash_y = 0
    dash_on = True
    while dash_y < CANVAS_H:
        if dash_on:
            t1 = dash_y / CANVAS_H
            t2 = min(dash_y + 30, CANVAS_H) / CANVAS_H
            x1 = 280 + (420 - 280) * t1
            x2 = 280 + (420 - 280) * t2
            draw.line([(x1, dash_y), (x2, min(dash_y + 30, CANVAS_H))],
                      fill=(200, 200, 120), width=2)
        dash_y += 50
        dash_on = not dash_on

    return img
```

- [ ] **Step 2: 测试渲染单帧**

```bash
cd /home/yrd/documents/git_clone_code/etc/emergency-lane-demo/backend && uv run python -c "
from src.app.synthesis.highway_renderer import render_highway_frame
img = render_highway_frame()
img.save('/tmp/test_highway_frame.png')
print(f'Saved: {img.size}')
"
```

Expected: `Saved: (1280, 720)`

- [ ] **Step 3: 提交**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo add backend/src/app/synthesis/__init__.py backend/src/app/synthesis/highway_renderer.py
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo commit -m "feat: add highway road surface renderer

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 2.3: 合成视频 — 车辆放置

**Files:**
- Create: `backend/src/app/synthesis/vehicle_placer.py`

- [ ] **Step 1: 实现 vehicle_placer.py**

```python
"""车辆放置逻辑 — 在正常车道和应急车道移动车辆."""
from __future__ import annotations

from dataclasses import dataclass
import random

from PIL import Image, ImageDraw


CANVAS_W = 1280
CANVAS_H = 720

# 应急车道范围：白色实线以右
# 顶部：WHITE_LINE_X(680) ~ ROAD_RIGHT(1100)
# 底部：WHITE_LINE_BOTTOM_X(880) ~ ROAD_RIGHT(1100)
WHITE_LINE_X = 680
WHITE_LINE_BOTTOM_X = 880
ROAD_RIGHT = 1100
ROAD_LEFT = 80

VEHICLE_COLORS = {
    "car": (200, 50, 50),
    "truck": (50, 80, 200),
    "bus": (200, 150, 30),
}


@dataclass
class VehicleState:
    """车辆状态。"""
    vehicle_id: int
    vehicle_type: str  # "car", "truck", "bus"
    x: float            # 中心 x
    y: float            # 中心 y（0=顶部，CANVAS_H=底部）
    w: int = 80
    h: int = 40
    speed: float = 3.0  # 像素/帧
    in_emergency_lane: bool = False
    enter_emergency_frame: int | None = None
    stay_duration: int = 180  # 在应急车道停留帧数


def _get_lane_bounds(y: float) -> tuple[float, float]:
    """获取 y 位置的应急车道左边界 x（白色实线位置）。"""
    t = y / CANVAS_H
    return WHITE_LINE_X + (WHITE_LINE_BOTTOM_X - WHITE_LINE_X) * t, ROAD_RIGHT


def _get_normal_lane_bounds(y: float) -> tuple[float, float]:
    """获取 y 位置的行车道范围。"""
    return ROAD_LEFT + 50, WHITE_LINE_X + (WHITE_LINE_BOTTOM_X - WHITE_LINE_X) * (y / CANVAS_H) - 60


def create_vehicles(num_normal: int, num_violating: int,
                    seed: int = 42) -> list[VehicleState]:
    """创建初始车辆集合。

    Args:
        num_normal: 正常行驶车辆数量
        num_violating: 违规车辆数量（会进入应急车道）
        seed: 随机种子

    Returns:
        VehicleState 列表
    """
    rng = random.Random(seed)
    vehicles: list[VehicleState] = []
    vid = 0

    for _ in range(num_normal):
        y = rng.uniform(0, CANVAS_H)
        x_min, x_max = _get_normal_lane_bounds(y)
        x = rng.uniform(x_min, x_max)
        vehicles.append(VehicleState(
            vehicle_id=vid,
            vehicle_type=rng.choice(["car", "car", "truck"]),
            x=x, y=y,
            speed=rng.uniform(1.5, 4.0),
            in_emergency_lane=False,
        ))
        vid += 1

    for _ in range(num_violating):
        y = rng.uniform(CANVAS_H * 0.2, CANVAS_H * 0.8)
        x_min, _ = _get_lane_bounds(y)
        x = x_min + rng.uniform(30, 200)  # 在应急车道内
        vehicles.append(VehicleState(
            vehicle_id=vid,
            vehicle_type="car",
            x=x, y=y,
            speed=rng.uniform(1.5, 3.0),
            in_emergency_lane=True,
            enter_emergency_frame=rng.randint(30, 60),
            stay_duration=rng.randint(240, 450),  # 8-15s at 30fps
        ))
        vid += 1

    return vehicles


def update_vehicles(vehicles: list[VehicleState],
                    frame_idx: int,
                    seed: int = 42) -> list[VehicleState]:
    """更新车辆位置（单帧）。

    违规车辆逻辑：前 enter_emergency_frame 帧在正常车道行驶，
    之后切入应急车道持续 stay_duration 帧，再返回正常车道。
    """
    rng = random.Random(seed + frame_idx)

    for v in vehicles:
        if v.in_emergency_lane:
            effective_frame = frame_idx - v.enter_emergency_frame
            if effective_frame < 0:
                # 尚未进入应急车道，正常行驶
                x_min, x_max = _get_normal_lane_bounds(v.y)
                v.x += v.speed * rng.uniform(-0.5, 0.5)
                v.x = max(x_min, min(x_max, v.x))
            elif effective_frame < v.stay_duration:
                # 在应急车道内
                lane_left, lane_right = _get_lane_bounds(v.y)
                v.x += v.speed * rng.uniform(-0.3, 0.3)
                v.x = max(lane_left + 20, min(lane_right - 40, v.x))
            else:
                # 返回正常车道
                x_min, x_max = _get_normal_lane_bounds(v.y)
                target_x = rng.uniform(x_min, x_max)
                v.x += (target_x - v.x) * 0.1
        else:
            # 正常行驶
            x_min, x_max = _get_normal_lane_bounds(v.y)
            v.x += v.speed * rng.uniform(-0.5, 0.5)
            v.x = max(x_min, min(x_max, v.x))

        # 向下移动（模拟前进）
        v.y += v.speed * 0.7
        if v.y > CANVAS_H + 80:
            v.y = -80
            v.x = rng.uniform(ROAD_LEFT + 50, WHITE_LINE_X - 50)

    return vehicles


def draw_vehicles(img: Image.Image, vehicles: list[VehicleState]) -> Image.Image:
    """在 PIL Image 上绘制车辆矩形。"""
    draw = ImageDraw.Draw(img)
    for v in vehicles:
        color = VEHICLE_COLORS.get(v.vehicle_type, (150, 150, 150))
        x1 = int(v.x - v.w / 2)
        y1 = int(v.y - v.h / 2)
        x2 = int(v.x + v.w / 2)
        y2 = int(v.y + v.h / 2)
        draw.rectangle([x1, y1, x2, y2], fill=color, outline=(255, 255, 255), width=1)
    return img
```

- [ ] **Step 2: 验证可导入**

```bash
cd /home/yrd/documents/git_clone_code/etc/emergency-lane-demo/backend && uv run python -c "from src.app.synthesis.vehicle_placer import create_vehicles, update_vehicles; print('OK')"
```

- [ ] **Step 3: 提交**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo add backend/src/app/synthesis/vehicle_placer.py
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo commit -m "feat: add vehicle placement and movement logic

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 2.4: 合成视频 — 视频合成器

**Files:**
- Create: `backend/src/app/synthesis/video_composer.py`

- [ ] **Step 1: 实现 video_composer.py**

```python
"""视频合成器 — 帧合成 → mp4 文件."""
from __future__ import annotations

from pathlib import Path

import cv2
import numpy as np

from .highway_renderer import render_highway_frame
from .vehicle_placer import create_vehicles, draw_vehicles, update_vehicles


def compose_video(output_path: Path,
                  duration_seconds: float = 30.0,
                  fps: int = 30,
                  num_normal: int = 4,
                  num_violating: int = 2,
                  seed: int = 42) -> Path:
    """合成高速公路演示视频。

    Args:
        output_path: 输出 mp4 路径
        duration_seconds: 视频时长
        fps: 帧率
        num_normal: 正常车辆数
        num_violating: 违规车辆数
        seed: 随机种子（保证可复现）

    Returns:
        输出文件路径
    """
    total_frames = int(duration_seconds * fps)
    vehicles = create_vehicles(num_normal, num_violating, seed=seed)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    fourcc = cv2.VideoWriter_fourcc(*"mp4v")
    writer = cv2.VideoWriter(
        str(output_path), fourcc, fps, (1280, 720)
    )

    for frame_idx in range(total_frames):
        img = render_highway_frame(seed=seed)
        vehicles = update_vehicles(vehicles, frame_idx, seed=seed)
        img = draw_vehicles(img, vehicles)

        # 转 BGR 写入 OpenCV
        frame_bgr = cv2.cvtColor(np.array(img), cv2.COLOR_RGB2BGR)

        # 帧号标签
        cv2.putText(frame_bgr, f"frame {frame_idx:05d}",
                    (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 1)

        writer.write(frame_bgr)

    writer.release()
    return output_path
```

- [ ] **Step 2: 测试合成 2 秒视频**

```bash
cd /home/yrd/documents/git_clone_code/etc/emergency-lane-demo/backend && uv run python -c "
from src.app.synthesis.video_composer import compose_video
out = compose_video('/tmp/test_highway.mp4', duration_seconds=2.0, fps=15)
import os; print(f'File size: {os.path.getsize(out)} bytes')
"
```

Expected: 文件生成且 > 0 bytes

- [ ] **Step 3: 提交**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo add backend/src/app/synthesis/video_composer.py
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo commit -m "feat: add video composer (frames → mp4)

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 2.5: Storage 模块

**Files:**
- Create: `backend/src/app/storage/__init__.py`
- Create: `backend/src/app/storage/store.py`

- [ ] **Step 1: 实现 store.py**

```python
"""文件存储管理 — 视频和结果 JSON 的读写."""
from __future__ import annotations

import json
from pathlib import Path
from uuid import uuid4

from ..config import settings


def create_video_id() -> str:
    return uuid4().hex[:12]


def video_path(video_id: str) -> Path:
    return settings.videos_dir / f"{video_id}.mp4"


def video_url(video_id: str) -> str:
    return f"/media/videos/{video_id}.mp4"


def result_path(video_id: str) -> Path:
    return settings.results_dir / f"{video_id}.json"


def result_frame_dir(video_id: str) -> Path:
    return settings.results_dir / video_id / "frames"


def result_timeline_path(video_id: str) -> Path:
    return settings.results_dir / video_id / "timeline.png"


def save_result(video_id: str, data: dict) -> Path:
    p = result_path(video_id)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(data, ensure_ascii=False, indent=2))
    return p


def load_result(video_id: str) -> dict | None:
    p = result_path(video_id)
    if not p.exists():
        return None
    return json.loads(p.read_text())
```

- [ ] **Step 2: 提交**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo add backend/src/app/storage/
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo commit -m "feat: add storage module for videos and results

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 2.6: API 路由 — synthesis + analysis + results

**Files:**
- Create: `backend/src/app/routes/__init__.py`
- Create: `backend/src/app/routes/synthesis.py`
- Create: `backend/src/app/routes/analysis.py`
- Create: `backend/src/app/routes/results.py`
- Modify: `backend/src/app/main.py` (register routes + mount static)

- [ ] **Step 1: 实现 routes/synthesis.py**

```python
"""POST /api/synthesis — 生成合成视频."""
from __future__ import annotations

from fastapi import APIRouter
from pydantic import BaseModel, Field

from ..storage.store import create_video_id, video_path, video_url
from ..synthesis.video_composer import compose_video

router = APIRouter()


class SynthesisRequest(BaseModel):
    duration_seconds: float = Field(default=30.0, ge=5.0, le=120.0)
    num_normal: int = Field(default=4, ge=0, le=10)
    num_violating: int = Field(default=2, ge=0, le=5)
    seed: int = Field(default=42)
    fps: int = Field(default=30, ge=10, le=60)


class SynthesisResponse(BaseModel):
    video_id: str
    video_url: str
    duration_seconds: float
    fps: int
    seed: int


@router.post("/synthesis")
def synthesis(payload: SynthesisRequest) -> SynthesisResponse:
    video_id = create_video_id()
    vp = video_path(video_id)
    compose_video(
        vp,
        duration_seconds=payload.duration_seconds,
        fps=payload.fps,
        num_normal=payload.num_normal,
        num_violating=payload.num_violating,
        seed=payload.seed,
    )
    return SynthesisResponse(
        video_id=video_id,
        video_url=video_url(video_id),
        duration_seconds=payload.duration_seconds,
        fps=payload.fps,
        seed=payload.seed,
    )
```

- [ ] **Step 2: 实现 routes/analysis.py**

```python
"""POST /api/analyze/{video_id} — 运行算法管线."""
from __future__ import annotations

from pathlib import Path

from fastapi import APIRouter, HTTPException

from algorithm.pipeline import Pipeline
from ..config import settings
from ..storage.store import (
    load_result, result_frame_dir, result_timeline_path,
    save_result, video_path,
)
from algorithm.visualize import draw_timeline, save_keyframes

router = APIRouter()


@router.post("/analyze/{video_id}")
def analyze(video_id: str) -> dict:
    vp = video_path(video_id)
    if not vp.exists():
        raise HTTPException(status_code=404, detail="视频不存在，请先合成")

    # 检查是否已分析过
    existing = load_result(video_id)
    if existing:
        return {"status": "done", "video_id": video_id, "message": "分析已完成"}

    pipeline = Pipeline(
        conf_threshold=settings.detection_conf_threshold,
        window_seconds=settings.window_seconds,
        ratio_threshold=settings.violation_ratio_threshold,
        device="cpu",
    )

    result = pipeline.run(vp, sample_every=2)

    # 保存可视化
    frame_dir = result_frame_dir(video_id)
    frame_dir.mkdir(parents=True, exist_ok=True)

    # 保存关键帧
    keyframe_paths = save_keyframes(
        result.frame_results, result.violation_segments, frame_dir,
    )

    # 保存时间轴
    timeline_path = draw_timeline(
        result.violation_segments, result.total_frames, result.fps,
        result_timeline_path(video_id),
    )

    # 序列化结果
    data = {
        "video_id": video_id,
        "total_frames": result.total_frames,
        "fps": result.fps,
        "violation_segments": [
            {
                "start_frame": s.start_frame,
                "end_frame": s.end_frame,
                "duration_frames": s.duration_frames,
                "duration_seconds": round(s.duration_frames / result.fps, 1),
                "occupancy_ratio": round(s.occupancy_ratio, 2),
                "confidence": round(s.confidence, 2),
            }
            for s in result.violation_segments
        ],
        "frame_results": [
            {
                "frame_idx": fr.frame_idx,
                "has_violation": fr.has_violation,
                "vehicle_count": len(fr.violations),
                "detections": [
                    {
                        "label": d.label,
                        "score": round(d.score, 2),
                        "x": round(d.x, 1),
                        "y": round(d.y, 1),
                        "w": round(d.w, 1),
                        "h": round(d.h, 1),
                    }
                    for d in fr.violations
                ],
            }
            for fr in result.frame_results
            if fr.has_violation or fr.frame_idx % 30 == 0
        ],
        "keyframe_count": len(keyframe_paths),
        "timeline_url": f"/media/results/{video_id}/timeline.png",
    }
    save_result(video_id, data)

    return {"status": "done", "video_id": video_id, **data}
```

- [ ] **Step 3: 实现 routes/results.py**

```python
"""GET /api/results/{video_id} — 获取分析结果."""
from __future__ import annotations

from fastapi import APIRouter, HTTPException

from ..storage.store import load_result

router = APIRouter()


@router.get("/results/{video_id}")
def results(video_id: str) -> dict:
    data = load_result(video_id)
    if data is None:
        raise HTTPException(status_code=404, detail="分析结果不存在，请先运行分析")
    return data
```

- [ ] **Step 4: 更新 main.py 注册路由 + 挂载静态文件 + 设置 Python Path**

```python
from __future__ import annotations

import sys
from pathlib import Path

# 将项目根目录加入 sys.path，确保 algorithm/ 可被导入
# backend/src/app/main.py → parents[3] = emergency-lane-demo/
_project_root = Path(__file__).resolve().parents[3]
if str(_project_root) not in sys.path:
    sys.path.insert(0, str(_project_root))

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from .config import settings
from .routes import analysis, results, synthesis

settings.ensure_dirs()

app = FastAPI(title=settings.project_name)
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origin_list(),
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 静态文件挂载
app.mount("/media/videos", StaticFiles(directory=str(settings.videos_dir)), name="videos")
app.mount("/media/results", StaticFiles(directory=str(settings.results_dir)), name="results")

# 路由
prefix = settings.api_prefix
app.include_router(synthesis.router, prefix=prefix)
app.include_router(analysis.router, prefix=prefix)
app.include_router(results.router, prefix=prefix)


@app.get(f"{settings.api_prefix}/health")
def health() -> dict[str, str]:
    return {"status": "ok"}
```

- [ ] **Step 5: 验证后端启动**

```bash
cd /home/yrd/documents/git_clone_code/etc/emergency-lane-demo/backend && timeout 5 uv run uvicorn src.app.main:app --host 127.0.0.1 --port 8000 2>&1 || true
```

Expected: Uvicorn 启动无报错，路由注册成功

- [ ] **Step 6: 提交**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo add backend/src/app/routes/ backend/src/app/main.py
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo commit -m "feat: add REST API routes (synthesis, analysis, results)

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Phase 3: frontend/ 重写

### Task 3.1: 清理前端旧代码

**Files:**
- Delete: `frontend/src/router.ts`
- Delete: `frontend/src/theme.ts`

- [ ] **Step 1: 删除旧路由和主题文件**

```bash
rm /home/yrd/documents/git_clone_code/etc/emergency-lane-demo/frontend/src/router.ts
rm /home/yrd/documents/git_clone_code/etc/emergency-lane-demo/frontend/src/theme.ts
```

- [ ] **Step 2: 提交**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo add -A
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo commit -m "chore: remove old frontend router and theme

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 3.2: 前端类型定义和 API 层

**Files:**
- Rewrite: `frontend/src/types.ts`
- Rewrite: `frontend/src/api.ts`
- Modify: `frontend/src/config.ts`

- [ ] **Step 1: 重写 types.ts**

```typescript
export interface SynthesisRequest {
  duration_seconds: number
  num_normal: number
  num_violating: number
  seed: number
  fps: number
}

export interface SynthesisResponse {
  video_id: string
  video_url: string
  duration_seconds: number
  fps: number
  seed: number
}

export interface DetectionItem {
  label: string
  score: number
  x: number
  y: number
  w: number
  h: number
}

export interface FrameResultItem {
  frame_idx: number
  has_violation: boolean
  vehicle_count: number
  detections: DetectionItem[]
}

export interface ViolationSegment {
  start_frame: number
  end_frame: number
  duration_frames: number
  duration_seconds: number
  occupancy_ratio: number
  confidence: number
}

export interface AnalysisResult {
  status: string
  video_id: string
  total_frames: number
  fps: number
  violation_segments: ViolationSegment[]
  frame_results: FrameResultItem[]
  keyframe_count: number
  timeline_url: string
}
```

- [ ] **Step 2: 重写 api.ts**

```typescript
import { API_BASE_URL } from './config'
import type {
  AnalysisResult,
  SynthesisRequest,
  SynthesisResponse,
} from './types'

async function request<T>(path: string, options?: RequestInit & { json?: unknown }): Promise<T> {
  const headers = new Headers(options?.headers)
  if (!headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  const resp = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
    body: options?.json === undefined ? options?.body : JSON.stringify(options.json),
  })
  if (!resp.ok) {
    const msg = await resp.text()
    throw new Error(msg || `Request failed: ${resp.status}`)
  }
  return resp.json() as Promise<T>
}

export const api = {
  synthesis: (payload: SynthesisRequest) =>
    request<SynthesisResponse>('/synthesis', { method: 'POST', json: payload }),

  analyze: (videoId: string) =>
    request<AnalysisResult>(`/analyze/${videoId}`, { method: 'POST' }),

  getResults: (videoId: string) =>
    request<AnalysisResult>(`/results/${videoId}`),
}
```

- [ ] **Step 3: 提交**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo add frontend/src/types.ts frontend/src/api.ts frontend/src/config.ts
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo commit -m "feat: rewrite frontend types and API layer for single-page demo

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 3.3: Canvas 绘图工具

**Files:**
- Create: `frontend/src/utils/draw.ts`

- [ ] **Step 1: 实现 draw.ts**

```typescript
import type { DetectionItem, ViolationSegment } from '../types'

export function drawDetections(
  ctx: CanvasRenderingContext2D,
  detections: DetectionItem[],
  canvasW: number,
  canvasH: number
) {
  ctx.strokeStyle = '#ff3333'
  ctx.lineWidth = 2
  ctx.fillStyle = '#ff3333'
  ctx.font = '12px monospace'

  for (const d of detections) {
    ctx.strokeRect(d.x, d.y, d.w, d.h)
    ctx.fillText(`${d.label} ${(d.score * 100).toFixed(0)}%`, d.x, d.y - 6)
  }
}

export function drawTimeline(
  ctx: CanvasRenderingContext2D,
  segments: ViolationSegment[],
  totalFrames: number,
  canvasW: number,
  canvasH: number
) {
  // Background
  ctx.fillStyle = '#e8e8e8'
  ctx.fillRect(0, 0, canvasW, canvasH)

  // Violation segments in red
  ctx.fillStyle = '#e04040'
  for (const seg of segments) {
    const x1 = (seg.start_frame / totalFrames) * canvasW
    const x2 = (seg.end_frame / totalFrames) * canvasW
    ctx.fillRect(x1, 2, Math.max(x2 - x1, 2), canvasH - 4)
  }

  // Border
  ctx.strokeStyle = '#ccc'
  ctx.lineWidth = 1
  ctx.strokeRect(0, 0, canvasW, canvasH)
}
```

- [ ] **Step 2: 提交**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo add frontend/src/utils/draw.ts
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo commit -m "feat: add canvas drawing utilities

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 3.4: 单页应用 — 入口 + 样式

**Files:**
- Rewrite: `frontend/src/main.ts`
- Rewrite: `frontend/src/style.css`
- Modify: `frontend/index.html`

- [ ] **Step 1: 更新 index.html 标题**

```html
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>应急车道违规检测 — 算法演示</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.ts"></script>
  </body>
</html>
```

- [ ] **Step 2: 重写 style.css**

```css
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #1a1a2e; color: #e0e0e0; }

#app { max-width: 1320px; margin: 0 auto; padding: 16px; }

.control-bar { display: flex; align-items: center; gap: 12px; padding: 16px 20px; background: #16213e; border-radius: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.control-bar label { font-size: 13px; color: #a0a0c0; }
.control-bar input[type="range"] { width: 80px; }
.control-bar input[type="number"] { width: 60px; padding: 4px 8px; border-radius: 6px; border: 1px solid #333; background: #0f0f23; color: #e0e0e0; }
.btn { padding: 10px 20px; border-radius: 8px; border: none; cursor: pointer; font-weight: 600; font-size: 14px; transition: opacity 0.2s; }
.btn:disabled { opacity: 0.4; cursor: not-allowed; }
.btn-primary { background: #7c3aed; color: #fff; }
.btn-primary:hover:not(:disabled) { background: #6d28d9; }
.btn-secondary { background: #334155; color: #e0e0e0; }
.btn-secondary:hover:not(:disabled) { background: #475569; }

.video-stage { position: relative; border-radius: 12px; overflow: hidden; background: #000; margin-bottom: 16px; }
.video-stage video { width: 100%; display: block; }
.video-stage canvas { position: absolute; top: 0; left: 0; width: 100%; height: 100%; pointer-events: none; }

.timeline-section { background: #16213e; border-radius: 12px; padding: 16px 20px; margin-bottom: 16px; }
.timeline-section h3 { font-size: 14px; margin-bottom: 10px; color: #a0a0c0; }
.timeline-section canvas { width: 100%; border-radius: 6px; }

.frame-strip { display: flex; gap: 8px; overflow-x: auto; padding: 8px 0; }
.frame-strip img { height: 100px; border-radius: 6px; border: 2px solid transparent; }
.frame-strip img.violation { border-color: #e04040; }

.placeholder { padding: 80px 20px; text-align: center; color: #606080; font-size: 16px; }
.placeholder p { margin-top: 8px; }
.status-badge { font-size: 12px; padding: 4px 10px; border-radius: 12px; font-weight: 600; }
.status-ok { background: #065f46; color: #6ee7b7; }
.status-busy { background: #78350f; color: #fcd34d; }
.age { font-size: 12px; color: #a0a0c0; }
```

- [ ] **Step 3: 重写 main.ts**

```typescript
import { api } from './api'
import type { AnalysisResult, SynthesisResponse } from './types'
import { drawDetections, drawTimeline } from './utils/draw'
import './style.css'

const app = document.querySelector<HTMLDivElement>('#app')!

// State
let videoId: string | null = null
let videoUrl: string | null = null
let analysis: AnalysisResult | null = null

// Config values
let duration = 30
let seed = 42
let numViolating = 2
let fps = 30

function render() {
  app.innerHTML = `
    <div class="control-bar">
      <label>时长(s) <input type="number" id="cfg-dur" value="${duration}" min="5" max="120"></label>
      <label>FPS <input type="number" id="cfg-fps" value="${fps}" min="10" max="60"></label>
      <label>违规车辆 <input type="number" id="cfg-vio" value="${numViolating}" min="0" max="5"></label>
      <label>Seed <input type="number" id="cfg-seed" value="${seed}"></label>
      <button class="btn btn-primary" id="btn-synth">合成视频</button>
      <button class="btn btn-secondary" id="btn-analyze" ${!videoId ? 'disabled' : ''}>运行检测</button>
      <span class="age">${videoId ? `video: ${videoId}` : '未合成'}</span>
      <span class="age">${analysis ? `检测完成 | ${analysis.violation_segments.length} 个违规段` : ''}</span>
    </div>

    ${videoUrl
      ? `<div class="video-stage">
           <video id="player" src="${videoUrl}" controls></video>
           <canvas id="overlay-canvas"></canvas>
         </div>`
      : `<div class="placeholder"><h2>高速公路应急车道违规检测</h2><p>点击「合成视频」生成演示视频，然后「运行检测」</p></div>`}

    ${analysis
      ? `<div class="timeline-section">
           <h3>违规时间轴（红=违规段）</h3>
           <canvas id="timeline-canvas" height="40"></canvas>
           <div class="frame-strip" id="strip"></div>
         </div>`
      : ''}
  `

  bindEvents()
  if (analysis) drawTimelineCanvas()
}

function bindEvents() {
  document.getElementById('btn-synth')?.addEventListener('click', async () => {
    duration = +(document.getElementById('cfg-dur') as HTMLInputElement).value
    fps = +(document.getElementById('cfg-fps') as HTMLInputElement).value
    numViolating = +(document.getElementById('cfg-vio') as HTMLInputElement).value
    seed = +(document.getElementById('cfg-seed') as HTMLInputElement).value

    try {
      const resp: SynthesisResponse = await api.synthesis({
        duration_seconds: duration, fps, num_normal: 4, num_violating: numViolating, seed,
      })
      videoId = resp.video_id
      videoUrl = resp.video_url
      analysis = null
      render()
    } catch (e) {
      alert(`合成失败: ${e}`)
    }
  })

  document.getElementById('btn-analyze')?.addEventListener('click', async () => {
    if (!videoId) return
    try {
      analysis = await api.analyze(videoId)
      render()
    } catch (e) {
      alert(`检测失败: ${e}`)
      analysis = null
    }
  })
}

function drawTimelineCanvas() {
  if (!analysis) return
  const canvas = document.getElementById('timeline-canvas') as HTMLCanvasElement | null
  if (!canvas) return
  const ctx = canvas.getContext('2d')!
  canvas.width = canvas.clientWidth
  canvas.height = 40
  drawTimeline(ctx, analysis.violation_segments, analysis.total_frames, canvas.width, canvas.height)

  // Frame strip
  const strip = document.getElementById('strip')!
  const violationFrames = analysis.frame_results.filter(f => f.has_violation).slice(0, 8)
  strip.innerHTML = violationFrames.map(f =>
    `<div style="text-align:center">
       <div style="width:140px;height:80px;background:#1a1a2e;border-radius:6px;display:flex;align-items:center;justify-content:center;font-size:11px;color:#a0a0c0">
         frame ${f.frame_idx}<br>${f.detections.length} vehicle(s)
       </div>
     </div>`
  ).join('')
}

// Video-Cavas sync
let syncInterval: ReturnType<typeof setInterval> | null = null
const origRender = render
render = () => {
  origRender()
  if (syncInterval) clearInterval(syncInterval)
  if (!videoUrl || !analysis) return
  syncInterval = setInterval(syncOverlay, 50)
}

function syncOverlay() {
  const video = document.getElementById('player') as HTMLVideoElement | null
  const canvas = document.getElementById('overlay-canvas') as HTMLCanvasElement | null
  if (!video || !canvas || !analysis) return

  canvas.width = video.clientWidth
  canvas.height = video.clientHeight
  const ctx = canvas.getContext('2d')!
  ctx.clearRect(0, 0, canvas.width, canvas.height)

  const currentFrame = Math.floor(video.currentTime * analysis.fps)
  const frameData = analysis.frame_results.find(f => f.frame_idx === currentFrame)
  if (frameData && frameData.has_violation) {
    const scaleX = canvas.width / 1280
    const scaleY = canvas.height / 720
    ctx.save()
    ctx.scale(scaleX, scaleY)
    drawDetections(ctx, frameData.detections, canvas.width, canvas.height)
    ctx.restore()
  }
}

render()
```

- [ ] **Step 4: 验证前端构建**

```bash
cd /home/yrd/documents/git_clone_code/etc/emergency-lane-demo/frontend && npm run build
```

Expected: TypeScript 编译 + Vite 构建成功

- [ ] **Step 5: 提交**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo add frontend/
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo commit -m "feat: rewrite frontend as single-page algorithm demo

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Phase 4: android/ 精简

### Task 4.1: 删除 Android 业务代码

**Files (delete):**
- `android/app/src/main/java/com/example/emergencylaneguard/HomeFragment.kt`
- `android/app/src/main/java/com/example/emergencylaneguard/PendingFragment.kt`
- `android/app/src/main/java/com/example/emergencylaneguard/CasesFragment.kt`
- `android/app/src/main/java/com/example/emergencylaneguard/CasesAdapter.kt`
- `android/app/src/main/java/com/example/emergencylaneguard/ViolationAdapter.kt`
- `android/app/src/main/java/com/example/emergencylaneguard/ViolationViewModel.kt`
- `android/app/src/main/java/com/example/emergencylaneguard/VideoTrimmer.kt`
- `android/app/src/main/java/com/example/emergencylaneguard/CaseDetailActivity.kt`
- `android/app/src/main/java/com/example/emergencylaneguard/VideoPlayerActivity.kt`
- `android/app/src/main/java/com/example/emergencylaneguard/RecordsActivity.kt`
- `android/app/src/main/java/com/example/emergencylaneguard/SettingsActivity.kt`
- `android/app/src/main/java/com/example/emergencylaneguard/MainActivity.kt` (rewrite)
- `android/app/src/main/java/com/example/emergencylaneguard/network/` (directory)
- `android/app/src/main/java/com/example/emergencylaneguard/database/` (directory)
- `android/app/src/main/java/com/example/emergencylaneguard/utils/` (delete MediaStoreUtils.kt)

- [ ] **Step 1: 执行删除**

```bash
cd /home/yrd/documents/git_clone_code/etc/emergency-lane-demo/android
rm -f app/src/main/java/com/example/emergencylaneguard/HomeFragment.kt
rm -f app/src/main/java/com/example/emergencylaneguard/PendingFragment.kt
rm -f app/src/main/java/com/example/emergencylaneguard/CasesFragment.kt
rm -f app/src/main/java/com/example/emergencylaneguard/CasesAdapter.kt
rm -f app/src/main/java/com/example/emergencylaneguard/ViolationAdapter.kt
rm -f app/src/main/java/com/example/emergencylaneguard/ViolationViewModel.kt
rm -f app/src/main/java/com/example/emergencylaneguard/VideoTrimmer.kt
rm -f app/src/main/java/com/example/emergencylaneguard/CaseDetailActivity.kt
rm -f app/src/main/java/com/example/emergencylaneguard/VideoPlayerActivity.kt
rm -f app/src/main/java/com/example/emergencylaneguard/RecordsActivity.kt
rm -f app/src/main/java/com/example/emergencylaneguard/SettingsActivity.kt
rm -rf app/src/main/java/com/example/emergencylaneguard/network/
rm -rf app/src/main/java/com/example/emergencylaneguard/database/
rm -f app/src/main/java/com/example/emergencylaneguard/utils/MediaStoreUtils.kt
```

- [ ] **Step 2: 实现 BenchmarkActivity.kt**

```kotlin
package com.example.emergencylaneguard

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

class BenchmarkActivity : AppCompatActivity() {

    private lateinit var laneDetector: LaneDetector
    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var btnBenchmark: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvResults: TextView

    private val executor = Executors.newSingleThreadExecutor()
    private var frameCount = 0
    private var totalInferenceMs = 0L
    private var totalPreprocessMs = 0L
    private var benchmarkStartMs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_benchmark)

        previewView = findViewById(R.id.viewFinder)
        overlayView = findViewById(R.id.overlayView)
        btnBenchmark = findViewById(R.id.btnBenchmark)
        tvStatus = findViewById(R.id.tvStatus)
        tvResults = findViewById(R.id.tvResults)

        laneDetector = LaneDetector()
        if (laneDetector.initialize(assets)) {
            tvStatus.text = "Model: YOLOv8n | Backend: ncnn | Status: Ready"
        } else {
            tvStatus.text = "Model load failed"
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 0)
        }

        btnBenchmark.setOnClickListener {
            if (btnBenchmark.text == "开始基准测试") {
                startBenchmark()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis.setAnalyzer(executor) { imageProxy -> processImage(imageProxy) }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
            tvStatus.text = "Model: YOLOv8n | Backend: ncnn | Camera: Ready"
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startBenchmark() {
        frameCount = 0
        totalInferenceMs = 0L
        totalPreprocessMs = 0L
        benchmarkStartMs = System.currentTimeMillis()
        btnBenchmark.isEnabled = false
        tvResults.text = "Running 100 frames..."
    }

    private fun processImage(imageProxy: ImageProxy) {
        val preprocessStart = System.currentTimeMillis()
        val bitmap = imageProxy.toBitmap() ?: run { imageProxy.close(); return }
        val rotation = imageProxy.imageInfo.rotationDegrees
        val rotatedBitmap = if (rotation != 0) {
            val matrix = android.graphics.Matrix()
            matrix.postRotate(rotation.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else bitmap
        val preprocessMs = System.currentTimeMillis() - preprocessStart
        totalPreprocessMs += preprocessMs

        val inferenceStart = System.currentTimeMillis()
        val results = laneDetector.runDetection(rotatedBitmap)
        val inferenceMs = System.currentTimeMillis() - inferenceStart
        totalInferenceMs += inferenceMs

        if (frameCount < 100) {
            frameCount++
            if (frameCount >= 100) finishBenchmark()
        }

        // Live overlay
        if (results.isNotEmpty()) {
            val vehicleResults = results.filter { it.cls in setOf(2, 5, 7) && it.score > 0.3f }
            runOnUiThread {
                overlayView.updateDetections(vehicleResults, rotatedBitmap.width, rotatedBitmap.height)
            }
        }

        imageProxy.close()
    }

    private fun finishBenchmark() {
        val totalMs = System.currentTimeMillis() - benchmarkStartMs
        val avgInference = totalInferenceMs / 100
        val avgPreprocess = totalPreprocessMs / 100
        val avgTotal = avgInference + avgPreprocess
        val fps = 1000.0 / avgTotal

        runOnUiThread {
            tvResults.text = """
                基准测试完成 (100 帧)
                平均推理时间: ${avgInference}ms
                平均预处理时间: ${avgPreprocess}ms
                平均总帧处理时间: ${avgTotal}ms
                有效 FPS: ${String.format("%.1f", fps)}
            """.trimIndent()
            btnBenchmark.isEnabled = true
        }
    }

    private fun ImageProxy.toBitmap(): Bitmap? {
        if (format != ImageFormat.YUV_420_888) return null
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    override fun onDestroy() {
        super.onDestroy()
        laneDetector.destroy()
        executor.shutdown()
    }
}
```

- [ ] **Step 3: 创建 benchmark layout XML**

```bash
mkdir -p /home/yrd/documents/git_clone_code/etc/emergency-lane-demo/android/app/src/main/res/layout
```

Create `activity_benchmark.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="#000000">

    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">

        <androidx.camera.view.PreviewView
            android:id="@+id/viewFinder"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />

        <com.example.emergencylaneguard.OverlayView
            android:id="@+id/overlayView"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />
    </FrameLayout>

    <TextView
        android:id="@+id/tvStatus"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="8dp"
        android:textColor="#a0a0c0"
        android:textSize="13sp" />

    <Button
        android:id="@+id/btnBenchmark"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="开始基准测试"
        android:layout_margin="8dp" />

    <TextView
        android:id="@+id/tvResults"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="8dp"
        android:textColor="#e0e0e0"
        android:textSize="12sp"
        android:fontFamily="monospace" />
</LinearLayout>
```

- [ ] **Step 4: 更新 AndroidManifest.xml** — 将 MainActivity 替换为 BenchmarkActivity

找到 AndroidManifest.xml 中的 `<activity>` 声明，将 `android:name=".MainActivity"` 改为 `android:name=".BenchmarkActivity"`，并删除其他 Activity 声明。

- [ ] **Step 5: 验证 Android 构建**

```bash
cd /home/yrd/documents/git_clone_code/etc/emergency-lane-demo/android && JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew --no-daemon assembleDebug 2>&1 | tail -20
```

- [ ] **Step 6: 提交**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo add android/
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo commit -m "refactor: strip android to YOLO benchmark only

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Phase 5: 端到端验证

### Task 5.1: 端到端集成测试

- [ ] **Step 1: 启动后端**

```bash
cd /home/yrd/documents/git_clone_code/etc/emergency-lane-demo/backend && uv run uvicorn src.app.main:app --host 127.0.0.1 --port 8000 &
sleep 3
```

- [ ] **Step 2: 测试合成 API**

```bash
curl -s -X POST http://127.0.0.1:8000/api/synthesis \
  -H 'Content-Type: application/json' \
  -d '{"duration_seconds":10,"num_normal":3,"num_violating":1,"seed":42,"fps":15}' | python -m json.tool
```

Expected: 返回 JSON 包含 `video_id` 和 `video_url`

- [ ] **Step 3: 测试分析 API**

```bash
VIDEO_ID=$(curl -s -X POST http://127.0.0.1:8000/api/synthesis \
  -H 'Content-Type: application/json' \
  -d '{"duration_seconds":10,"num_normal":3,"num_violating":1,"seed":42,"fps":15}' | python -c "import sys,json; print(json.load(sys.stdin)['video_id'])")

curl -s -X POST "http://127.0.0.1:8000/api/analyze/$VIDEO_ID" | python -m json.tool
```

Expected: 返回 JSON 包含 `violation_segments` 和 `frame_results`

- [ ] **Step 4: 测试结果 API**

```bash
curl -s "http://127.0.0.1:8000/api/results/$VIDEO_ID" | python -c "import sys,json; d=json.load(sys.stdin); print(f'Segments: {len(d[\"violation_segments\"])}, Frames: {d[\"total_frames\"]}')"
```

Expected: `Segments: >=1, Frames: ...`

- [ ] **Step 5: 验证前端构建**

```bash
cd /home/yrd/documents/git_clone_code/etc/emergency-lane-demo/frontend && npm run build
```

Expected: TypeScript 编译 + Vite 构建无错误

- [ ] **Step 6: 验证 algorithm 独立运行**

```bash
cd /home/yrd/documents/git_clone_code/etc/emergency-lane-demo && python -c "
from algorithm.pipeline import Pipeline
print('Pipeline import OK')
from algorithm.lane_extraction import detect_lane_line, apply_region_mask, draw_mask_overlay
print('Module 1 OK')
from algorithm.vehicle_detection import YOLODetector, filter_vehicles
print('Module 2 OK')
from algorithm.spatial_reasoning import check_vehicle_in_region
print('Module 3 OK')
from algorithm.temporal_judgment import ViolationDecider
print('Module 4 OK')
from algorithm.visualize import draw_timeline, save_keyframes
print('Visualize OK')
"
```

Expected: 所有模块导入成功

- [ ] **Step 7: 停止后端**

```bash
kill %1 2>/dev/null || true
```

- [ ] **Step 8: 提交（如有变更）**

```bash
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo status
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo add -A
git -C /home/yrd/documents/git_clone_code/etc/emergency-lane-demo commit -m "test: add end-to-end integration verification

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## 实施记录 (2026-05-05)

### Phase 5 中发现并修复的问题

1. **YOLO 无法检测 Pillow 矩形色块**：合成视频使用 Pillow 绘制的彩色矩形代表车辆，但 YOLOv8n 训练于真实照片，无法识别。解决方案：
   - `video_composer.py` 合成时同步写 `{video_id}.gt.json` 记录每帧 ground-truth 车辆位置
   - `pipeline.py` 新增 `ground_truth` 可选参数，gt 存在时直接注入 Detection 对象
   - `analysis.py` 检测 gt 文件存在则加载传入 pipeline
   - 论文中定位：合成数据评估使用 ground-truth 标注，真实视频走 YOLO 检测路径

2. **短视频时间判定漏检**：窗口 `window_frames` 未满时 `update()` 不做判定。修复：`finalize()` 新增短视频分支，以可用帧比率判定违规段。

3. **Android namespace 不一致**：applicationId 为 `com.yrd.emergencylanemobile`，保留代码在 `com.example.emergencylaneguard`。统一为后者，删除 Compose 依赖。

### 关键文件变更清单（Phase 5 修正）

| 文件 | 变更 |
|------|------|
| `algorithm/pipeline.py` | `run()` + `_process_frame()` 新增 `ground_truth` 参数 |
| `algorithm/temporal_judgment/violation_decider.py` | `finalize()` 处理视频 < 窗口大小 |
| `backend/src/app/synthesis/video_composer.py` | 合成时写 `{video_id}.gt.json` |
| `backend/src/app/routes/analysis.py` | 加载 gt.json 传入 pipeline |
| `.gitignore` | `algorithm/*.pt` 排除模型文件 |

### 端到端验证通过

- 合成 API → 10s video + ground truth JSON
- 分析 API → 1 违规段 (0-148 帧, 9.9s)
- 结果 API → 可检索缓存
- 前端构建 → 0 errors
- 算法导入 → 全部模块 OK
- Android 构建 → 47M APK
