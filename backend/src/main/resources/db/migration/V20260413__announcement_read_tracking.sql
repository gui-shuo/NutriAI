-- 公告已读记录表
CREATE TABLE IF NOT EXISTS announcement_reads (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  announcement_id BIGINT NOT NULL,
  read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_announcement (user_id, announcement_id),
  KEY idx_user_id (user_id),
  KEY idx_announcement_id (announcement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告已读记录';
