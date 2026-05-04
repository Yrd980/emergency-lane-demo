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
