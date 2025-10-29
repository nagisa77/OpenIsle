"""Application configuration helpers for the OpenIsle MCP server."""

from __future__ import annotations

from functools import lru_cache
from typing import Literal

from pydantic import Field, SecretStr
from pydantic.networks import AnyHttpUrl
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Configuration for the MCP server."""

    backend_base_url: AnyHttpUrl = Field(
        "http://springboot:8080",
        description="Base URL for the OpenIsle backend service.",
    )
    host: str = Field(
        "0.0.0.0",
        description="Host interface to bind when running with HTTP transports.",
    )
    port: int = Field(
        8085,
        ge=1,
        le=65535,
        description="TCP port for HTTP transports.",
    )
    transport: Literal["stdio", "sse", "streamable-http"] = Field(
        "streamable-http",
        description="MCP transport to use when running the server.",
    )
    request_timeout: float = Field(
        10.0,
        gt=0,
        description="Timeout (seconds) for backend search requests.",
    )
    access_token: SecretStr | None = Field(
        default=None,
        description=(
            "Optional JWT bearer token used for authenticated backend calls. "
            "When set, tools that support authentication will use this token "
            "automatically unless an explicit token override is provided."
        ),
    )
    log_level: str = Field(
        "INFO",
        description=(
            "Logging level for the MCP server (e.g. DEBUG, INFO, WARNING)."
        ),
    )

    model_config = SettingsConfigDict(
        env_prefix="OPENISLE_MCP_",
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
    )


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    """Return cached application settings."""

    return Settings()
