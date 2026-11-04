-- Proposed production schema, separate from the in-memory Java bonus.
-- Replace datacenter1 with the actual data center name before provisioning.
CREATE KEYSPACE IF NOT EXISTS social_feed
WITH replication = {
    'class': 'NetworkTopologyStrategy',
    'datacenter1': 3
};

CREATE TABLE IF NOT EXISTS social_feed.posts_by_author (
    author_id text,
    bucket_month text,
    created_at timestamp,
    post_id uuid,
    content text,
    PRIMARY KEY ((author_id, bucket_month), created_at, post_id)
) WITH CLUSTERING ORDER BY (created_at DESC, post_id DESC);

CREATE TABLE IF NOT EXISTS social_feed.feed_by_user (
    user_id text,
    bucket_month text,
    created_at timestamp,
    post_id uuid,
    author_id text,
    content text,
    PRIMARY KEY ((user_id, bucket_month), created_at, post_id)
) WITH CLUSTERING ORDER BY (created_at DESC, post_id DESC);

-- Configure read and write consistency ONE in the production driver.
-- For a cqlsh session, use: CONSISTENCY ONE;
-- bucket_month is the UTC creation month in YYYY-MM format.
-- Example access pattern:
-- SELECT * FROM social_feed.feed_by_user
-- WHERE user_id = 'u01' AND bucket_month = '2026-11' LIMIT 50;
-- CDC capture must be enabled and configured separately for posts_by_author.
