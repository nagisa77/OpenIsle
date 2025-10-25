"""Configuration helpers for the MCP service."""

from __future__ import annotations

import os
from functools import lru_cache
from typing import Any

from pydantic import BaseModel, ConfigDict, Field, ValidationError


class Settings(BaseModel):
    """Application settings sourced from environment variables."""

    host: str = Field(default="0.0.0.0", description="Host to bind the HTTP server to")
    port: int = Field(default=9090, ge=1, le=65535, description="Port exposed by the MCP server")
    backend_base_url: str = Field(
        default="http://springboot:8080",
        description="Base URL of the Spring Boot backend that provides search endpoints",
    )
    connect_timeout: float = Field(
        default=5.0,
        ge=0.0,
        description="Connection timeout when communicating with the backend (seconds)",
    )
    read_timeout: float = Field(
        default=10.0,
        ge=0.0,
        description="Read timeout when communicating with the backend (seconds)",
    )

    model_config = ConfigDict(extra="ignore")

    @property
    def normalized_backend_base_url(self) -> str:
        """Return the backend base URL without a trailing slash."""

        return self.backend_base_url.rstrip("/")


ENV_MAPPING: dict[str, str] = {
    "host": "MCP_HOST",
    "port": "MCP_PORT",
    "backend_base_url": "MCP_BACKEND_BASE_URL",
    "connect_timeout": "MCP_CONNECT_TIMEOUT",
    "read_timeout": "MCP_READ_TIMEOUT",
}


def _load_environment_values() -> dict[str, Any]:
    values: dict[str, Any] = {}
    for field, env_name in ENV_MAPPING.items():
        value = os.getenv(env_name)
        if value is None:
            continue
        values[field] = value
    return values


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    """Load and validate application settings."""

    values = _load_environment_values()
    try:
        return Settings(**values)
    except ValidationError as exc:  # pragma: no cover - defensive branch
        raise RuntimeError("Invalid MCP configuration") from exc


__all__ = ["Settings", "get_settings"]
