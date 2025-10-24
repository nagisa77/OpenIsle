"""OpenIsle MCP server package."""

from importlib import metadata

try:
    __version__ = metadata.version("openisle-mcp")
except metadata.PackageNotFoundError:  # pragma: no cover - best effort during dev
    __version__ = "0.0.0"

__all__ = ["__version__"]
