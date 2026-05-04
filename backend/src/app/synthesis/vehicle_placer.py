"""车辆放置逻辑 — 在正常车道和应急车道移动车辆."""
from __future__ import annotations

from dataclasses import dataclass
import random

from PIL import Image, ImageDraw


CANVAS_W = 1280
CANVAS_H = 720

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
    vehicle_id: int
    vehicle_type: str
    x: float
    y: float
    w: int = 80
    h: int = 40
    speed: float = 3.0
    in_emergency_lane: bool = False
    enter_emergency_frame: int | None = None
    stay_duration: int = 180


def _get_lane_bounds(y: float) -> tuple[float, float]:
    t = y / CANVAS_H
    return WHITE_LINE_X + (WHITE_LINE_BOTTOM_X - WHITE_LINE_X) * t, ROAD_RIGHT


def _get_normal_lane_bounds(y: float) -> tuple[float, float]:
    lane_right = WHITE_LINE_X + (WHITE_LINE_BOTTOM_X - WHITE_LINE_X) * (y / CANVAS_H) - 60
    return ROAD_LEFT + 50, lane_right


def create_vehicles(num_normal: int, num_violating: int,
                    seed: int = 42) -> list[VehicleState]:
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
        x = x_min + rng.uniform(30, 200)
        vehicles.append(VehicleState(
            vehicle_id=vid,
            vehicle_type="car",
            x=x, y=y,
            speed=rng.uniform(1.5, 3.0),
            in_emergency_lane=True,
            enter_emergency_frame=rng.randint(30, 60),
            stay_duration=rng.randint(240, 450),
        ))
        vid += 1

    return vehicles


def update_vehicles(vehicles: list[VehicleState],
                    frame_idx: int,
                    seed: int = 42) -> list[VehicleState]:
    rng = random.Random(seed + frame_idx)

    for v in vehicles:
        if v.in_emergency_lane:
            effective_frame = frame_idx - (v.enter_emergency_frame or 0)
            if effective_frame < 0:
                x_min, x_max = _get_normal_lane_bounds(v.y)
                v.x += v.speed * rng.uniform(-0.5, 0.5)
                v.x = max(x_min, min(x_max, v.x))
            elif effective_frame < v.stay_duration:
                lane_left, lane_right = _get_lane_bounds(v.y)
                v.x += v.speed * rng.uniform(-0.3, 0.3)
                v.x = max(lane_left + 20, min(lane_right - 40, v.x))
            else:
                x_min, x_max = _get_normal_lane_bounds(v.y)
                target_x = rng.uniform(x_min, x_max)
                v.x += (target_x - v.x) * 0.1
        else:
            x_min, x_max = _get_normal_lane_bounds(v.y)
            v.x += v.speed * rng.uniform(-0.5, 0.5)
            v.x = max(x_min, min(x_max, v.x))

        v.y += v.speed * 0.7
        if v.y > CANVAS_H + 80:
            v.y = -80
            v.x = rng.uniform(ROAD_LEFT + 50, WHITE_LINE_X - 50)

    return vehicles


def draw_vehicles(img: Image.Image, vehicles: list[VehicleState]) -> Image.Image:
    draw = ImageDraw.Draw(img)
    for v in vehicles:
        color = VEHICLE_COLORS.get(v.vehicle_type, (150, 150, 150))
        x1 = int(v.x - v.w / 2)
        y1 = int(v.y - v.h / 2)
        x2 = int(v.x + v.w / 2)
        y2 = int(v.y + v.h / 2)
        draw.rectangle([x1, y1, x2, y2], fill=color, outline=(255, 255, 255), width=1)
    return img
