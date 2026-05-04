"""高速路面渲染器 — Pillow 绘制双车道 + 应急车道."""
from __future__ import annotations

from PIL import Image, ImageDraw


# 画面尺寸
CANVAS_W = 1280
CANVAS_H = 720

# 车道几何（像素坐标）
ROAD_LEFT = 80
ROAD_RIGHT = 1100
WHITE_LINE_X = 680
WHITE_LINE_BOTTOM_X = 880

# 颜色
ROAD_COLOR = (60, 60, 65)
WHITE_LINE_COLOR = (230, 230, 235)
GRASS_COLOR = (95, 130, 65)


def render_highway_frame(seed: int = 0) -> Image.Image:
    """渲染单帧高速公路场景。"""
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
