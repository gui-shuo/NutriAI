-- ===========================================
-- AI健康饮食规划助手系统 - 数据库初始化脚本
-- 版本: V1.0
-- 创建时间: 2025-12-02
-- 数据库: MySQL 8.0
-- 字符集: utf8mb4
-- ===========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ===========================================
-- 1. 用户相关表
-- ===========================================

-- 1.1 用户基础表
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `role` ENUM('user', 'admin', 'super_admin') NOT NULL DEFAULT 'user' COMMENT '角色',
  `status` ENUM('active', 'inactive', 'banned') NOT NULL DEFAULT 'active' COMMENT '账号状态',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_role` (`role`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户基础表';

-- 1.2 用户档案表
DROP TABLE IF EXISTS `user_profiles`;
CREATE TABLE `user_profiles` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '档案ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `gender` ENUM('male', 'female', 'other') DEFAULT NULL COMMENT '性别',
  `birth_date` DATE DEFAULT NULL COMMENT '出生日期',
  `height` DECIMAL(5,2) DEFAULT NULL COMMENT '身高（cm）',
  `weight` DECIMAL(5,2) DEFAULT NULL COMMENT '体重（kg）',
  `bmi` DECIMAL(5,2) DEFAULT NULL COMMENT 'BMI指数',
  `health_goal` ENUM('lose_weight', 'gain_muscle', 'maintain', 'improve_health') DEFAULT 'maintain' COMMENT '健康目标',
  `activity_level` ENUM('sedentary', 'light', 'moderate', 'active', 'very_active') DEFAULT 'moderate' COMMENT '活动水平',
  `allergies` JSON DEFAULT NULL COMMENT '过敏源列表（JSON数组）',
  `dietary_restrictions` JSON DEFAULT NULL COMMENT '饮食限制（JSON数组）',
  `medical_conditions` TEXT DEFAULT NULL COMMENT '健康状况说明',
  `daily_calorie_target` INT DEFAULT NULL COMMENT '每日热量目标（kcal）',
  `daily_protein_target` INT DEFAULT NULL COMMENT '每日蛋白质目标（g）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_health_goal` (`health_goal`),
  KEY `idx_bmi` (`bmi`),
  CONSTRAINT `fk_profile_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户健康档案表';

-- ===========================================
-- 2. 会员相关表
-- ===========================================

-- 2.1 会员信息表
DROP TABLE IF EXISTS `memberships`;
CREATE TABLE `memberships` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会员ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `level` ENUM('free', 'silver', 'gold') NOT NULL DEFAULT 'free' COMMENT '会员等级',
  `growth_points` INT NOT NULL DEFAULT 0 COMMENT '成长值',
  `expire_date` DATE DEFAULT NULL COMMENT '会员到期日期',
  `ai_quota_used` INT NOT NULL DEFAULT 0 COMMENT '今日已使用AI次数',
  `ai_quota_total` INT NOT NULL DEFAULT 3 COMMENT '今日AI总配额',
  `last_quota_reset_date` DATE DEFAULT NULL COMMENT '配额重置日期',
  `invite_code` VARCHAR(20) DEFAULT NULL COMMENT '邀请码',
  `invited_by` BIGINT DEFAULT NULL COMMENT '邀请人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_invite_code` (`invite_code`),
  KEY `idx_level` (`level`),
  KEY `idx_growth_points` (`growth_points`),
  KEY `idx_invited_by` (`invited_by`),
  CONSTRAINT `fk_membership_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员信息表';

-- 2.2 成长值记录表
DROP TABLE IF EXISTS `growth_records`;
CREATE TABLE `growth_records` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `points` INT NOT NULL COMMENT '成长值变化（正数为增加，负数为减少）',
  `type` ENUM('login', 'ai_chat', 'invite_register', 'invite_active', 'admin_adjust') NOT NULL COMMENT '获取类型',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
  `related_id` BIGINT DEFAULT NULL COMMENT '关联ID（如邀请用户ID）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_growth_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成长值记录表';

-- ===========================================
-- 3. 饮食记录表
-- ===========================================

-- 3.1 饮食记录表
DROP TABLE IF EXISTS `diet_records`;
CREATE TABLE `diet_records` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `record_date` DATE NOT NULL COMMENT '记录日期',
  `meal_type` ENUM('breakfast', 'lunch', 'dinner', 'snack') NOT NULL COMMENT '餐次',
  `meal_time` TIME DEFAULT NULL COMMENT '用餐时间',
  `food_items` JSON NOT NULL COMMENT '食物列表（JSON数组）',
  `total_calories` INT DEFAULT NULL COMMENT '总热量（kcal）',
  `total_protein` DECIMAL(8,2) DEFAULT NULL COMMENT '总蛋白质（g）',
  `total_carbs` DECIMAL(8,2) DEFAULT NULL COMMENT '总碳水化合物（g）',
  `total_fat` DECIMAL(8,2) DEFAULT NULL COMMENT '总脂肪（g）',
  `images` JSON DEFAULT NULL COMMENT '照片URL列表（JSON数组）',
  `notes` TEXT DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`, `record_date`),
  KEY `idx_meal_type` (`meal_type`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_diet_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='饮食记录表';

-- ===========================================
-- 4. AI对话表
-- ===========================================

-- 4.1 聊天消息表
DROP TABLE IF EXISTS `chat_messages`;
CREATE TABLE `chat_messages` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `session_id` VARCHAR(50) NOT NULL COMMENT '会话ID',
  `role` ENUM('user', 'assistant', 'system') NOT NULL COMMENT '角色',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `tokens_used` INT DEFAULT NULL COMMENT '使用tokens数',
  `model` VARCHAR(50) DEFAULT NULL COMMENT '使用模型',
  `response_time` INT DEFAULT NULL COMMENT '响应时间（ms）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_session` (`user_id`, `session_id`),
  KEY `idx_role` (`role`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_chat_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天消息表';

-- 4.2 生成的饮食计划表
DROP TABLE IF EXISTS `diet_plans`;
CREATE TABLE `diet_plans` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `title` VARCHAR(100) NOT NULL COMMENT '计划标题',
  `description` TEXT DEFAULT NULL COMMENT '计划描述',
  `plan_type` ENUM('weight_loss', 'muscle_gain', 'balanced', 'custom') NOT NULL COMMENT '计划类型',
  `duration_days` INT NOT NULL COMMENT '计划天数',
  `daily_budget` DECIMAL(10,2) DEFAULT NULL COMMENT '每日预算（元）',
  `plan_content` JSON NOT NULL COMMENT '计划内容（JSON结构）',
  `is_favorite` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否收藏',
  `status` ENUM('active', 'completed', 'cancelled') NOT NULL DEFAULT 'active' COMMENT '计划状态',
  `start_date` DATE DEFAULT NULL COMMENT '开始日期',
  `end_date` DATE DEFAULT NULL COMMENT '结束日期',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_plan_type` (`plan_type`),
  KEY `idx_status` (`status`),
  KEY `idx_is_favorite` (`is_favorite`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_plan_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI生成饮食计划表';

-- ===========================================
-- 5. 营养数据表
-- ===========================================

-- 5.1 食物营养数据表
DROP TABLE IF EXISTS `nutrition_data`;
CREATE TABLE `nutrition_data` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '食物ID',
  `food_name` VARCHAR(100) NOT NULL COMMENT '食物名称',
  `food_name_en` VARCHAR(100) DEFAULT NULL COMMENT '英文名称',
  `category` VARCHAR(50) NOT NULL COMMENT '食物分类',
  `serving_size` VARCHAR(50) NOT NULL COMMENT '标准份量',
  `calories` DECIMAL(8,2) NOT NULL COMMENT '热量（kcal/100g）',
  `protein` DECIMAL(8,2) NOT NULL COMMENT '蛋白质（g/100g）',
  `carbs` DECIMAL(8,2) NOT NULL COMMENT '碳水化合物（g/100g）',
  `fat` DECIMAL(8,2) NOT NULL COMMENT '脂肪（g/100g）',
  `fiber` DECIMAL(8,2) DEFAULT NULL COMMENT '膳食纤维（g/100g）',
  `sodium` DECIMAL(8,2) DEFAULT NULL COMMENT '钠（mg/100g）',
  `vitamin_c` DECIMAL(8,2) DEFAULT NULL COMMENT '维生素C（mg/100g）',
  `calcium` DECIMAL(8,2) DEFAULT NULL COMMENT '钙（mg/100g）',
  `iron` DECIMAL(8,2) DEFAULT NULL COMMENT '铁（mg/100g）',
  `image_url` VARCHAR(255) DEFAULT NULL COMMENT '食物图片URL',
  `description` TEXT DEFAULT NULL COMMENT '食物描述',
  `common_allergens` JSON DEFAULT NULL COMMENT '常见过敏源（JSON数组）',
  `source` VARCHAR(50) DEFAULT 'manual' COMMENT '数据来源',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_food_name` (`food_name`),
  KEY `idx_category` (`category`),
  KEY `idx_calories` (`calories`),
  FULLTEXT KEY `ft_food_name` (`food_name`, `food_name_en`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食物营养数据表';

-- 5.2 食物分类表
DROP TABLE IF EXISTS `food_categories`;
CREATE TABLE `food_categories` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `name_en` VARCHAR(50) DEFAULT NULL COMMENT '英文名称',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父分类ID',
  `icon` VARCHAR(100) DEFAULT NULL COMMENT '分类图标',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食物分类表';

-- ===========================================
-- 6. 系统日志表
-- ===========================================

-- 6.1 操作日志表
DROP TABLE IF EXISTS `operation_logs`;
CREATE TABLE `operation_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `operation` VARCHAR(50) NOT NULL COMMENT '操作类型',
  `module` VARCHAR(50) NOT NULL COMMENT '模块名称',
  `method` VARCHAR(100) DEFAULT NULL COMMENT '方法名',
  `params` TEXT DEFAULT NULL COMMENT '请求参数',
  `result` TEXT DEFAULT NULL COMMENT '返回结果',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` VARCHAR(255) DEFAULT NULL COMMENT 'User Agent',
  `status` ENUM('success', 'failure') NOT NULL DEFAULT 'success' COMMENT '操作状态',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `execution_time` INT DEFAULT NULL COMMENT '执行时间（ms）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_operation` (`operation`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- 6.2 API调用日志表
DROP TABLE IF EXISTS `api_logs`;
CREATE TABLE `api_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `api_type` ENUM('tongyi', 'oss', 'image_recognition', 'other') NOT NULL COMMENT 'API类型',
  `api_name` VARCHAR(100) NOT NULL COMMENT 'API名称',
  `request_data` TEXT DEFAULT NULL COMMENT '请求数据',
  `response_data` TEXT DEFAULT NULL COMMENT '响应数据',
  `tokens_used` INT DEFAULT NULL COMMENT '使用tokens数',
  `cost` DECIMAL(10,4) DEFAULT NULL COMMENT '费用（元）',
  `status_code` INT DEFAULT NULL COMMENT 'HTTP状态码',
  `response_time` INT DEFAULT NULL COMMENT '响应时间（ms）',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_api_type` (`api_type`),
  KEY `idx_status_code` (`status_code`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='第三方API调用日志表';

-- ===========================================
-- 7. 收藏表
-- ===========================================

-- 7.1 收藏表
DROP TABLE IF EXISTS `favorites`;
CREATE TABLE `favorites` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `item_type` ENUM('diet_plan', 'recipe', 'food', 'chat_message') NOT NULL COMMENT '收藏类型',
  `item_id` BIGINT NOT NULL COMMENT '收藏项ID',
  `notes` TEXT DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_item` (`user_id`, `item_type`, `item_id`),
  KEY `idx_item_type` (`item_type`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏表';

-- ===========================================
-- 8. 系统配置表
-- ===========================================

-- 8.1 系统配置表
DROP TABLE IF EXISTS `system_configs`;
CREATE TABLE `system_configs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
  `config_value` TEXT NOT NULL COMMENT '配置值',
  `config_type` ENUM('string', 'number', 'boolean', 'json') NOT NULL DEFAULT 'string' COMMENT '配置类型',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '配置描述',
  `is_public` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否公开',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ===========================================
-- 初始数据插入
-- ===========================================

-- 插入默认管理员账号（密码: admin123，使用BCrypt加密）
INSERT INTO `users` (`username`, `password`, `email`, `nickname`, `role`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iEDAKmRa', 'admin@nutriai.com', '系统管理员', 'super_admin', 'active');

-- 插入食物分类
INSERT INTO `food_categories` (`name`, `name_en`, `parent_id`, `icon`, `sort_order`) VALUES
('谷物类', 'Grains', NULL, '🌾', 1),
('蔬菜类', 'Vegetables', NULL, '🥬', 2),
('水果类', 'Fruits', NULL, '🍎', 3),
('肉类', 'Meat', NULL, '🥩', 4),
('水产类', 'Seafood', NULL, '🐟', 5),
('蛋奶类', 'Dairy', NULL, '🥛', 6),
('豆制品', 'Legumes', NULL, '🫘', 7),
('坚果类', 'Nuts', NULL, '🌰', 8);

-- 插入常见食物营养数据（示例）
INSERT INTO `nutrition_data` (`food_name`, `food_name_en`, `category`, `serving_size`, `calories`, `protein`, `carbs`, `fat`) VALUES
('米饭', 'Rice', '谷物类', '100g', 116, 2.6, 25.9, 0.3),
('全麦面包', 'Whole Wheat Bread', '谷物类', '100g', 247, 13.2, 41.3, 3.4),
('鸡胸肉', 'Chicken Breast', '肉类', '100g', 165, 31.0, 0, 3.6),
('牛肉', 'Beef', '肉类', '100g', 250, 26.0, 0, 15.0),
('三文鱼', 'Salmon', '水产类', '100g', 208, 20.0, 0, 13.0),
('鸡蛋', 'Egg', '蛋奶类', '1个（50g）', 72, 6.3, 0.4, 4.8),
('牛奶', 'Milk', '蛋奶类', '100ml', 54, 3.0, 5.0, 3.2),
('西蓝花', 'Broccoli', '蔬菜类', '100g', 34, 2.8, 7.0, 0.4),
('苹果', 'Apple', '水果类', '100g', 52, 0.3, 14.0, 0.2),
('香蕉', 'Banana', '水果类', '100g', 89, 1.1, 23.0, 0.3),
('豆腐', 'Tofu', '豆制品', '100g', 76, 8.1, 1.9, 4.2),
('核桃', 'Walnut', '坚果类', '100g', 654, 15.2, 13.7, 65.2);

-- 插入系统配置
INSERT INTO `system_configs` (`config_key`, `config_value`, `config_type`, `description`, `is_public`) VALUES
('system.name', 'AI健康饮食规划助手', 'string', '系统名称', TRUE),
('system.version', '1.0.0', 'string', '系统版本', TRUE),
('ai.daily_free_quota', '3', 'number', '普通用户每日AI咨询次数', TRUE),
('ai.silver_quota', '10', 'number', '白银会员每日AI咨询次数', TRUE),
('ai.gold_quota', '20', 'number', '黄金会员每日AI咨询次数', TRUE),
('growth.upgrade_silver', '100', 'number', '升级白银会员所需成长值', TRUE),
('growth.upgrade_gold', '300', 'number', '升级黄金会员所需成长值', TRUE);

SET FOREIGN_KEY_CHECKS = 1;

-- ===========================================
-- 创建完成提示
-- ===========================================
SELECT '数据库初始化完成！' AS message, 
       '已创建13张核心表' AS tables, 
       '已插入初始数据' AS data;
