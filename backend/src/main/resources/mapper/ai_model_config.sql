-- AI 模型配置表
CREATE TABLE IF NOT EXISTS `ai_model_config` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID，NULL表示全局配置',
  `model_type` VARCHAR(20) NOT NULL DEFAULT 'deepseek' COMMENT '模型类型：deepseek / glm / minimax',
  `api_url` VARCHAR(512) DEFAULT NULL COMMENT 'API地址',
  `api_key` VARCHAR(256) DEFAULT NULL COMMENT 'API Key（建议加密存储）',
  `model_name` VARCHAR(64) DEFAULT NULL COMMENT '模型名称，如 deepseek-chat / glm-4-flash / abab6.5s-chat',
  `system_prompt` TEXT DEFAULT NULL COMMENT '自定义系统提示词',
  `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  `temperature` FLOAT DEFAULT 0.7 COMMENT '温度参数',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型配置表';

-- SOP AI 审核记录表
CREATE TABLE IF NOT EXISTS `sop_ai_review` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `sop_id` BIGINT NOT NULL COMMENT 'SOP ID',
  `sop_version` INT DEFAULT NULL COMMENT 'SOP 版本号',
  `review_mode` VARCHAR(20) DEFAULT 'review' COMMENT '审核模式：create / execute / review',
  `raw_response` TEXT DEFAULT NULL COMMENT 'AI 返回的原始 JSON',
  `verdict` VARCHAR(20) DEFAULT 'warning' COMMENT '审核结论：pass / warning / reject',
  `issues` TEXT DEFAULT NULL COMMENT '问题列表（JSON数组）',
  `suggestions` TEXT DEFAULT NULL COMMENT '改进建议（JSON数组）',
  `cost_ms` BIGINT DEFAULT NULL COMMENT '审核耗时（毫秒）',
  `model_type` VARCHAR(20) DEFAULT NULL COMMENT '使用的模型类型',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_sop_id` (`sop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SOP AI审核记录表';
