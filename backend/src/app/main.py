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
