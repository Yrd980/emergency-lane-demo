from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    project_name: str = "高速公路应急车道违章辅助举报原型"
    app_env: str = "development"
    api_prefix: str = "/api"
    cors_origins: str = "http://127.0.0.1:5173,http://localhost:5173"
    zhipu_api_key: str = ""
    zhipu_model: str = "glm-4v-flash"
    use_mock_zhipu: bool = False
    frame_interval_seconds: float = 1.0
    violation_hold_seconds: float = 3.0
    location_label: str = "G60 沪昆高速 K12+300 测试路段"

    @property
    def backend_root(self) -> Path:
        return Path(__file__).resolve().parents[3]

    @property
    def data_dir(self) -> Path:
        return self.backend_root / "data"

    @property
    def storage_dir(self) -> Path:
        return self.backend_root / "storage"

    @property
    def evidence_dir(self) -> Path:
        return self.storage_dir / "evidence"

    @property
    def frame_dir(self) -> Path:
        return self.storage_dir / "frames"

    @property
    def db_path(self) -> Path:
        return self.storage_dir / "demo.db"

    @property
    def demo_video_path(self) -> Path:
        return self.data_dir / "demo_highway.mp4"

    @property
    def demo_manifest_path(self) -> Path:
        return self.data_dir / "demo_manifest.json"

    @property
    def demo_cover_path(self) -> Path:
        return self.data_dir / "demo_cover.jpg"

    def cors_origin_list(self) -> list[str]:
        return [item.strip() for item in self.cors_origins.split(",") if item.strip()]

    def ensure_dirs(self) -> None:
        self.data_dir.mkdir(parents=True, exist_ok=True)
        self.storage_dir.mkdir(parents=True, exist_ok=True)
        self.evidence_dir.mkdir(parents=True, exist_ok=True)
        self.frame_dir.mkdir(parents=True, exist_ok=True)


settings = Settings()

