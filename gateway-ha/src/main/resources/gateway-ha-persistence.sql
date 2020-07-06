CREATE TABLE IF NOT EXISTS gateway_backend (
name VARCHAR(256) PRIMARY KEY,
routing_group VARCHAR (256),
backend_url VARCHAR (256),
external_url VARCHAR (256),
active BOOLEAN
);

CREATE TABLE IF NOT EXISTS query_history (
query_id VARCHAR(256) PRIMARY KEY,
query_text VARCHAR (256),
created bigint,
backend_url VARCHAR (256),
user_name VARCHAR(256),
source VARCHAR(256)
);
CREATE INDEX query_history_created_idx ON query_history(created);

CREATE TABLE IF NOT EXISTS resource_groups (
    resource_group_id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(250) NOT NULL UNIQUE,

    -- OPTIONAL POLICY CONTROLS
    parent BIGINT NULL,
    jmx_export BOOLEAN NULL,
    scheduling_policy VARCHAR(128) NULL,
    scheduling_weight INT NULL,

    -- REQUIRED QUOTAS
    soft_memory_limit VARCHAR(128) NOT NULL,
    max_queued INT NOT NULL,
    hard_concurrency_limit INT NOT NULL,

    -- OPTIONAL QUOTAS
    soft_concurrency_limit INT NULL,
    soft_cpu_limit VARCHAR(128) NULL,
    hard_cpu_limit VARCHAR(128) NULL,
    environment VARCHAR(128) NULL,

    PRIMARY KEY(resource_group_id),
    FOREIGN KEY (parent) REFERENCES resource_groups (resource_group_id)
);

CREATE TABLE IF NOT EXISTS selectors (
    resource_group_id BIGINT NOT NULL,
    priority BIGINT NOT NULL,

    -- Regex fields -- these will be used as a regular expression pattern to
    --                 match against the field of the same name on queries
    user_regex VARCHAR(512),
    source_regex VARCHAR(512),

    -- Selector fields -- these must match exactly.
    query_type VARCHAR(512),
    client_tags VARCHAR(512),
    selector_resource_estimate VARCHAR(1024),

    FOREIGN KEY (resource_group_id) REFERENCES resource_groups(resource_group_id)
);

CREATE TABLE IF NOT EXISTS resource_groups_global_properties (
    name VARCHAR(128) NOT NULL PRIMARY KEY,
    value VARCHAR(512) NULL,
    CHECK (name in ('cpu_quota_period'))
);

CREATE TABLE IF NOT EXISTS exact_match_source_selectors (
    resource_group_id VARCHAR(256) NOT NULL,  -- WTF varchar?!
    update_time DATETIME NOT NULL,

    -- Selector fields which must exactly match a query
    source VARCHAR(512) NOT NULL,
    environment VARCHAR(128),
    query_type VARCHAR(128), -- (reduced from 512)

    PRIMARY KEY (environment, source, query_type),
    UNIQUE (source, environment, query_type, resource_group_id)
);