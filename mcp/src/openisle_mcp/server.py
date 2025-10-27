"""Entry point for running the OpenIsle MCP server."""

from __future__ import annotations

from contextlib import asynccontextmanager
from typing import Annotated

import httpx
from mcp.server.fastmcp import Context, FastMCP
from pydantic import ValidationError
from pydantic import Field as PydanticField

from .config import get_settings
from .schemas import (
    CommentData,
    CommentReplyResult,
    PostDetail,
    PostSummary,
    RecentPostsResponse,
    SearchResponse,
    SearchResultItem,
)
from .search_client import SearchClient

settings = get_settings()
search_client = SearchClient(
    str(settings.backend_base_url), timeout=settings.request_timeout
)


@asynccontextmanager
async def lifespan(_: FastMCP):
    """Lifecycle hook that disposes shared resources when the server stops."""

    try:
        yield
    finally:
        await search_client.aclose()


app = FastMCP(
    name="openisle-mcp",
    instructions=(
        "Use this server to search OpenIsle content, reply to comments with an authentication "
        "token, retrieve details for a specific post, and list posts created within a recent time "
        "window."
    ),
    host=settings.host,
    port=settings.port,
    lifespan=lifespan,
)


@app.tool(
    name="search",
    description="Perform a global search across OpenIsle resources.",
    structured_output=True,
)
async def search(
    keyword: Annotated[str, PydanticField(description="Keyword to search for.")],
    ctx: Context | None = None,
) -> SearchResponse:
    """Call the OpenIsle global search endpoint and return structured results."""

    sanitized = keyword.strip()
    if not sanitized:
        raise ValueError("Keyword must not be empty.")

    try:
        raw_results = await search_client.global_search(sanitized)
    except httpx.HTTPStatusError as exc:  # pragma: no cover - network errors
        message = (
            "OpenIsle backend returned HTTP "
            f"{exc.response.status_code} while searching for '{sanitized}'."
        )
        if ctx is not None:
            await ctx.error(message)
        raise ValueError(message) from exc
    except httpx.RequestError as exc:  # pragma: no cover - network errors
        message = f"Unable to reach OpenIsle backend search service: {exc}."
        if ctx is not None:
            await ctx.error(message)
        raise ValueError(message) from exc

    try:
        results = [SearchResultItem.model_validate(entry) for entry in raw_results]
    except ValidationError as exc:
        message = "Received malformed data from the OpenIsle backend search endpoint."
        if ctx is not None:
            await ctx.error(message)
        raise ValueError(message) from exc

    if ctx is not None:
        await ctx.info(f"Search keyword '{sanitized}' returned {len(results)} results.")

    return SearchResponse(keyword=sanitized, total=len(results), results=results)


@app.tool(
    name="reply_to_comment",
    description="Reply to an existing comment using an authentication token.",
    structured_output=True,
)
async def reply_to_comment(
    comment_id: Annotated[
        int,
        PydanticField(ge=1, description="Identifier of the comment being replied to."),
    ],
    token: Annotated[str, PydanticField(description="JWT bearer token for the user performing the reply.")],
    content: Annotated[
        str,
        PydanticField(description="Markdown content of the reply."),
    ],
    captcha: Annotated[
        str | None,
        PydanticField(
            default=None,
            description="Optional captcha solution if the backend requires it.",
        ),
    ] = None,
    ctx: Context | None = None,
) -> CommentReplyResult:
    """Create a reply for a comment and return the backend payload."""

    sanitized_content = content.strip()
    if not sanitized_content:
        raise ValueError("Reply content must not be empty.")

    sanitized_token = token.strip()
    if not sanitized_token:
        raise ValueError("Authentication token must not be empty.")

    sanitized_captcha = captcha.strip() if isinstance(captcha, str) else None

    try:
        raw_comment = await search_client.reply_to_comment(
            comment_id,
            sanitized_token,
            sanitized_content,
            sanitized_captcha,
        )
    except httpx.HTTPStatusError as exc:  # pragma: no cover - network errors
        status_code = exc.response.status_code
        if status_code == 401:
            message = (
                "Authentication failed while replying to comment "
                f"{comment_id}. Please verify the token."
            )
        elif status_code == 403:
            message = (
                "The provided token is not authorized to reply to comment "
                f"{comment_id}."
            )
        else:
            message = (
                "OpenIsle backend returned HTTP "
                f"{status_code} while replying to comment {comment_id}."
            )
        if ctx is not None:
            await ctx.error(message)
        raise ValueError(message) from exc
    except httpx.RequestError as exc:  # pragma: no cover - network errors
        message = (
            "Unable to reach OpenIsle backend comment service: "
            f"{exc}."
        )
        if ctx is not None:
            await ctx.error(message)
        raise ValueError(message) from exc

    try:
        comment = CommentData.model_validate(raw_comment)
    except ValidationError as exc:
        message = "Received malformed data from the reply comment endpoint."
        if ctx is not None:
            await ctx.error(message)
        raise ValueError(message) from exc

    if ctx is not None:
        await ctx.info(
            "Reply created successfully for comment "
            f"{comment_id}."
        )

    return CommentReplyResult(comment=comment)


@app.tool(
    name="recent_posts",
    description="Retrieve posts created in the last N minutes.",
    structured_output=True,
)
async def recent_posts(
    minutes: Annotated[
        int,
        PydanticField(gt=0, le=1440, description="Time window in minutes to search for new posts."),
    ],
    ctx: Context | None = None,
) -> RecentPostsResponse:
    """Fetch recent posts from the backend and return structured data."""

    try:
        raw_posts = await search_client.recent_posts(minutes)
    except httpx.HTTPStatusError as exc:  # pragma: no cover - network errors
        message = (
            "OpenIsle backend returned HTTP "
            f"{exc.response.status_code} while fetching recent posts for the last {minutes} minutes."
        )
        if ctx is not None:
            await ctx.error(message)
        raise ValueError(message) from exc
    except httpx.RequestError as exc:  # pragma: no cover - network errors
        message = f"Unable to reach OpenIsle backend recent posts service: {exc}."
        if ctx is not None:
            await ctx.error(message)
        raise ValueError(message) from exc

    try:
        posts = [PostSummary.model_validate(entry) for entry in raw_posts]
    except ValidationError as exc:
        message = "Received malformed data from the recent posts endpoint."
        if ctx is not None:
            await ctx.error(message)
        raise ValueError(message) from exc

    if ctx is not None:
        await ctx.info(
            f"Found {len(posts)} posts created within the last {minutes} minutes."
        )

    return RecentPostsResponse(minutes=minutes, total=len(posts), posts=posts)


@app.tool(
    name="get_post",
    description="Retrieve detailed information for a single post.",
    structured_output=True,
)
async def get_post(
    post_id: Annotated[
        int,
        PydanticField(ge=1, description="Identifier of the post to retrieve."),
    ],
    token: Annotated[
        str | None,
        PydanticField(
            default=None,
            description="Optional JWT bearer token to view the post as an authenticated user.",
        ),
    ] = None,
    ctx: Context | None = None,
) -> PostDetail:
    """Fetch post details from the backend and validate the response."""

    sanitized_token = token.strip() if isinstance(token, str) else None
    if sanitized_token == "":
        sanitized_token = None

    try:
        raw_post = await search_client.get_post(post_id, sanitized_token)
    except httpx.HTTPStatusError as exc:  # pragma: no cover - network errors
        status_code = exc.response.status_code
        if status_code == 404:
            message = f"Post {post_id} was not found."
        elif status_code == 401:
            message = "Authentication failed while retrieving the post."
        elif status_code == 403:
            message = "The provided token is not authorized to view this post."
        else:
            message = (
                "OpenIsle backend returned HTTP "
                f"{status_code} while retrieving post {post_id}."
            )
        if ctx is not None:
            await ctx.error(message)
        raise ValueError(message) from exc
    except httpx.RequestError as exc:  # pragma: no cover - network errors
        message = f"Unable to reach OpenIsle backend post service: {exc}."
        if ctx is not None:
            await ctx.error(message)
        raise ValueError(message) from exc

    try:
        post = PostDetail.model_validate(raw_post)
    except ValidationError as exc:
        message = "Received malformed data from the post detail endpoint."
        if ctx is not None:
            await ctx.error(message)
        raise ValueError(message) from exc

    if ctx is not None:
        await ctx.info(f"Retrieved post {post_id} successfully.")

    return post


def main() -> None:
    """Run the MCP server using the configured transport."""

    app.run(transport=settings.transport)


if __name__ == "__main__":  # pragma: no cover - manual execution
    main()

