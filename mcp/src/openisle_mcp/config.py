"""Configuration helpers for the OpenIsle MCP server."""

from __future__ import annotations

import os
from functools import lru_cache
from typing import Dict, Literal

from pydantic import AnyHttpUrl, BaseModel, Field, ValidationError

from .models import SearchScope

TransportType = Literal["stdio", "sse", "streamable-http"]


class Settings(BaseModel):
    """Runtime configuration for the MCP server."""

    backend_base_url: AnyHttpUrl = Field(
        default="http://localhost:8080",
        description="Base URL of the OpenIsle backend API.",
    )
    request_timeout_seconds: float = Field(
        default=10.0,
        gt=0,
        description="HTTP timeout when talking to the backend APIs.",
    )
    transport: TransportType = Field(
        default="streamable-http",
        description="Transport mode for the MCP server.",
    )
    host: str = Field(default="127.0.0.1", description="Hostname/interface used by the MCP HTTP server.")
    port: int = Field(default=8000, ge=0, description="Port used by the MCP HTTP server.")
    search_paths: Dict[str, str] = Field(
        default_factory=lambda: {
            SearchScope.GLOBAL.value: "/api/search/global",
            SearchScope.USERS.value: "/api/search/users",
            SearchScope.POSTS.value: "/api/search/posts",
            SearchScope.POSTS_TITLE.value: "/api/search/posts/title",
            SearchScope.POSTS_CONTENT.value: "/api/search/posts/content",
        },
        description="Mapping between search scopes and backend API paths.",
    )

    def get_search_path(self, scope: SearchScope) -> str:
        """Return the backend path associated with a given search scope."""

        try:
            return self.search_paths[scope.value]
        except KeyError as exc:  # pragma: no cover - defensive guard
            raise ValueError(f"Unsupported search scope: {scope}") from exc


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    """Load settings from environment variables with caching."""

    raw_settings: Dict[str, object] = {}

    backend_url = os.getenv("OPENISLE_API_BASE_URL")
    if backend_url:
        raw_settings["backend_base_url"] = backend_url

    timeout = os.getenv("OPENISLE_MCP_TIMEOUT_SECONDS")
    if timeout:
        raw_settings["request_timeout_seconds"] = float(timeout)

    transport = os.getenv("OPENISLE_MCP_TRANSPORT")
    if transport:
        raw_settings["transport"] = transport

    host = os.getenv("OPENISLE_MCP_HOST")
    if host:
        raw_settings["host"] = host

    port = os.getenv("OPENISLE_MCP_PORT")
    if port:
        raw_settings["port"] = int(port)

    try:
        return Settings(**raw_settings)
    except (ValidationError, ValueError) as exc:  # pragma: no cover - configuration errors should surface clearly
        raise RuntimeError(f"Invalid MCP configuration: {exc}") from exc
