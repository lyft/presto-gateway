-- Schema inlined in presto in:
--    presto-resource-group-managers/src/main/java/io/prestosql/plugin
--      /resourcegropus/db/ResourceGroupsDao.java
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
--    KEY(name),
    FOREIGN KEY (parent) REFERENCES resource_groups (resource_group_id)
)

