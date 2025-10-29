"""Entry point for running the OpenIsle MCP server."""

from __future__ import annotations

import logging
from contextlib import asynccontextmanager
from typing import Annotated

import httpx
from mcp.server.fastmcp import Context, FastMCP
from pydantic import ValidationError
from pydantic import Field as PydanticField

from .config import get_settings
from .schemas import (
    CommentCreateResult,
    CommentData,
    CommentReplyResult,
    NotificationData,
    NotificationCleanupResult,
    UnreadNotificationsResponse,
    PostDetail,
    PostCreateResult,
    PostSummary,
    RecentPostsResponse,
    SearchResponse,
    SearchResultItem,
)
from .search_client import SearchClient

settings = get_settings()
if not logging.getLogger().handlers:
    logging.basicConfig(
        level=getattr(logging, settings.log_level.upper(), logging.INFO),
        format="%(asctime)s | %(levelname)s | %(name)s | %(message)s",
    )
else:
    logging.getLogger().setLevel(
        getattr(logging, settings.log_level.upper(), logging.INFO)
    )

logger = logging.getLogger(__name__)

search_client = SearchClient(
    str(settings.backend_base_url),
    timeout=settings.request_timeout,
    access_token=(
        settings.access_token.get_secret_value()
        if settings.access_token is not None
        else None
    ),
)


def _extract_authorization_token(ctx: Context | None) -> str | None:
    """Return the Bearer token from the incoming MCP request headers."""

    if ctx is None:
        return None

    try:
        request_context = ctx.request_context
    except ValueError:
        return None

    request = getattr(request_context, "request", None)
    if request is None:
        return None

    headers = getattr(request, "headers", None)
    if headers is None:
        return None

    authorization = headers.get("authorization")
    if not authorization:
        return None

    scheme, _, token = authorization.partition(" ")
    if scheme.lower() != "bearer":
        return None

    stripped = token.strip()
    return stripped or None


@asynccontextmanager
async def lifespan(_: FastMCP):
    """Lifecycle hook that disposes shared resources when the server stops."""

    try:
        logger.debug("OpenIsle MCP server lifespan started.")
        yield
    finally:
        logger.debug("Disposing shared SearchClient instance.")
        await search_client.aclose()


app = FastMCP(
    name="openisle-mcp",
    instructions=(
        "Use this server to search OpenIsle content, create new posts, reply to posts and "
        "comments using the Authorization header or configured access token, retrieve details "
        "for a specific post, list posts created within a recent time window, and review "
        "unread notification messages."
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
        logger.info("Received search request for keyword='%s'", sanitized)
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
    logger.debug(
        "Validated %d search results for keyword='%s'",
        len(results),
        sanitized,
    )

    return SearchResponse(keyword=sanitized, total=len(results), results=results)


@app.tool(
    name="reply_to_post",
    description=(
        "Create a comment on a post using the request Authorization header or the configured "
        "access token."
    ),
    structured_output=True,
)
async def reply_to_post(
    post_id: Annotated[
        int,
        PydanticField(ge=1, description="Identifier of the post being replied to."),
    ],
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
) -> CommentCreateResult:
    """Create a comment on a post and return the backend payload."""

    sanitized_content = content.strip()
    if not sanitized_content:
        raise ValueError("Reply content must not be empty.")

    sanitized_captcha = captcha.strip() if isinstance(captcha, str) else None

    request_token = _extract_authorization_token(ctx)

    try:
        logger.info(
            "Creating reply for post_id=%s (captcha=%s)",
            post_id,
            bool(sanitized_captcha),
        )
        raw_comment = await search_client.reply_to_post(
            post_id,
            token=request_token,
            content=sanitized_content,
            captcha=sanitized_captcha,
        )
    except httpx.HTTPStatusError as exc:  # pragma: no cover - network errors
        status_code = exc.response.status_code
        if status_code == 401:
            message = (
                "Authentication failed while replying to post "
                f"{post_id}. Please verify the Authorization header or configured token."
            )
        elif status_code == 403:
            message = (
                "The provided Authorization token is not authorized to reply to post "
                f"{post_id}."
            )
        elif status_code == 404:
            message = f"Post {post_id} was not found."
        else:
            message = (
                "OpenIsle backend returned HTTP "
                f"{status_code} while replying to post {post_id}."
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
        message = "Received malformed data from the post comment endpoint."
        if ctx is not None:
            await ctx.error(message)
        raise ValueError(message) from exc

    if ctx is not None:
        await ctx.info(
            "Reply created successfully for post "
            f"{post_id}."
        )
    logger.debug(
        "Validated reply comment payload for post_id=%s (comment_id=%s)",
        post_id,
        comment.id,
    )

    return CommentCreateResult(comment=comment)


@app.tool(
    name="reply_to_comment",
    description=(
        "Reply to an existing comment using the request Authorization header or the configured "
        "access token."
    ),
    structured_output=True,
)
async def reply_to_comment(
    comment_id: Annotated[
        int,
        PydanticField(ge=1, description="Identifier of the comment being replied to."),
    ],
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

    sanitized_captcha = captcha.strip() if isinstance(captcha, str) else None

    request_token = _extract_authorization_token(ctx)

    try:
        logger.info(
            "Creating reply for comment_id=%s (captcha=%s)",
            comment_id,
            bool(sanitized_captcha),
        )
        raw_comment = await search_client.reply_to_comment(
            comment_id,
            token=request_token,
            content=sanitized_content,
            captcha=sanitized_captcha,
        )
    except httpx.HTTPStatusError as exc:  # pragma: no cover - network errors
        status_code = exc.response.status_code
        if status_code == 401:
            message = (
                "Authentication failed while replying to comment "
                f"{comment_id}. Please verify the Authorization header or configured token."
            )
        elif status_code == 403:
            message = (
                "The provided Authorization token is not authorized to reply to comment "
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
    logger.debug(
        "Validated reply payload for comment_id=%s (reply_id=%s)",
        comment_id,
        comment.id,
    )

    return CommentReplyResult(comment=comment)


@app.tool(
    name="create_post",
    description=(
        "Publish a new post using the request Authorization header or the configured access "
        "token."
    ),
    structured_output=True,
)
async def create_post(
    title: Annotated[
        str,
        PydanticField(description="Title of the post to be created."),
    ],
    content: Annotated[
        str,
        PydanticField(description="Markdown content of the post."),
    ],
    category_id: Annotated[
        int | None,
        PydanticField(
            default=None,
            ge=1,
            description="Optional category identifier for the post.",
        ),
    ] = None,
    tag_ids: Annotated[
        list[int] | None,
        PydanticField(
            default=None,
            min_length=1,
            description="Optional list of tag identifiers to assign to the post.",
        ),
    ] = None,
    post_type: Annotated[
        str | None,
        PydanticField(
            default=None,
            description="Optional post type value (e.g. LOTTERY, POLL).",
        ),
    ] = None,
    visible_scope: Annotated[
        str | None,
        PydanticField(
            default=None,
            description="Optional visibility scope for the post.",
        ),
    ] = None,
    prize_description: Annotated[
        str | None,
        PydanticField(
            default=None,
            description="Description of the prize for lottery posts.",
        ),
    ] = None,
    prize_icon: Annotated[
        str | None,
        PydanticField(
            default=None,
            description="Icon URL for the lottery prize.",
        ),
    ] = None,
    prize_count: Annotated[
        int | None,
        PydanticField(
            default=None,
            ge=1,
            description="Total number of prizes available for lottery posts.",
        ),
    ] = None,
    point_cost: Annotated[
        int | None,
        PydanticField(
            default=None,
            ge=0,
            description="Point cost required to participate in the post, when applicable.",
        ),
    ] = None,
    start_time: Annotated[
        str | None,
        PydanticField(
            default=None,
            description="ISO 8601 start time for lottery or poll posts.",
        ),
    ] = None,
    end_time: Annotated[
        str | None,
        PydanticField(
            default=None,
            description="ISO 8601 end time for lottery or poll posts.",
        ),
    ] = None,
    options: Annotated[
        list[str] | None,
        PydanticField(
            default=None,
            min_length=1,
            description="Poll options when creating a poll post.",
        ),
    ] = None,
    multiple: Annotated[
        bool | None,
        PydanticField(
            default=None,
            description="Whether the poll allows selecting multiple options.",
        ),
    ] = None,
    proposed_name: Annotated[
        str | None,
        PydanticField(
            default=None,
            description="Proposed category name for suggestion posts.",
        ),
    ] = None,
    proposal_description: Annotated[
        str | None,
        PydanticField(
            default=None,
            description="Supporting description for the proposed category.",
        ),
    ] = None,
    captcha: Annotated[
        str | None,
        PydanticField(
            default=None,
            description="Captcha solution if the backend requires one to create posts.",
        ),
    ] = None,
    ctx: Context | None = None,
) -> PostCreateResult:
    """Create a new post in OpenIsle and return the detailed backend payload."""

    sanitized_title = title.strip()
    if not sanitized_title:
        raise ValueError("Post title must not be empty.")

    sanitized_content = content.strip()
    if not sanitized_content:
        raise ValueError("Post content must not be empty.")

    sanitized_category_id: int | None = None
    if category_id is not None:
        if isinstance(category_id, bool):
            raise ValueError("Category identifier must be an integer, not a boolean.")
        try:
            sanitized_category_id = int(category_id)
        except (TypeError, ValueError) as exc:
            raise ValueError("Category identifier must be an integer.") from exc
        if sanitized_category_id <= 0:
            raise ValueError("Category identifier must be a positive integer.")
    if sanitized_category_id is None:
        raise ValueError("A category identifier is required to create a post.")

    sanitized_tag_ids: list[int] | None = None
    if tag_ids is not None:
        sanitized_tag_ids = []
        for value in tag_ids:
            if isinstance(value, bool):
                raise ValueError("Tag identifiers must be integers, not booleans.")
            try:
                converted = int(value)
            except (TypeError, ValueError) as exc:
                raise ValueError("Tag identifiers must be integers.") from exc
            if converted <= 0:
                raise ValueError("Tag identifiers must be positive integers.")
            sanitized_tag_ids.append(converted)
        if not sanitized_tag_ids:
            sanitized_tag_ids = None
    if not sanitized_tag_ids:
        raise ValueError("At least one tag identifier is required to create a post.")
    if len(sanitized_tag_ids) > 2:
        raise ValueError("At most two tag identifiers can be provided for a post.")

    sanitized_post_type = post_type.strip() if isinstance(post_type, str) else None
    if sanitized_post_type == "":
        sanitized_post_type = None

    sanitized_visible_scope = (
        visible_scope.strip() if isinstance(visible_scope, str) else None
    )
    if sanitized_visible_scope == "":
        sanitized_visible_scope = None

    sanitized_prize_description = (
        prize_description.strip() if isinstance(prize_description, str) else None
    )
    if sanitized_prize_description == "":
        sanitized_prize_description = None

    sanitized_prize_icon = prize_icon.strip() if isinstance(prize_icon, str) else None
    if sanitized_prize_icon == "":
        sanitized_prize_icon = None

    sanitized_prize_count: int | None = None
    if prize_count is not None:
        if isinstance(prize_count, bool):
            raise ValueError("Prize count must be an integer, not a boolean.")
        try:
            sanitized_prize_count = int(prize_count)
        except (TypeError, ValueError) as exc:
            raise ValueError("Prize count must be an integer.") from exc
        if sanitized_prize_count <= 0:
            raise ValueError("Prize count must be a positive integer.")

    sanitized_point_cost: int | None = None
    if point_cost is not None:
        if isinstance(point_cost, bool):
            raise ValueError("Point cost must be an integer, not a boolean.")
        try:
            sanitized_point_cost = int(point_cost)
        except (TypeError, ValueError) as exc:
            raise ValueError("Point cost must be an integer.") from exc
        if sanitized_point_cost < 0:
            raise ValueError("Point cost cannot be negative.")

    sanitized_start_time = start_time.strip() if isinstance(start_time, str) else None
    if sanitized_start_time == "":
        sanitized_start_time = None

    sanitized_end_time = end_time.strip() if isinstance(end_time, str) else None
    if sanitized_end_time == "":
        sanitized_end_time = None

    sanitized_options: list[str] | None = None
    if options is not None:
        sanitized_options = []
        for option in options:
            if option is None:
                continue
            stripped_option = option.strip()
            if stripped_option:
                sanitized_options.append(stripped_option)
        if not sanitized_options:
            sanitized_options = None

    sanitized_multiple = bool(multiple) if isinstance(multiple, bool) else None

    sanitized_proposed_name = (
        proposed_name.strip() if isinstance(proposed_name, str) else None
    )
    if sanitized_proposed_name == "":
        sanitized_proposed_name = None

    sanitized_proposal_description = (
        proposal_description.strip() if isinstance(proposal_description, str) else None
    )
    if sanitized_proposal_description == "":
        sanitized_proposal_description = None

    sanitized_captcha = captcha.strip() if isinstance(captcha, str) else None
    if sanitized_captcha == "":
        sanitized_captcha = None

    payload: dict[str, object] = {
        "title": sanitized_title,
        "content": sanitized_content,
    }
    if sanitized_category_id is not None:
        payload["categoryId"] = sanitized_category_id
    if sanitized_tag_ids is not None:
        payload["tagIds"] = sanitized_tag_ids
    if sanitized_post_type is not None:
        payload["type"] = sanitized_post_type
    if sanitized_visible_scope is not None:
        payload["postVisibleScopeType"] = sanitized_visible_scope
    if sanitized_prize_description is not None:
        payload["prizeDescription"] = sanitized_prize_description
    if sanitized_prize_icon is not None:
        payload["prizeIcon"] = sanitized_prize_icon
    if sanitized_prize_count is not None:
        payload["prizeCount"] = sanitized_prize_count
    if sanitized_point_cost is not None:
        payload["pointCost"] = sanitized_point_cost
    if sanitized_start_time is not None:
        payload["startTime"] = sanitized_start_time
    if sanitized_end_time is not None:
        payload["endTime"] = sanitized_end_time
    if sanitized_options is not None:
        payload["options"] = sanitized_options
    if sanitized_multiple is not None:
        payload["multiple"] = sanitized_multiple
    if sanitized_proposed_name is not None:
        payload["proposedName"] = sanitized_proposed_name
    if sanitized_proposal_description is not None:
        payload["proposalDescription"] = sanitized_proposal_description
    if sanitized_captcha is not None:
        payload["captcha"] = sanitized_captcha

    try:
        logger.info("Creating post with title='%s'", sanitized_title)
        raw_post = await search_client.create_post(payload, token=_extract_authorization_token(ctx))
    except httpx.HTTPStatusError as exc:  # pragma: no cover - network errors
        status_code = exc.response.status_code
        if status_code == 400:
            message = (
                "Post creation failed due to invalid input or captcha verification errors."
            )
        elif status_code == 401:
            message = (
                "Authentication failed while creating the post. Please verify the "
                "Authorization header or configured token."
            )
        elif status_code == 403:
            message = "The provided Authorization token is not authorized to create posts."
        else:
            message = (
                "OpenIsle backend returned HTTP "
                f"{status_code} while creating the post."
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
        message = "Received malformed data from the post creation endpoint."
        if ctx is not None:
            await ctx.error(message)
        raise ValueError(message) from exc

    if ctx is not None:
        await ctx.info(f"Post '{post.title}' created successfully.")
    logger.debug(
        "Validated created post payload with id=%s and title='%s'",
        post.id,
        post.title,
    )

    return PostCreateResult(post=post)


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
        logger.info("Fetching recent posts for last %s minutes", minutes)
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
    logger.debug(
        "Validated %d recent posts for window=%s minutes",
        len(posts),
        minutes,
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
    ctx: Context | None = None,
) -> PostDetail:
    """Fetch post details from the backend and validate the response."""

    try:
        logger.info("Fetching post details for post_id=%s", post_id)
        raw_post = await search_client.get_post(
            post_id, _extract_authorization_token(ctx)
        )
    except httpx.HTTPStatusError as exc:  # pragma: no cover - network errors
        status_code = exc.response.status_code
        if status_code == 404:
            message = f"Post {post_id} was not found."
        elif status_code == 401:
            message = "Authentication failed while retrieving the post."
        elif status_code == 403:
            message = "The provided Authorization token is not authorized to view this post."
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
    logger.debug(
        "Validated post payload for post_id=%s with %d comments",
        post_id,
        len(post.comments),
    )

    return post


@app.tool(
    name="list_unread_messages",
    description="List unread notification messages for the authenticated user.",
    structured_output=True,
)
async def list_unread_messages(
    page: Annotated[
        int,
        PydanticField(
            default=0,
            ge=0,
            description="Page number of unread notifications to retrieve.",
        ),
    ] = 0,
    size: Annotated[
        int,
        PydanticField(
            default=30,
            ge=1,
            le=100,
            description="Number of unread notifications to include per page.",
        ),
    ] = 30,
    ctx: Context | None = None,
) -> UnreadNotificationsResponse:
    """Retrieve unread notifications and return structured data."""

    try:
        logger.info(
            "Fetching unread notifications (page=%s, size=%s)",
            page,
            size,
        )
        raw_notifications = await search_client.list_unread_notifications(
            page=page,
            size=size,
            token=_extract_authorization_token(ctx),
        )
    except httpx.HTTPStatusError as exc:  # pragma: no cover - network errors
        message = (
            "OpenIsle backend returned HTTP "
            f"{exc.response.status_code} while fetching unread notifications."
        )
        if ctx is not None:
            await ctx.error(message)
        raise ValueError(message) from exc
    except httpx.RequestError as exc:  # pragma: no cover - network errors
        message = f"Unable to reach OpenIsle backend notification service: {exc}."
        if ctx is not None:
            await ctx.error(message)
        raise ValueError(message) from exc

    try:
        notifications = [
            NotificationData.model_validate(entry) for entry in raw_notifications
        ]
    except ValidationError as exc:
        message = "Received malformed data from the unread notifications endpoint."
        if ctx is not None:
            await ctx.error(message)
        raise ValueError(message) from exc

    total = len(notifications)
    if ctx is not None:
        await ctx.info(
            f"Retrieved {total} unread notifications (page {page}, size {size})."
        )
    logger.debug(
        "Validated %d unread notifications for page=%s size=%s",
        total,
        page,
        size,
    )

    return UnreadNotificationsResponse(
        page=page,
        size=size,
        total=total,
        notifications=notifications,
    )


@app.tool(
    name="mark_notifications_read",
    description="Mark specific notification messages as read to remove them from the unread list.",
    structured_output=True,
)
async def mark_notifications_read(
    ids: Annotated[
        list[int],
        PydanticField(
            min_length=1,
            description="Notification identifiers that should be marked as read.",
        ),
    ],
    ctx: Context | None = None,
) -> NotificationCleanupResult:
    """Mark the supplied notifications as read and report the processed identifiers."""

    try:
        logger.info(
            "Marking %d notifications as read",  # pragma: no branch - logging
            len(ids),
        )
        await search_client.mark_notifications_read(
            ids, token=_extract_authorization_token(ctx)
        )
    except httpx.HTTPStatusError as exc:  # pragma: no cover - network errors
        message = (
            "OpenIsle backend returned HTTP "
            f"{exc.response.status_code} while marking notifications as read."
        )
        if ctx is not None:
            await ctx.error(message)
        raise ValueError(message) from exc
    except httpx.RequestError as exc:  # pragma: no cover - network errors
        message = f"Unable to reach OpenIsle backend notification service: {exc}."
        if ctx is not None:
            await ctx.error(message)
        raise ValueError(message) from exc

    processed_ids: list[int] = []
    for value in ids:
        if isinstance(value, bool):
            raise ValueError("Notification identifiers must be integers, not booleans.")
        converted = int(value)
        if converted <= 0:
            raise ValueError("Notification identifiers must be positive integers.")
        processed_ids.append(converted)
    if ctx is not None:
        await ctx.info(
            f"Marked {len(processed_ids)} notifications as read.",
        )
    logger.debug(
        "Successfully marked notifications as read: ids=%s",
        processed_ids,
    )

    return NotificationCleanupResult(
        processed_ids=processed_ids,
        total_marked=len(processed_ids),
    )


def main() -> None:
    """Run the MCP server using the configured transport."""

    app.run(transport=settings.transport)


if __name__ == "__main__":  # pragma: no cover - manual execution
    main()

