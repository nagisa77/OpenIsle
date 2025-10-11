-- Create table for category proposal posts (subclass of poll_posts)
CREATE TABLE IF NOT EXISTS category_proposal_posts (
    post_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    proposed_name VARCHAR(255) NOT NULL,
    proposed_slug VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    approve_threshold INT NOT NULL DEFAULT 60,
    quorum INT NOT NULL DEFAULT 10,
    start_at DATETIME(6) NULL,
    result_snapshot LONGTEXT NULL,
    reject_reason VARCHAR(255),
    PRIMARY KEY (post_id),
    CONSTRAINT fk_category_proposal_posts_parent
        FOREIGN KEY (post_id) REFERENCES poll_posts (post_id)
);

CREATE INDEX IF NOT EXISTS idx_category_proposal_posts_status
    ON category_proposal_posts (status);

CREATE UNIQUE INDEX IF NOT EXISTS idx_category_proposal_posts_slug
    ON category_proposal_posts (proposed_slug);



