CREATE TABLE uni_asr_cache (
  id BIGINT NOT NULL PRIMARY KEY,
  md5 varchar,
  path varchar,
  format varchar,
  text varchar,
  creator VARCHAR(64) DEFAULT '',
  create_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) DEFAULT '',
  update_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0,
  tenant_id BIGINT NOT NULL DEFAULT 0
);

create index uni_asr_cache_md5 on uni_asr_cache(md5);