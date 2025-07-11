package com.openisle.model;

/**
 * Types of user notifications.
 */
public enum NotificationType {
    /** Someone viewed your post */
    POST_VIEWED,
    /** Someone replied to your post or comment */
    COMMENT_REPLY,
    /** Someone reacted to your post or comment */
    REACTION,
    /** A new post requires admin review */
    POST_REVIEW,
    /** Your post under review was approved or rejected */
    POST_REVIEWED,
    /** A subscribed post received a new comment */
    POST_UPDATED,
    /** Someone subscribed to your post */
    POST_SUBSCRIBED,
    /** Someone unsubscribed from your post */
    POST_UNSUBSCRIBED,
    /** Someone you follow published a new post */
    FOLLOWED_POST,
    /** Someone started following you */
    USER_FOLLOWED,
    /** Someone unfollowed you */
    USER_UNFOLLOWED,
    /** A user you subscribe to created a post or comment */
    USER_ACTIVITY
}
