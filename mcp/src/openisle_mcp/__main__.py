"""Entrypoint for running the MCP service with ``python -m``."""

from __future__ import annotations

import logging

import uvicorn

from .config import get_settings


def main() -> None:
    settings = get_settings()
    logging.basicConfig(level=logging.INFO)
    uvicorn.run(
        "openisle_mcp.server:create_app",
        host=settings.host,
        port=settings.port,
        factory=True,
    )


if __name__ == "__main__":  # pragma: no cover
    main()
