"""Environment configuration for the MCP server."""

from __future__ import annotations

import os
from typing import Any

from pydantic import BaseModel, Field, ValidationError, field_validator


class Settings(BaseModel):
    """Runtime configuration sourced from environment variables."""

    api_base_url: str = Field(
        default="http://springboot:8080",
        description="Base URL of the OpenIsle backend REST API.",
    )
    request_timeout: float = Field(
        default=10.0,
        description="Timeout in seconds for outgoing HTTP requests.",
        ge=0.1,
    )
    default_limit: int = Field(
        default=20,
        description="Default maximum number of results returned by the search tool.",
    )
    snippet_length: int = Field(
        default=160,
        description="Maximum length for the normalised snippet field.",
        ge=40,
    )

    model_config = {
        "extra": "ignore",
        "validate_assignment": True,
    }

    @field_validator("api_base_url", mode="before")
    @classmethod
    def _strip_trailing_slash(cls, value: Any) -> Any:
        if isinstance(value, str):
            value = value.strip()
            if value.endswith("/"):
                return value.rstrip("/")
        return value

    @field_validator("default_limit", mode="before")
    @classmethod
    def _parse_default_limit(cls, value: Any) -> Any:
        if isinstance(value, str) and value.strip():
            try:
                return int(value)
            except ValueError as exc:  # pragma: no cover - defensive
                raise ValueError("default_limit must be an integer") from exc
        return value

    @field_validator("snippet_length", mode="before")
    @classmethod
    def _parse_snippet_length(cls, value: Any) -> Any:
        if isinstance(value, str) and value.strip():
            try:
                return int(value)
            except ValueError as exc:  # pragma: no cover - defensive
                raise ValueError("snippet_length must be an integer") from exc
        return value

    @field_validator("request_timeout", mode="before")
    @classmethod
    def _parse_timeout(cls, value: Any) -> Any:
        if isinstance(value, str) and value.strip():
            try:
                return float(value)
            except ValueError as exc:  # pragma: no cover - defensive
                raise ValueError("request_timeout must be a number") from exc
        return value

    @classmethod
    def from_env(cls) -> "Settings":
        """Build a settings object using environment variables."""

        data: dict[str, Any] = {}
        mapping = {
            "api_base_url": "OPENISLE_API_BASE_URL",
            "request_timeout": "OPENISLE_API_TIMEOUT",
            "default_limit": "OPENISLE_MCP_DEFAULT_LIMIT",
            "snippet_length": "OPENISLE_MCP_SNIPPET_LENGTH",
        }
        for field, env_key in mapping.items():
            value = os.getenv(env_key)
            if value is not None and value != "":
                data[field] = value
        try:
            return cls.model_validate(data)
        except ValidationError as exc:  # pragma: no cover - validation errors surface early
            raise ValueError(
                "Invalid MCP settings derived from environment variables"
            ) from exc

    def sanitized_base_url(self) -> str:
        """Return the API base URL without trailing slashes."""

        return self.api_base_url.rstrip("/")
