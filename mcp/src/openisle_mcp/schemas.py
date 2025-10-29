"""Pydantic models describing tool inputs and outputs."""

from __future__ import annotations

from datetime import datetime
from typing import Any, Optional

from pydantic import BaseModel, Field, ConfigDict, field_validator


class SearchResultItem(BaseModel):
    """A single search result entry."""

    type: str = Field(description="Entity type for the result (post, user, tag, etc.).")
    id: Optional[int] = Field(default=None, description="Identifier of the matched entity.")
    text: Optional[str] = Field(default=None, description="Primary text associated with the result.")
    sub_text: Optional[str] = Field(
        default=None,
        alias="subText",
        description="Secondary text, e.g. a username or excerpt.",
    )
    extra: Optional[str] = Field(default=None, description="Additional contextual information.")
    post_id: Optional[int] = Field(
        default=None,
        alias="postId",
        description="Associated post identifier when relevant.",
    )
    highlighted_text: Optional[str] = Field(
        default=None,
        alias="highlightedText",
        description="Highlighted snippet of the primary text if available.",
    )
    highlighted_sub_text: Optional[str] = Field(
        default=None,
        alias="highlightedSubText",
        description="Highlighted snippet of the secondary text if available.",
    )
    highlighted_extra: Optional[str] = Field(
        default=None,
        alias="highlightedExtra",
        description="Highlighted snippet of extra information if available.",
    )

    model_config = ConfigDict(populate_by_name=True)


class SearchResponse(BaseModel):
    """Structured response returned by the search tool."""

    keyword: str = Field(description="The keyword that was searched.")
    total: int = Field(description="Total number of matches returned by the backend.")
    results: list[SearchResultItem] = Field(
        default_factory=list,
        description="Ordered collection of search results.",
    )


class AuthorInfo(BaseModel):
    """Summary of a post or comment author."""

    id: Optional[int] = Field(default=None, description="Author identifier.")
    username: Optional[str] = Field(default=None, description="Author username.")
    avatar: Optional[str] = Field(default=None, description="URL of the author's avatar.")
    display_medal: Optional[str] = Field(
        default=None,
        alias="displayMedal",
        description="Medal displayed next to the author, when available.",
    )

    model_config = ConfigDict(populate_by_name=True, extra="allow")


class CategoryInfo(BaseModel):
    """Basic information about a post category."""

    id: Optional[int] = Field(default=None, description="Category identifier.")
    name: Optional[str] = Field(default=None, description="Category name.")
    description: Optional[str] = Field(
        default=None, description="Human friendly description of the category."
    )
    icon: Optional[str] = Field(default=None, description="Icon URL associated with the category.")
    small_icon: Optional[str] = Field(
        default=None,
        alias="smallIcon",
        description="Compact icon URL for the category.",
    )
    count: Optional[int] = Field(default=None, description="Number of posts within the category.")

    model_config = ConfigDict(populate_by_name=True, extra="allow")


class TagInfo(BaseModel):
    """Details for a tag assigned to a post."""

    id: Optional[int] = Field(default=None, description="Tag identifier.")
    name: Optional[str] = Field(default=None, description="Tag name.")
    description: Optional[str] = Field(default=None, description="Description of the tag.")
    icon: Optional[str] = Field(default=None, description="Icon URL for the tag.")
    small_icon: Optional[str] = Field(
        default=None,
        alias="smallIcon",
        description="Compact icon URL for the tag.",
    )
    created_at: Optional[datetime] = Field(
        default=None,
        alias="createdAt",
        description="When the tag was created.",
    )
    count: Optional[int] = Field(default=None, description="Number of posts using the tag.")

    model_config = ConfigDict(populate_by_name=True, extra="allow")


class ReactionInfo(BaseModel):
    """Representation of a reaction on a post or comment."""

    id: Optional[int] = Field(default=None, description="Reaction identifier.")
    type: Optional[str] = Field(default=None, description="Reaction type (emoji, like, etc.).")
    user: Optional[str] = Field(default=None, description="Username of the reacting user.")
    post_id: Optional[int] = Field(
        default=None,
        alias="postId",
        description="Related post identifier when applicable.",
    )
    comment_id: Optional[int] = Field(
        default=None,
        alias="commentId",
        description="Related comment identifier when applicable.",
    )
    message_id: Optional[int] = Field(
        default=None,
        alias="messageId",
        description="Related message identifier when applicable.",
    )
    reward: Optional[int] = Field(default=None, description="Reward granted for the reaction, if any.")

    model_config = ConfigDict(populate_by_name=True, extra="allow")


class CommentData(BaseModel):
    """Comment information returned by the backend."""

    id: Optional[int] = Field(default=None, description="Comment identifier.")
    content: Optional[str] = Field(default=None, description="Markdown content of the comment.")
    created_at: Optional[datetime] = Field(
        default=None,
        alias="createdAt",
        description="Timestamp when the comment was created.",
    )
    pinned_at: Optional[datetime] = Field(
        default=None,
        alias="pinnedAt",
        description="Timestamp when the comment was pinned, if applicable.",
    )
    author: Optional[AuthorInfo] = Field(default=None, description="Author of the comment.")
    replies: list["CommentData"] = Field(
        default_factory=list,
        description="Nested replies associated with the comment.",
    )
    reactions: list[ReactionInfo] = Field(
        default_factory=list,
        description="Reactions applied to the comment.",
    )
    reward: Optional[int] = Field(default=None, description="Reward gained by posting the comment.")
    point_reward: Optional[int] = Field(
        default=None,
        alias="pointReward",
        description="Points rewarded for the comment.",
    )

    model_config = ConfigDict(populate_by_name=True, extra="allow")

    @field_validator("replies", "reactions", mode="before")
    @classmethod
    def _ensure_comment_lists(cls, value: Any) -> list[Any]:
        """Convert ``None`` payloads to empty lists for comment collections."""

        if value is None:
            return []
        return value


class CommentReplyResult(BaseModel):
    """Structured response returned when replying to a comment."""

    comment: CommentData = Field(description="Reply comment returned by the backend.")


class CommentCreateResult(BaseModel):
    """Structured response returned when creating a comment on a post."""

    comment: CommentData = Field(description="Comment returned by the backend.")


class PostCreateResult(BaseModel):
    """Structured response returned when creating a new post."""

    post: PostDetail = Field(description="Detailed post payload returned by the backend.")


class PostSummary(BaseModel):
    """Summary information for a post."""

    id: Optional[int] = Field(default=None, description="Post identifier.")
    title: Optional[str] = Field(default=None, description="Title of the post.")
    content: Optional[str] = Field(default=None, description="Excerpt or content of the post.")
    created_at: Optional[datetime] = Field(
        default=None,
        alias="createdAt",
        description="When the post was created.",
    )
    author: Optional[AuthorInfo] = Field(default=None, description="Author who created the post.")
    category: Optional[CategoryInfo] = Field(default=None, description="Category of the post.")
    tags: list[TagInfo] = Field(default_factory=list, description="Tags assigned to the post.")
    views: Optional[int] = Field(default=None, description="Total view count for the post.")
    comment_count: Optional[int] = Field(
        default=None,
        alias="commentCount",
        description="Number of comments on the post.",
    )
    status: Optional[str] = Field(default=None, description="Workflow status of the post.")
    pinned_at: Optional[datetime] = Field(
        default=None,
        alias="pinnedAt",
        description="When the post was pinned, if ever.",
    )
    last_reply_at: Optional[datetime] = Field(
        default=None,
        alias="lastReplyAt",
        description="Timestamp of the most recent reply.",
    )
    reactions: list[ReactionInfo] = Field(
        default_factory=list,
        description="Reactions received by the post.",
    )
    participants: list[AuthorInfo] = Field(
        default_factory=list,
        description="Users participating in the discussion.",
    )
    subscribed: Optional[bool] = Field(
        default=None,
        description="Whether the current user is subscribed to the post.",
    )
    reward: Optional[int] = Field(default=None, description="Reward granted for the post.")
    point_reward: Optional[int] = Field(
        default=None,
        alias="pointReward",
        description="Points granted for the post.",
    )
    type: Optional[str] = Field(default=None, description="Type of the post.")
    lottery: Optional[dict[str, Any]] = Field(
        default=None, description="Lottery information for the post."
    )
    poll: Optional[dict[str, Any]] = Field(
        default=None, description="Poll information for the post."
    )
    rss_excluded: Optional[bool] = Field(
        default=None,
        alias="rssExcluded",
        description="Whether the post is excluded from RSS feeds.",
    )
    closed: Optional[bool] = Field(default=None, description="Whether the post is closed for replies.")
    visible_scope: Optional[str] = Field(
        default=None,
        alias="visibleScope",
        description="Visibility scope configuration for the post.",
    )

    model_config = ConfigDict(populate_by_name=True, extra="allow")

    @field_validator("tags", "reactions", "participants", mode="before")
    @classmethod
    def _ensure_post_lists(cls, value: Any) -> list[Any]:
        """Normalize ``None`` values returned by the backend to empty lists."""

        if value is None:
            return []
        return value


class RecentPostsResponse(BaseModel):
    """Structured response for the recent posts tool."""

    minutes: int = Field(description="Time window, in minutes, used for the query.")
    total: int = Field(description="Number of posts returned by the backend.")
    posts: list[PostSummary] = Field(
        default_factory=list,
        description="Posts created within the requested time window.",
    )


CommentData.model_rebuild()


class PostDetail(PostSummary):
    """Detailed information for a single post, including comments."""

    comments: list[CommentData] = Field(
        default_factory=list,
        description="Comments that belong to the post.",
    )

    model_config = ConfigDict(populate_by_name=True, extra="allow")

    @field_validator("comments", mode="before")
    @classmethod
    def _ensure_comments_list(cls, value: Any) -> list[Any]:
        """Treat ``None`` comments payloads as empty lists."""

        if value is None:
            return []
        return value


class NotificationData(BaseModel):
    """Unread notification payload returned by the backend."""

    id: Optional[int] = Field(default=None, description="Notification identifier.")
    type: Optional[str] = Field(default=None, description="Type of the notification.")
    post: Optional[PostSummary] = Field(
        default=None, description="Post associated with the notification if applicable."
    )
    comment: Optional[CommentData] = Field(
        default=None, description="Comment referenced by the notification when available."
    )
    parent_comment: Optional[CommentData] = Field(
        default=None,
        alias="parentComment",
        description="Parent comment for nested replies, when present.",
    )
    from_user: Optional[AuthorInfo] = Field(
        default=None,
        alias="fromUser",
        description="User who triggered the notification.",
    )
    reaction_type: Optional[str] = Field(
        default=None,
        alias="reactionType",
        description="Reaction type for reaction-based notifications.",
    )
    content: Optional[str] = Field(
        default=None, description="Additional content or message for the notification."
    )
    approved: Optional[bool] = Field(
        default=None, description="Approval status for moderation notifications."
    )
    read: Optional[bool] = Field(default=None, description="Whether the notification is read.")
    created_at: Optional[datetime] = Field(
        default=None,
        alias="createdAt",
        description="Timestamp when the notification was created.",
    )

    model_config = ConfigDict(populate_by_name=True, extra="allow")


class UnreadNotificationsResponse(BaseModel):
    """Structured response for unread notification queries."""

    page: int = Field(description="Requested page index for the unread notifications.")
    size: int = Field(description="Requested page size for the unread notifications.")
    total: int = Field(description="Number of unread notifications returned in this page.")
    notifications: list[NotificationData] = Field(
        default_factory=list,
        description="Unread notifications returned by the backend.",
    )


class NotificationCleanupResult(BaseModel):
    """Structured response returned after marking notifications as read."""

    processed_ids: list[int] = Field(
        default_factory=list,
        description="Identifiers that were marked as read in the backend.",
    )
    total_marked: int = Field(
        description="Total number of notifications successfully marked as read.",
    )
