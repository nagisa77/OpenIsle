-- Add comment count and last reply time fields to posts table for performance optimization
ALTER TABLE posts ADD COLUMN comment_count BIGINT NOT NULL DEFAULT 0;
ALTER TABLE posts ADD COLUMN last_reply_at DATETIME(6) NULL;

-- Add index on last_reply_at for sorting by latest reply
CREATE INDEX idx_posts_last_reply_at ON posts(last_reply_at);

-- Initialize comment_count and last_reply_at with existing data
UPDATE posts p SET 
    comment_count = (
        SELECT COUNT(*) 
        FROM comments c 
        WHERE c.post_id = p.id AND c.deleted_at IS NULL
    ),
    last_reply_at = (
        SELECT MAX(c.created_at) 
        FROM comments c 
        WHERE c.post_id = p.id AND c.deleted_at IS NULL
    );
