-- ====================================================================
-- NutriAI 生产环境数据库初始化脚本
-- 版本: 1.0.0  |  创建时间: 2026-03-26
-- 适用: 全新生产环境一次性初始化
-- 说明:
--   1. 包含全部完整表结构（DDL）及必要的初始化种子数据
--   2. 清除了所有测试/事务性数据，保留参考数据与管理员账户
--   3. 默认管理员账户: admin / 密码请部署后立即通过后台修改
--   4. 本文件作为 Docker MySQL 容器 /docker-entrypoint-initdb.d/init.sql 使用
-- ====================================================================

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+08:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

-- ====================================================================
-- 第一步：清除旧表（按外键依赖逆序）
-- ====================================================================
DROP VIEW  IF EXISTS `v_food_category_stats`;
DROP TABLE IF EXISTS `growth_records`;
DROP TABLE IF EXISTS `invitations`;
DROP TABLE IF EXISTS `members`;
DROP TABLE IF EXISTS `memberships`;
DROP TABLE IF EXISTS `chat_messages`;
DROP TABLE IF EXISTS `diet_plans`;
DROP TABLE IF EXISTS `diet_records`;
DROP TABLE IF EXISTS `favorites`;
DROP TABLE IF EXISTS `food_records`;
DROP TABLE IF EXISTS `health_profiles`;
DROP TABLE IF EXISTS `user_profiles`;
DROP TABLE IF EXISTS `food_portion_reference`;
DROP TABLE IF EXISTS `food_nutrition`;
DROP TABLE IF EXISTS `food_category`;
DROP TABLE IF EXISTS `food_categories`;
DROP TABLE IF EXISTS `food_recognition_history`;
DROP TABLE IF EXISTS `food_search_log`;
DROP TABLE IF EXISTS `vip_orders`;
DROP TABLE IF EXISTS `vip_plans`;
DROP TABLE IF EXISTS `ai_chat_favorite`;
DROP TABLE IF EXISTS `ai_chat_history`;
DROP TABLE IF EXISTS `ai_chat_log`;
DROP TABLE IF EXISTS `api_logs`;
DROP TABLE IF EXISTS `admin_operation_log`;
DROP TABLE IF EXISTS `diet_plan_history`;
DROP TABLE IF EXISTS `diet_plan_tasks`;
DROP TABLE IF EXISTS `nutrition_data`;
DROP TABLE IF EXISTS `operation_logs`;
DROP TABLE IF EXISTS `system_announcement`;
DROP TABLE IF EXISTS `system_config`;
DROP TABLE IF EXISTS `system_configs`;
DROP TABLE IF EXISTS `flyway_schema_history`;
DROP TABLE IF EXISTS `users`;
DROP TABLE IF EXISTS `member_levels`;

-- ====================================================================
-- 第二步：创建表（按外键依赖正序）
-- ====================================================================

-- ------------------------------------------------------------------
-- 用户基础表（无外键，最先创建）
-- ------------------------------------------------------------------
CREATE TABLE `users` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`          varchar(50)  NOT NULL COMMENT '用户名',
  `password`          varchar(100) NOT NULL COMMENT '密码（BCrypt加密）',
  `email`             varchar(100)          DEFAULT NULL COMMENT '邮箱',
  `phone`             varchar(20)           DEFAULT NULL COMMENT '手机号',
  `avatar`            varchar(255)          DEFAULT NULL COMMENT '头像URL',
  `nickname`          varchar(50)           DEFAULT NULL COMMENT '昵称',
  `role`  enum('USER','ADMIN','SUPER_ADMIN') NOT NULL DEFAULT 'USER',
  `status`            varchar(20)           DEFAULT 'ACTIVE',
  `member_level`      varchar(20)           DEFAULT 'FREE',
  `growth_value`      int                   DEFAULT '0',
  `member_expire_time` timestamp            DEFAULT NULL,
  `last_login_time`   datetime              DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip`     varchar(50)           DEFAULT NULL COMMENT '最后登录IP',
  `created_at`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email`    (`email`),
  UNIQUE KEY `uk_phone`    (`phone`),
  KEY `idx_role`       (`role`),
  KEY `idx_status`     (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户基础表';

-- ------------------------------------------------------------------
-- 会员等级配置表
-- ------------------------------------------------------------------
CREATE TABLE `member_levels` (
  `id`               bigint      NOT NULL AUTO_INCREMENT COMMENT '等级ID',
  `level_code`       varchar(20) NOT NULL COMMENT '等级代码',
  `level_name`       varchar(50) NOT NULL COMMENT '等级名称',
  `level_order`      int         NOT NULL COMMENT '等级顺序',
  `growth_required`  int         NOT NULL COMMENT '所需成长值',
  `benefits`         json                 DEFAULT NULL COMMENT '权益配置',
  `icon_url`         varchar(255)         DEFAULT NULL COMMENT '等级图标',
  `color`            varchar(20)          DEFAULT NULL COMMENT '等级颜色',
  `created_at`       timestamp            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       timestamp            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `level_code` (`level_code`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员等级配置表';

-- ------------------------------------------------------------------
-- 食物分类表（简化版，前端展示用）
-- ------------------------------------------------------------------
CREATE TABLE `food_categories` (
  `id`         bigint      NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name`       varchar(50) NOT NULL COMMENT '分类名称',
  `name_en`    varchar(50)          DEFAULT NULL COMMENT '英文名称',
  `parent_id`  bigint               DEFAULT NULL COMMENT '父分类ID',
  `icon`       varchar(100)         DEFAULT NULL COMMENT '分类图标',
  `sort_order` int         NOT NULL DEFAULT '0' COMMENT '排序',
  `created_at` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id`  (`parent_id`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食物分类表';

-- ------------------------------------------------------------------
-- 食材分类表（完整层级分类）
-- ------------------------------------------------------------------
CREATE TABLE `food_category` (
  `id`            bigint      NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `category_code` varchar(50) NOT NULL COMMENT '分类代码',
  `category_name` varchar(100) NOT NULL COMMENT '分类名称',
  `parent_id`     bigint               DEFAULT NULL COMMENT '父分类ID',
  `level`         int                  DEFAULT '1' COMMENT '分类层级（1-3）',
  `sort_order`    int                  DEFAULT '0' COMMENT '排序',
  `icon`          varchar(200)         DEFAULT NULL COMMENT '图标URL',
  `description`   text                 COMMENT '描述',
  `created_at`    timestamp            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    timestamp            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `category_code` (`category_code`),
  KEY `idx_parent_id`    (`parent_id`),
  KEY `idx_category_code`(`category_code`)
) ENGINE=InnoDB AUTO_INCREMENT=104 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='食材分类表';

-- ------------------------------------------------------------------
-- 食材营养数据表（依赖 food_category）
-- ------------------------------------------------------------------
CREATE TABLE `food_nutrition` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '食材ID',
  `food_code`         varchar(50)  NOT NULL COMMENT '食材代码',
  `food_name`         varchar(200) NOT NULL COMMENT '食材名称',
  `food_name_en`      varchar(200)          DEFAULT NULL COMMENT '英文名称',
  `category_id`       bigint       NOT NULL COMMENT '分类ID',
  `edible_portion`    decimal(5,2)          DEFAULT '100.00' COMMENT '可食部分（%）',
  `energy`            decimal(10,2)         DEFAULT '0.00' COMMENT '能量（kcal）',
  `protein`           decimal(10,2)         DEFAULT '0.00' COMMENT '蛋白质（g）',
  `fat`               decimal(10,2)         DEFAULT '0.00' COMMENT '脂肪（g）',
  `carbohydrate`      decimal(10,2)         DEFAULT '0.00' COMMENT '碳水化合物（g）',
  `dietary_fiber`     decimal(10,2)         DEFAULT '0.00' COMMENT '膳食纤维（g）',
  `vitamin_a`         decimal(10,2)         DEFAULT '0.00' COMMENT '维生素A（μg）',
  `vitamin_b1`        decimal(10,2)         DEFAULT '0.00' COMMENT '维生素B1（mg）',
  `vitamin_b2`        decimal(10,2)         DEFAULT '0.00' COMMENT '维生素B2（mg）',
  `vitamin_c`         decimal(10,2)         DEFAULT '0.00' COMMENT '维生素C（mg）',
  `vitamin_e`         decimal(10,2)         DEFAULT '0.00' COMMENT '维生素E（mg）',
  `calcium`           decimal(10,2)         DEFAULT '0.00' COMMENT '钙（mg）',
  `phosphorus`        decimal(10,2)         DEFAULT '0.00' COMMENT '磷（mg）',
  `potassium`         decimal(10,2)         DEFAULT '0.00' COMMENT '钾（mg）',
  `sodium`            decimal(10,2)         DEFAULT '0.00' COMMENT '钠（mg）',
  `magnesium`         decimal(10,2)         DEFAULT '0.00' COMMENT '镁（mg）',
  `iron`              decimal(10,2)         DEFAULT '0.00' COMMENT '铁（mg）',
  `zinc`              decimal(10,2)         DEFAULT '0.00' COMMENT '锌（mg）',
  `selenium`          decimal(10,2)         DEFAULT '0.00' COMMENT '硒（μg）',
  `cholesterol`       decimal(10,2)         DEFAULT '0.00' COMMENT '胆固醇（mg）',
  `water`             decimal(10,2)         DEFAULT '0.00' COMMENT '水分（g）',
  `tags`              varchar(500)          DEFAULT NULL COMMENT '标签（JSON数组）',
  `season`            varchar(100)          DEFAULT NULL COMMENT '季节',
  `storage_method`    varchar(200)          DEFAULT NULL COMMENT '储存方法',
  `cooking_methods`   varchar(500)          DEFAULT NULL COMMENT '烹饪方法（JSON数组）',
  `health_benefits`   text                  COMMENT '健康益处',
  `suitable_people`   varchar(500)          DEFAULT NULL COMMENT '适宜人群',
  `unsuitable_people` varchar(500)          DEFAULT NULL COMMENT '不适宜人群',
  `search_keywords`   varchar(500)          DEFAULT NULL COMMENT '搜索关键词',
  `pinyin`            varchar(200)          DEFAULT NULL COMMENT '拼音',
  `data_source`       varchar(100)          DEFAULT NULL COMMENT '数据来源',
  `reference_url`     varchar(500)          DEFAULT NULL COMMENT '参考链接',
  `status`            varchar(20)           DEFAULT 'ACTIVE' COMMENT '状态（ACTIVE/INACTIVE）',
  `is_verified`       tinyint(1)            DEFAULT '0' COMMENT '是否已验证',
  `created_at`        timestamp             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        timestamp             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `food_code` (`food_code`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_food_code`   (`food_code`),
  KEY `idx_food_name`   (`food_name`),
  KEY `idx_status`      (`status`),
  FULLTEXT KEY `ft_search` (`food_name`,`search_keywords`,`pinyin`) COMMENT '全文搜索索引',
  CONSTRAINT `food_nutrition_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `food_category` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=65 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='食材营养数据表';

-- ------------------------------------------------------------------
-- 食材份量参考表（依赖 food_nutrition）
-- ------------------------------------------------------------------
CREATE TABLE `food_portion_reference` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `food_id`        bigint       NOT NULL COMMENT '食材ID',
  `portion_name`   varchar(100) NOT NULL COMMENT '份量名称（如：一个、一碗、一份）',
  `portion_weight` decimal(10,2) NOT NULL COMMENT '份量重量（g）',
  `description`    varchar(200)          DEFAULT NULL COMMENT '描述',
  `created_at`     timestamp             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_food_id` (`food_id`),
  CONSTRAINT `food_portion_reference_ibfk_1` FOREIGN KEY (`food_id`) REFERENCES `food_nutrition` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='食材份量参考表';

-- ------------------------------------------------------------------
-- 食材搜索日志表
-- ------------------------------------------------------------------
CREATE TABLE `food_search_log` (
  `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id`          bigint                DEFAULT NULL COMMENT '用户ID',
  `search_keyword`   varchar(200) NOT NULL COMMENT '搜索关键词',
  `result_count`     int                   DEFAULT '0' COMMENT '结果数量',
  `clicked_food_id`  bigint                DEFAULT NULL COMMENT '点击的食材ID',
  `search_time`      timestamp             DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id`        (`user_id`),
  KEY `idx_search_keyword` (`search_keyword`),
  KEY `idx_search_time`    (`search_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='食材搜索日志表';

-- ------------------------------------------------------------------
-- 食物识别历史记录表
-- ------------------------------------------------------------------
CREATE TABLE `food_recognition_history` (
  `id`                 bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`            bigint       NOT NULL COMMENT '用户ID',
  `recognition_type`   varchar(20)           DEFAULT 'TEXT' COMMENT '识别方式：TEXT/IMAGE',
  `input_text`         varchar(200)          DEFAULT NULL COMMENT '输入文本',
  `image_url`          varchar(500)          DEFAULT NULL COMMENT '图片URL',
  `recognition_result` json                  DEFAULT NULL COMMENT '识别结果（JSON格式）',
  `created_at`         timestamp             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id`          (`user_id`),
  KEY `idx_created_at`       (`created_at`),
  KEY `idx_recognition_type` (`recognition_type`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食物识别历史记录';

-- ------------------------------------------------------------------
-- 饮食记录表（依赖 users）
-- ------------------------------------------------------------------
CREATE TABLE `food_records` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id`      bigint       NOT NULL COMMENT '用户ID',
  `meal_type`    varchar(20)  NOT NULL COMMENT '餐次类型：BREAKFAST/LUNCH/DINNER/SNACK',
  `food_name`    varchar(100) NOT NULL COMMENT '食物名称',
  `photo_url`    varchar(500)          DEFAULT NULL COMMENT '食物照片URL',
  `portion`      decimal(8,2)          DEFAULT NULL COMMENT '份量（克）',
  `calories`     decimal(8,2)          DEFAULT NULL COMMENT '卡路里（千卡）',
  `protein`      decimal(8,2)          DEFAULT NULL COMMENT '蛋白质（克）',
  `carbohydrates` decimal(8,2)         DEFAULT NULL COMMENT '碳水化合物（克）',
  `fat`           decimal(8,2)         DEFAULT NULL COMMENT '脂肪（克）',
  `fiber`         decimal(8,2)         DEFAULT NULL COMMENT '纤维（克）',
  `record_time`   datetime    NOT NULL COMMENT '记录时间',
  `notes`         varchar(500)         DEFAULT NULL COMMENT '备注',
  `created_at`    timestamp            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    timestamp            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id`    (`user_id`),
  KEY `idx_record_time`(`record_time`),
  KEY `idx_user_meal`  (`user_id`,`meal_type`),
  KEY `idx_user_time`  (`user_id`,`record_time`),
  CONSTRAINT `fk_food_record_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='饮食记录表';

-- ------------------------------------------------------------------
-- 食物营养数据表（简化版，供前端展示）
-- ------------------------------------------------------------------
CREATE TABLE `nutrition_data` (
  `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '食物ID',
  `food_name`        varchar(100) NOT NULL COMMENT '食物名称',
  `food_name_en`     varchar(100)          DEFAULT NULL COMMENT '英文名称',
  `category`         varchar(50)  NOT NULL COMMENT '食物分类',
  `serving_size`     varchar(50)  NOT NULL COMMENT '标准份量',
  `calories`         decimal(8,2) NOT NULL COMMENT '热量（kcal/100g）',
  `protein`          decimal(8,2) NOT NULL COMMENT '蛋白质（g/100g）',
  `carbs`            decimal(8,2) NOT NULL COMMENT '碳水化合物（g/100g）',
  `fat`              decimal(8,2) NOT NULL COMMENT '脂肪（g/100g）',
  `fiber`            decimal(8,2)          DEFAULT NULL COMMENT '膳食纤维（g/100g）',
  `sodium`           decimal(8,2)          DEFAULT NULL COMMENT '钠（mg/100g）',
  `vitamin_c`        decimal(8,2)          DEFAULT NULL COMMENT '维生素C（mg/100g）',
  `calcium`          decimal(8,2)          DEFAULT NULL COMMENT '钙（mg/100g）',
  `iron`             decimal(8,2)          DEFAULT NULL COMMENT '铁（mg/100g）',
  `image_url`        varchar(255)          DEFAULT NULL COMMENT '食物图片URL',
  `description`      text                  COMMENT '食物描述',
  `common_allergens` json                  DEFAULT NULL COMMENT '常见过敏源（JSON数组）',
  `source`           varchar(50)           DEFAULT 'manual' COMMENT '数据来源',
  `created_at`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_food_name` (`food_name`),
  KEY `idx_category`  (`category`),
  KEY `idx_calories`  (`calories`),
  FULLTEXT KEY `ft_food_name` (`food_name`,`food_name_en`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食物营养数据表';

-- ------------------------------------------------------------------
-- 会员信息表（依赖 users, member_levels）
-- ------------------------------------------------------------------
CREATE TABLE `members` (
  `id`               bigint      NOT NULL AUTO_INCREMENT COMMENT '会员ID',
  `user_id`          bigint      NOT NULL COMMENT '用户ID',
  `level_id`         bigint      NOT NULL COMMENT '当前等级ID',
  `total_growth`     int                  DEFAULT '0' COMMENT '总成长值',
  `current_growth`   int                  DEFAULT '0' COMMENT '当前等级成长值',
  `invitation_code`  varchar(20) NOT NULL COMMENT '邀请码',
  `invited_by`       bigint               DEFAULT NULL COMMENT '邀请人ID',
  `invitation_count` int                  DEFAULT '0' COMMENT '邀请人数',
  `is_active`        tinyint(1)           DEFAULT '1' COMMENT '是否激活',
  `activated_at`     timestamp            DEFAULT NULL COMMENT '激活时间',
  `expire_at`        timestamp            DEFAULT NULL COMMENT '过期时间',
  `created_at`       timestamp            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       timestamp            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id`         (`user_id`),
  UNIQUE KEY `invitation_code` (`invitation_code`),
  KEY `level_id`              (`level_id`),
  KEY `idx_user_id`           (`user_id`),
  KEY `idx_invitation_code`   (`invitation_code`),
  KEY `idx_invited_by`        (`invited_by`),
  CONSTRAINT `members_ibfk_1` FOREIGN KEY (`user_id`)     REFERENCES `users`         (`id`) ON DELETE CASCADE,
  CONSTRAINT `members_ibfk_2` FOREIGN KEY (`level_id`)    REFERENCES `member_levels` (`id`),
  CONSTRAINT `members_ibfk_3` FOREIGN KEY (`invited_by`)  REFERENCES `users`         (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员信息表';

-- ------------------------------------------------------------------
-- 旧版会员信息表（保留兼容性）
-- ------------------------------------------------------------------
CREATE TABLE `memberships` (
  `id`                    bigint      NOT NULL AUTO_INCREMENT COMMENT '会员ID',
  `user_id`               bigint      NOT NULL COMMENT '用户ID',
  `level` enum('free','silver','gold') NOT NULL DEFAULT 'free' COMMENT '会员等级',
  `growth_points`         int         NOT NULL DEFAULT '0' COMMENT '成长值',
  `expire_date`           date                 DEFAULT NULL COMMENT '会员到期日期',
  `ai_quota_used`         int         NOT NULL DEFAULT '0' COMMENT '今日已使用AI次数',
  `ai_quota_total`        int         NOT NULL DEFAULT '3' COMMENT '今日AI总配额',
  `last_quota_reset_date` date                 DEFAULT NULL COMMENT '配额重置日期',
  `invite_code`           varchar(20)          DEFAULT NULL COMMENT '邀请码',
  `invited_by`            bigint               DEFAULT NULL COMMENT '邀请人ID',
  `created_at`            datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`            datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id`     (`user_id`),
  UNIQUE KEY `uk_invite_code` (`invite_code`),
  KEY `idx_level`       (`level`),
  KEY `idx_growth_points`(`growth_points`),
  KEY `idx_invited_by`  (`invited_by`),
  CONSTRAINT `fk_membership_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员信息表（旧版）';

-- ------------------------------------------------------------------
-- 邀请记录表（依赖 users）
-- ------------------------------------------------------------------
CREATE TABLE `invitations` (
  `id`              bigint      NOT NULL AUTO_INCREMENT COMMENT '邀请ID',
  `inviter_id`      bigint      NOT NULL COMMENT '邀请人ID',
  `invitee_id`      bigint               DEFAULT NULL COMMENT '被邀请人ID',
  `invitation_code` varchar(20) NOT NULL COMMENT '邀请码',
  `status`          varchar(20)          DEFAULT 'PENDING' COMMENT '状态: PENDING/ACCEPTED/EXPIRED',
  `invited_at`      timestamp            DEFAULT CURRENT_TIMESTAMP COMMENT '邀请时间',
  `accepted_at`     timestamp            DEFAULT NULL COMMENT '接受时间',
  `reward_growth`   int                  DEFAULT '0' COMMENT '奖励成长值',
  `is_rewarded`     tinyint(1)           DEFAULT '0' COMMENT '是否已发放奖励',
  PRIMARY KEY (`id`),
  KEY `idx_inviter_id`      (`inviter_id`),
  KEY `idx_invitee_id`      (`invitee_id`),
  KEY `idx_invitation_code` (`invitation_code`),
  KEY `idx_status`          (`status`),
  CONSTRAINT `invitations_ibfk_1` FOREIGN KEY (`inviter_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `invitations_ibfk_2` FOREIGN KEY (`invitee_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请记录表';

-- ------------------------------------------------------------------
-- 成长值记录表（依赖 members, users）
-- ------------------------------------------------------------------
CREATE TABLE `growth_records` (
  `id`           bigint      NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `member_id`    bigint      NOT NULL COMMENT '会员ID',
  `user_id`      bigint      NOT NULL COMMENT '用户ID',
  `growth_value` int         NOT NULL COMMENT '成长值',
  `growth_type`  varchar(50) NOT NULL COMMENT '成长值类型',
  `description`  varchar(255)         DEFAULT NULL COMMENT '描述',
  `related_id`   bigint               DEFAULT NULL COMMENT '关联业务ID',
  `created_at`   timestamp            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_member_id`  (`member_id`),
  KEY `idx_user_id`    (`user_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_growth_type`(`growth_type`),
  CONSTRAINT `growth_records_ibfk_1` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`) ON DELETE CASCADE,
  CONSTRAINT `growth_records_ibfk_2` FOREIGN KEY (`user_id`)   REFERENCES `users`   (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成长值记录表';

-- ------------------------------------------------------------------
-- AI聊天消息表（依赖 users）
-- ------------------------------------------------------------------
CREATE TABLE `chat_messages` (
  `id`            bigint      NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `user_id`       bigint      NOT NULL COMMENT '用户ID',
  `session_id`    varchar(50) NOT NULL COMMENT '会话ID',
  `role` enum('user','assistant','system') NOT NULL COMMENT '角色',
  `content`       text        NOT NULL COMMENT '消息内容',
  `tokens_used`   int                  DEFAULT NULL COMMENT '使用tokens数',
  `model`         varchar(50)          DEFAULT NULL COMMENT '使用模型',
  `response_time` int                  DEFAULT NULL COMMENT '响应时间（ms）',
  `created_at`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_session` (`user_id`,`session_id`),
  KEY `idx_role`         (`role`),
  KEY `idx_created_at`   (`created_at`),
  CONSTRAINT `fk_chat_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天消息表';

-- ------------------------------------------------------------------
-- AI生成饮食计划表（依赖 users）
-- ------------------------------------------------------------------
CREATE TABLE `diet_plans` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `user_id`       bigint       NOT NULL COMMENT '用户ID',
  `title`         varchar(100) NOT NULL COMMENT '计划标题',
  `description`   text                  DEFAULT NULL COMMENT '计划描述',
  `plan_type` enum('weight_loss','muscle_gain','balanced','custom') NOT NULL COMMENT '计划类型',
  `duration_days` int          NOT NULL COMMENT '计划天数',
  `daily_budget`  decimal(10,2)         DEFAULT NULL COMMENT '每日预算（元）',
  `plan_content`  json         NOT NULL COMMENT '计划内容（JSON结构）',
  `is_favorite`   tinyint(1)   NOT NULL DEFAULT '0' COMMENT '是否收藏',
  `status` enum('active','completed','cancelled') NOT NULL DEFAULT 'active' COMMENT '计划状态',
  `start_date`    date                  DEFAULT NULL COMMENT '开始日期',
  `end_date`      date                  DEFAULT NULL COMMENT '结束日期',
  `created_at`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id`    (`user_id`),
  KEY `idx_plan_type`  (`plan_type`),
  KEY `idx_status`     (`status`),
  KEY `idx_is_favorite`(`is_favorite`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_plan_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI生成饮食计划表';

-- ------------------------------------------------------------------
-- 饮食记录表（依赖 users）
-- ------------------------------------------------------------------
CREATE TABLE `diet_records` (
  `id`              bigint  NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id`         bigint  NOT NULL COMMENT '用户ID',
  `record_date`     date    NOT NULL COMMENT '记录日期',
  `meal_type` enum('breakfast','lunch','dinner','snack') NOT NULL COMMENT '餐次',
  `meal_time`       time             DEFAULT NULL COMMENT '用餐时间',
  `food_items`      json    NOT NULL COMMENT '食物列表（JSON数组）',
  `total_calories`  int              DEFAULT NULL COMMENT '总热量（kcal）',
  `total_protein`   decimal(8,2)     DEFAULT NULL COMMENT '总蛋白质（g）',
  `total_carbs`     decimal(8,2)     DEFAULT NULL COMMENT '总碳水化合物（g）',
  `total_fat`       decimal(8,2)     DEFAULT NULL COMMENT '总脂肪（g）',
  `images`          json             DEFAULT NULL COMMENT '照片URL列表（JSON数组）',
  `notes`           text             DEFAULT NULL COMMENT '备注',
  `created_at`      datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_date`  (`user_id`,`record_date`),
  KEY `idx_meal_type`  (`meal_type`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_diet_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='饮食记录表';

-- ------------------------------------------------------------------
-- 饮食计划历史记录表
-- ------------------------------------------------------------------
CREATE TABLE `diet_plan_history` (
  `id`               bigint      NOT NULL AUTO_INCREMENT,
  `plan_id`          varchar(50) NOT NULL COMMENT '计划ID',
  `user_id`          bigint      NOT NULL COMMENT '用户ID',
  `title`            varchar(200) NOT NULL COMMENT '计划标题',
  `days`             int         NOT NULL COMMENT '计划天数',
  `goal`             varchar(50)          DEFAULT NULL COMMENT '目标',
  `markdown_content` longtext             COMMENT 'Markdown内容',
  `is_favorite`      tinyint(1)  NOT NULL DEFAULT '0' COMMENT '是否收藏',
  `created_at`       timestamp            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       timestamp            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `plan_id`       (`plan_id`),
  KEY `idx_user_id`    (`user_id`),
  KEY `idx_plan_id`    (`plan_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_is_favorite`(`is_favorite`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='饮食计划历史记录表';

-- ------------------------------------------------------------------
-- 饮食计划生成任务表
-- ------------------------------------------------------------------
CREATE TABLE `diet_plan_tasks` (
  `id`            bigint      NOT NULL AUTO_INCREMENT,
  `task_id`       varchar(50) NOT NULL COMMENT '任务ID',
  `user_id`       bigint      NOT NULL COMMENT '用户ID',
  `status`        varchar(20) NOT NULL DEFAULT 'pending' COMMENT '任务状态: pending/running/completed/failed/cancelled',
  `progress`      int                  DEFAULT '0' COMMENT '进度百分比',
  `total_days`    int         NOT NULL COMMENT '总天数',
  `current_day`   int                  DEFAULT '0' COMMENT '当前生成到第几天',
  `plan_id`       varchar(50)          DEFAULT NULL COMMENT '生成的计划ID（完成后）',
  `request_data`  text                 COMMENT '请求参数JSON',
  `error_message` text                 COMMENT '错误信息',
  `created_at`    timestamp            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    timestamp            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `task_id` (`task_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_status`  (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='饮食计划生成任务表';

-- ------------------------------------------------------------------
-- 用户收藏表（依赖 users）
-- ------------------------------------------------------------------
CREATE TABLE `favorites` (
  `id`        bigint  NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id`   bigint  NOT NULL COMMENT '用户ID',
  `item_type` enum('diet_plan','recipe','food','chat_message') NOT NULL COMMENT '收藏类型',
  `item_id`   bigint  NOT NULL COMMENT '收藏项ID',
  `notes`     text             DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_item` (`user_id`,`item_type`,`item_id`),
  KEY `idx_item_type`  (`item_type`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏表';

-- ------------------------------------------------------------------
-- 健康档案表（依赖 users）
-- ------------------------------------------------------------------
CREATE TABLE `health_profiles` (
  `id`                  bigint        NOT NULL AUTO_INCREMENT,
  `user_id`             bigint        NOT NULL COMMENT '关联用户ID',
  `gender`              varchar(10)            DEFAULT NULL COMMENT '性别: MALE/FEMALE',
  `birth_date`          date                   DEFAULT NULL COMMENT '出生日期',
  `height`              decimal(5,1)           DEFAULT NULL COMMENT '身高(cm)',
  `weight`              decimal(5,1)           DEFAULT NULL COMMENT '体重(kg)',
  `target_weight`       decimal(5,1)           DEFAULT NULL COMMENT '目标体重(kg)',
  `bmi`                 decimal(4,1)           DEFAULT NULL COMMENT 'BMI指数（自动计算）',
  `activity_level`      varchar(20)            DEFAULT NULL COMMENT '活动量: SEDENTARY/LIGHT/MODERATE/ACTIVE/VERY_ACTIVE',
  `health_goals`        varchar(500)           DEFAULT NULL COMMENT '健康目标(JSON数组)',
  `dietary_restrictions` varchar(500)          DEFAULT NULL COMMENT '饮食限制(JSON数组)',
  `allergies`           varchar(500)           DEFAULT NULL COMMENT '过敏源(JSON数组)',
  `medical_conditions`  varchar(500)           DEFAULT NULL COMMENT '健康状况(JSON数组)',
  `daily_calorie_target` int                   DEFAULT NULL COMMENT '每日卡路里目标(kcal)',
  `waist_circumference` decimal(5,1)           DEFAULT NULL COMMENT '腰围(cm)',
  `body_fat_percentage` decimal(4,1)           DEFAULT NULL COMMENT '体脂率(%)',
  `notes`               text                   COMMENT '备注',
  `created_at`          datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`          datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  CONSTRAINT `fk_health_profiles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康档案表';

-- ------------------------------------------------------------------
-- 用户健康档案表（旧版，依赖 users）
-- ------------------------------------------------------------------
CREATE TABLE `user_profiles` (
  `id`                     bigint  NOT NULL AUTO_INCREMENT COMMENT '档案ID',
  `user_id`                bigint  NOT NULL COMMENT '用户ID',
  `gender` enum('male','female','other') DEFAULT NULL COMMENT '性别',
  `birth_date`             date             DEFAULT NULL COMMENT '出生日期',
  `height`                 decimal(5,2)     DEFAULT NULL COMMENT '身高（cm）',
  `weight`                 decimal(5,2)     DEFAULT NULL COMMENT '体重（kg）',
  `bmi`                    decimal(5,2)     DEFAULT NULL COMMENT 'BMI指数',
  `health_goal` enum('lose_weight','gain_muscle','maintain','improve_health') DEFAULT 'maintain' COMMENT '健康目标',
  `activity_level` enum('sedentary','light','moderate','active','very_active') DEFAULT 'moderate' COMMENT '活动水平',
  `allergies`              json             DEFAULT NULL COMMENT '过敏源列表（JSON数组）',
  `dietary_restrictions`   json             DEFAULT NULL COMMENT '饮食限制（JSON数组）',
  `medical_conditions`     text             DEFAULT NULL COMMENT '健康状况说明',
  `daily_calorie_target`   int              DEFAULT NULL COMMENT '每日热量目标（kcal）',
  `daily_protein_target`   int              DEFAULT NULL COMMENT '每日蛋白质目标（g）',
  `created_at`             datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`             datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id`    (`user_id`),
  KEY `idx_health_goal`      (`health_goal`),
  KEY `idx_bmi`              (`bmi`),
  CONSTRAINT `fk_profile_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户健康档案表';

-- ------------------------------------------------------------------
-- AI聊天收藏表
-- ------------------------------------------------------------------
CREATE TABLE `ai_chat_favorite` (
  `id`              bigint  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`         bigint  NOT NULL COMMENT '用户ID',
  `message_content` text    NOT NULL COMMENT '收藏的消息内容',
  `message_role`    varchar(20) NOT NULL COMMENT '消息角色：user/assistant',
  `created_at`      timestamp   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id`    (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天收藏表';

-- ------------------------------------------------------------------
-- AI聊天历史记录表
-- ------------------------------------------------------------------
CREATE TABLE `ai_chat_history` (
  `id`         bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`    bigint       NOT NULL COMMENT '用户ID',
  `title`      varchar(200) NOT NULL COMMENT '对话标题',
  `messages`   json         NOT NULL COMMENT '消息列表（JSON格式）',
  `created_at` timestamp            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id`    (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天历史记录表';

-- ------------------------------------------------------------------
-- AI聊天日志表
-- ------------------------------------------------------------------
CREATE TABLE `ai_chat_log` (
  `id`            bigint      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`       bigint      NOT NULL COMMENT '用户ID',
  `session_id`    varchar(100)         DEFAULT NULL COMMENT '会话ID',
  `user_message`  text        NOT NULL COMMENT '用户消息',
  `ai_response`   text                 DEFAULT NULL COMMENT 'AI回复',
  `model`         varchar(50)          DEFAULT NULL COMMENT 'AI模型',
  `tokens_used`   int                  DEFAULT NULL COMMENT '使用的token数',
  `response_time` int                  DEFAULT NULL COMMENT '响应时间(ms)',
  `status`        varchar(20)          DEFAULT NULL COMMENT '状态：success/error',
  `error_message` text                 DEFAULT NULL COMMENT '错误信息',
  `created_at`    timestamp            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id`    (`user_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_status`     (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天日志表';

-- ------------------------------------------------------------------
-- 第三方API调用日志表
-- ------------------------------------------------------------------
CREATE TABLE `api_logs` (
  `id`            bigint  NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id`       bigint           DEFAULT NULL COMMENT '用户ID',
  `api_type` enum('tongyi','oss','image_recognition','other') NOT NULL COMMENT 'API类型',
  `api_name`      varchar(100) NOT NULL COMMENT 'API名称',
  `request_data`  text             DEFAULT NULL COMMENT '请求数据',
  `response_data` text             DEFAULT NULL COMMENT '响应数据',
  `tokens_used`   int              DEFAULT NULL COMMENT '使用tokens数',
  `cost`          decimal(10,4)    DEFAULT NULL COMMENT '费用（元）',
  `status_code`   int              DEFAULT NULL COMMENT 'HTTP状态码',
  `response_time` int              DEFAULT NULL COMMENT '响应时间（ms）',
  `error_message` text             DEFAULT NULL COMMENT '错误信息',
  `created_at`    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id`     (`user_id`),
  KEY `idx_api_type`    (`api_type`),
  KEY `idx_status_code` (`status_code`),
  KEY `idx_created_at`  (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='第三方API调用日志表';

-- ------------------------------------------------------------------
-- 管理员操作日志表
-- ------------------------------------------------------------------
CREATE TABLE `admin_operation_log` (
  `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `admin_id`        bigint       NOT NULL COMMENT '管理员ID',
  `operation_type`  varchar(50)  NOT NULL COMMENT '操作类型',
  `operation_desc`  varchar(500)          DEFAULT NULL COMMENT '操作描述',
  `target_type`     varchar(50)           DEFAULT NULL COMMENT '目标类型：user/config/announcement',
  `target_id`       bigint                DEFAULT NULL COMMENT '目标ID',
  `ip_address`      varchar(50)           DEFAULT NULL COMMENT 'IP地址',
  `user_agent`      varchar(500)          DEFAULT NULL COMMENT '用户代理',
  `request_params`  text                  DEFAULT NULL COMMENT '请求参数',
  `created_at`      timestamp             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_admin_id`       (`admin_id`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_created_at`     (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员操作日志表';

-- ------------------------------------------------------------------
-- 操作日志表
-- ------------------------------------------------------------------
CREATE TABLE `operation_logs` (
  `id`             bigint      NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id`        bigint               DEFAULT NULL COMMENT '用户ID',
  `operation`      varchar(50) NOT NULL COMMENT '操作类型',
  `module`         varchar(50) NOT NULL COMMENT '模块名称',
  `method`         varchar(100)         DEFAULT NULL COMMENT '方法名',
  `params`         text                 DEFAULT NULL COMMENT '请求参数',
  `result`         text                 DEFAULT NULL COMMENT '返回结果',
  `ip_address`     varchar(50)          DEFAULT NULL COMMENT 'IP地址',
  `user_agent`     varchar(255)         DEFAULT NULL COMMENT 'User Agent',
  `status` enum('success','failure') NOT NULL DEFAULT 'success' COMMENT '操作状态',
  `error_message`  text                 DEFAULT NULL COMMENT '错误信息',
  `execution_time` int                  DEFAULT NULL COMMENT '执行时间（ms）',
  `created_at`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id`    (`user_id`),
  KEY `idx_operation`  (`operation`),
  KEY `idx_status`     (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ------------------------------------------------------------------
-- VIP套餐配置表
-- ------------------------------------------------------------------
CREATE TABLE `vip_plans` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `plan_code`      VARCHAR(20)     NOT NULL COMMENT '套餐代码：MONTHLY/QUARTERLY/YEARLY',
  `plan_name`      VARCHAR(100)    NOT NULL COMMENT '套餐名称',
  `description`    VARCHAR(500)             DEFAULT NULL COMMENT '套餐描述',
  `original_price` DECIMAL(10,2)   NOT NULL COMMENT '原价（元）',
  `discount_price` DECIMAL(10,2)   NOT NULL COMMENT '优惠价（元）',
  `duration_days`  INT             NOT NULL COMMENT '有效天数',
  `benefits`       JSON                     DEFAULT NULL COMMENT '权益配置JSON',
  `bonus_growth`   INT             NOT NULL DEFAULT 0 COMMENT '购买赠送成长值',
  `is_active`      TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用',
  `sort_order`     INT             NOT NULL DEFAULT 0 COMMENT '排序权重',
  `badge`          VARCHAR(20)              DEFAULT NULL COMMENT '促销标签',
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_code` (`plan_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VIP套餐配置';

-- ------------------------------------------------------------------
-- VIP充值订单表
-- ------------------------------------------------------------------
CREATE TABLE `vip_orders` (
  `id`                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_no`             VARCHAR(32)     NOT NULL COMMENT '业务订单号',
  `user_id`              BIGINT          NOT NULL COMMENT '用户ID',
  `plan_id`              BIGINT          NOT NULL COMMENT '套餐ID',
  `plan_name`            VARCHAR(100)    NOT NULL COMMENT '套餐名称（冗余）',
  `amount`               DECIMAL(10,2)   NOT NULL COMMENT '实付金额',
  `payment_method`       VARCHAR(20)     NOT NULL DEFAULT 'ALIPAY' COMMENT '支付方式',
  `payment_status`       VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '支付状态',
  `trade_no`             VARCHAR(64)              DEFAULT NULL COMMENT '支付宝交易流水号',
  `paid_at`              DATETIME                 DEFAULT NULL COMMENT '实际支付时间',
  `expire_time`          DATETIME                 DEFAULT NULL COMMENT '订单超时取消时间',
  `vip_expire_at`        DATETIME                 DEFAULT NULL COMMENT 'VIP到期时间',
  `bonus_growth_granted` TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '赠送成长值是否已发放',
  `notify_data`          TEXT                     DEFAULT NULL COMMENT '支付回调原始数据',
  `remark`               VARCHAR(500)             DEFAULT NULL,
  `created_at`           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id`       (`user_id`),
  KEY `idx_trade_no`      (`trade_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VIP充值订单';

-- ------------------------------------------------------------------
-- 系统公告表
-- ------------------------------------------------------------------
CREATE TABLE `system_announcement` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title`       varchar(200) NOT NULL COMMENT '公告标题',
  `content`     text         NOT NULL COMMENT '公告内容',
  `type`        varchar(20)  NOT NULL COMMENT '公告类型：info/warning/error',
  `priority`    int                   DEFAULT '0' COMMENT '优先级（数字越大越优先）',
  `is_active`   tinyint(1)            DEFAULT '1' COMMENT '是否启用',
  `start_time`  timestamp             DEFAULT NULL COMMENT '开始时间',
  `end_time`    timestamp             DEFAULT NULL COMMENT '结束时间',
  `created_by`  bigint                DEFAULT NULL COMMENT '创建人ID',
  `created_at`  timestamp             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  timestamp             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_is_active`  (`is_active`),
  KEY `idx_priority`   (`priority`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统公告表';

-- ------------------------------------------------------------------
-- 系统配置表（主配置）
-- ------------------------------------------------------------------
CREATE TABLE `system_config` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key`   varchar(100) NOT NULL COMMENT '配置键',
  `config_value` text         NOT NULL COMMENT '配置值',
  `config_type`  varchar(20)  NOT NULL COMMENT '配置类型：string/number/boolean/json',
  `description`  varchar(500)          DEFAULT NULL COMMENT '配置描述',
  `category`     varchar(50)           DEFAULT NULL COMMENT '配置分类',
  `is_public`    tinyint(1)            DEFAULT '0' COMMENT '是否公开（前端可访问）',
  `created_at`   timestamp             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   timestamp             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `config_key`  (`config_key`),
  KEY `idx_category`  (`category`),
  KEY `idx_is_public` (`is_public`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ------------------------------------------------------------------
-- 系统配置表（旧版）
-- ------------------------------------------------------------------
CREATE TABLE `system_configs` (
  `id`           bigint      NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key`   varchar(100) NOT NULL COMMENT '配置键',
  `config_value` text         NOT NULL COMMENT '配置值',
  `config_type` enum('string','number','boolean','json') NOT NULL DEFAULT 'string' COMMENT '配置类型',
  `description`  varchar(255)          DEFAULT NULL COMMENT '配置描述',
  `is_public`    tinyint(1)   NOT NULL DEFAULT '0' COMMENT '是否公开',
  `created_at`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表（旧版）';

-- ------------------------------------------------------------------
-- Flyway 迁移历史记录表
-- ------------------------------------------------------------------
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int          NOT NULL,
  `version`        varchar(50)           DEFAULT NULL,
  `description`    varchar(200) NOT NULL,
  `type`           varchar(20)  NOT NULL,
  `script`         varchar(1000) NOT NULL,
  `checksum`       int                   DEFAULT NULL,
  `installed_by`   varchar(100) NOT NULL,
  `installed_on`   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int          NOT NULL,
  `success`        tinyint(1)   NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ====================================================================
-- 第三步：插入初始化/种子数据
-- ====================================================================

-- ------------------------------------------------------------------
-- 管理员用户（BCrypt 密码，请部署后通过后台立即修改）
-- ------------------------------------------------------------------
LOCK TABLES `users` WRITE;
INSERT INTO `users` VALUES
(1,'admin','$2a$10$N7TG/HHY9rfCcY5/eX6rDu6BN.eFsSwsC2dvwwJQqb/e4UPpjp02a',
 'admin@nutriai.com',NULL,NULL,'管理员','ADMIN','ACTIVE','GOLD',0,NULL,
 NULL,NULL,NOW(),NOW());
UNLOCK TABLES;

-- ------------------------------------------------------------------
-- 会员等级数据
-- ------------------------------------------------------------------
LOCK TABLES `member_levels` WRITE;
INSERT INTO `member_levels` VALUES
(1,'ROOKIE','新手会员',1,0,
 '{"features":["基础营养记录","AI咨询(3次/天)","基础数据分析"],"maxAiQueries":3,"maxFoodRecords":10}',
 '/icons/level-rookie.png','#95a5a6',NOW(),NOW()),
(2,'BRONZE','青铜会员',2,100,
 '{"features":["营养记录无限","AI咨询(10次/天)","高级数据分析","自定义目标"],"maxAiQueries":10,"maxFoodRecords":-1}',
 '/icons/level-bronze.png','#cd7f32',NOW(),NOW()),
(3,'SILVER','白银会员',3,500,
 '{"features":["所有青铜权益","AI咨询(30次/天)","专属营养报告","优先客服"],"maxAiQueries":30,"maxFoodRecords":-1}',
 '/icons/level-silver.png','#c0c0c0',NOW(),NOW()),
(4,'GOLD','黄金会员',4,2000,
 '{"features":["所有白银权益","AI咨询无限","个性化食谱","健康顾问","数据导出"],"maxAiQueries":-1,"maxFoodRecords":-1}',
 '/icons/level-gold.png','#ffd700',NOW(),NOW()),
(5,'PLATINUM','铂金会员',5,5000,
 '{"features":["所有黄金权益","专属营养师","线下活动","合作商家折扣","终身成长值加成"],"growthBonus":1.5,"maxAiQueries":-1,"maxFoodRecords":-1}',
 '/icons/level-platinum.png','#e5e4e2',NOW(),NOW());
UNLOCK TABLES;

-- ------------------------------------------------------------------
-- 管理员会员记录（GOLD等级）
-- ------------------------------------------------------------------
LOCK TABLES `members` WRITE;
INSERT INTO `members`
  (id,user_id,level_id,total_growth,current_growth,invitation_code,
   invited_by,invitation_count,is_active,activated_at,expire_at,created_at,updated_at)
VALUES
  (1,1,4,0,0,'INV000001000000',NULL,0,1,NULL,NULL,NOW(),NOW());
UNLOCK TABLES;

-- ------------------------------------------------------------------
-- 食物分类（简化版，前端展示用）
-- ------------------------------------------------------------------
LOCK TABLES `food_categories` WRITE;
INSERT INTO `food_categories` VALUES
(1,'谷物类','Grains',NULL,'🌾',1,NOW()),
(2,'蔬菜类','Vegetables',NULL,'🥬',2,NOW()),
(3,'水果类','Fruits',NULL,'🍎',3,NOW()),
(4,'肉类','Meat',NULL,'🥩',4,NOW()),
(5,'水产类','Seafood',NULL,'🐟',5,NOW()),
(6,'蛋奶类','Dairy',NULL,'🥛',6,NOW()),
(7,'豆制品','Legumes',NULL,'🫘',7,NOW()),
(8,'坚果类','Nuts',NULL,'🌰',8,NOW());
UNLOCK TABLES;

-- ------------------------------------------------------------------
-- 食材分类（完整层级）
-- ------------------------------------------------------------------
LOCK TABLES `food_category` WRITE;
INSERT INTO `food_category` VALUES
(1,'CEREALS','谷薯类',NULL,1,1,NULL,'包括大米、面粉、杂粮、薯类等主食',NOW(),NOW()),
(2,'VEGETABLES','蔬菜类',NULL,1,2,NULL,'各种新鲜蔬菜、菌菇类',NOW(),NOW()),
(3,'FRUITS','水果类',NULL,1,3,NULL,'各种新鲜水果、干果',NOW(),NOW()),
(4,'MEAT','畜肉类',NULL,1,4,NULL,'猪肉、牛肉、羊肉等',NOW(),NOW()),
(5,'POULTRY','禽肉类',NULL,1,5,NULL,'鸡肉、鸭肉等禽类',NOW(),NOW()),
(6,'SEAFOOD','水产类',NULL,1,6,NULL,'鱼类、虾类、贝类等',NOW(),NOW()),
(7,'EGGS','蛋类',NULL,1,7,NULL,'鸡蛋、鸭蛋等',NOW(),NOW()),
(8,'DAIRY','奶类',NULL,1,8,NULL,'牛奶、酸奶、奶酪等',NOW(),NOW()),
(9,'BEANS','豆类',NULL,1,9,NULL,'大豆、豆制品等',NOW(),NOW()),
(10,'NUTS','坚果类',NULL,1,10,NULL,'核桃、杏仁、花生等',NOW(),NOW()),
(11,'OILS','油脂类',NULL,1,11,NULL,'食用油、黄油等',NOW(),NOW()),
(12,'BEVERAGES','饮品类',NULL,1,12,NULL,'茶、咖啡、果汁等',NOW(),NOW()),
(13,'CONDIMENTS','调味品',NULL,1,13,NULL,'盐、酱油、香料等',NOW(),NOW()),
(14,'PROCESSED','加工食品',NULL,1,14,NULL,'罐头、腌制品等',NOW(),NOW()),
(15,'SNACKS','零食类',NULL,1,15,NULL,'饼干、糖果、膨化食品等',NOW(),NOW()),
(16,'RICE','米类',1,2,1,NULL,'大米、糙米、黑米等',NOW(),NOW()),
(17,'FLOUR','面类',1,2,2,NULL,'面粉、面条、面包等',NOW(),NOW()),
(18,'GRAINS','杂粮',1,2,3,NULL,'小米、玉米、燕麦等',NOW(),NOW()),
(19,'POTATOES','薯类',1,2,4,NULL,'土豆、红薯、山药等',NOW(),NOW()),
(20,'LEAFY_VEGETABLES','叶菜类',2,2,1,NULL,'白菜、菠菜、生菜等',NOW(),NOW()),
(21,'ROOT_VEGETABLES','根茎类',2,2,2,NULL,'萝卜、胡萝卜、莲藕等',NOW(),NOW()),
(22,'FRUIT_VEGETABLES','瓜果类',2,2,3,NULL,'番茄、黄瓜、茄子等',NOW(),NOW()),
(23,'MUSHROOMS','菌菇类',2,2,4,NULL,'香菇、平菇、木耳等',NOW(),NOW()),
(24,'BEAN_VEGETABLES','豆类蔬菜',2,2,5,NULL,'豆角、豌豆、毛豆等',NOW(),NOW()),
(25,'CITRUS','柑橘类',3,2,1,NULL,'橙子、柚子、柠檬等',NOW(),NOW()),
(26,'BERRIES','浆果类',3,2,2,NULL,'草莓、蓝莓、葡萄等',NOW(),NOW()),
(27,'TROPICAL','热带水果',3,2,3,NULL,'香蕉、芒果、菠萝等',NOW(),NOW()),
(28,'STONE_FRUITS','核果类',3,2,4,NULL,'桃子、李子、樱桃等',NOW(),NOW()),
(29,'POME_FRUITS','仁果类',3,2,5,NULL,'苹果、梨等',NOW(),NOW()),
(30,'MELON','瓜类',3,2,6,NULL,'西瓜、哈密瓜等',NOW(),NOW()),
(31,'PORK','猪肉',4,2,1,NULL,'猪肉及制品',NOW(),NOW()),
(32,'BEEF','牛肉',4,2,2,NULL,'牛肉及制品',NOW(),NOW()),
(33,'LAMB','羊肉',4,2,3,NULL,'羊肉及制品',NOW(),NOW()),
(34,'CHICKEN','鸡肉',5,2,1,NULL,'鸡肉及制品',NOW(),NOW()),
(35,'DUCK','鸭肉',5,2,2,NULL,'鸭肉及制品',NOW(),NOW()),
(36,'OTHER_POULTRY','其他禽类',5,2,3,NULL,'鹅肉、鸽肉等',NOW(),NOW()),
(37,'FISH','鱼类',6,2,1,NULL,'各种淡水鱼、海水鱼',NOW(),NOW()),
(38,'SHRIMP','虾类',6,2,2,NULL,'对虾、基围虾等',NOW(),NOW()),
(39,'CRAB','蟹类',6,2,3,NULL,'梭子蟹、大闸蟹等',NOW(),NOW()),
(40,'SHELLFISH','贝类',6,2,4,NULL,'蛤蜊、扇贝等',NOW(),NOW()),
(41,'SEAWEED','海藻类',6,2,5,NULL,'海带、紫菜等',NOW(),NOW()),
(42,'SOYBEANS','大豆类',9,2,1,NULL,'黄豆、黑豆等',NOW(),NOW()),
(43,'TOFU','豆制品',9,2,2,NULL,'豆腐、豆浆、豆干等',NOW(),NOW()),
(44,'OTHER_BEANS','其他豆类',9,2,3,NULL,'绿豆、红豆等',NOW(),NOW()),
(45,'MILK','鲜奶',8,2,1,NULL,'纯牛奶、脱脂奶等',NOW(),NOW()),
(46,'YOGURT','酸奶',8,2,2,NULL,'各种酸奶、乳酸菌饮料',NOW(),NOW()),
(47,'CHEESE','奶酪',8,2,3,NULL,'各种奶酪制品',NOW(),NOW()),
(48,'TREE_NUTS','树坚果',10,2,1,NULL,'核桃、杏仁、腰果等',NOW(),NOW()),
(49,'PEANUTS','花生',10,2,2,NULL,'花生及花生制品',NOW(),NOW()),
(50,'SEEDS','种子类',10,2,3,NULL,'瓜子、芝麻等',NOW(),NOW());
UNLOCK TABLES;

-- ------------------------------------------------------------------
-- 食材营养数据（中国食物成分表参考数据）
-- ------------------------------------------------------------------
LOCK TABLES `food_nutrition` WRITE;
INSERT INTO `food_nutrition` VALUES
(1,'RICE_001','大米','Rice',16,100.00,346.00,7.40,0.80,77.90,0.70,0.00,0.11,0.05,0.00,0.00,13.00,110.00,0.00,0.00,0.00,1.30,0.00,0.00,0.00,13.30,NULL,'四季',NULL,NULL,'提供能量，易消化吸收','一般人群',NULL,'米饭,白米,粳米','dami','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(2,'RICE_002','糙米','Brown Rice',16,100.00,348.00,7.70,2.70,77.40,3.90,0.00,0.41,0.10,0.00,0.00,14.00,297.00,0.00,0.00,0.00,2.30,0.00,0.00,0.00,12.50,NULL,'四季',NULL,NULL,'富含膳食纤维，有助于控制血糖','糖尿病患者,减肥人群',NULL,'粗米,全米','zaomi','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(3,'FLOUR_001','小麦粉（标准粉）','Wheat Flour',17,100.00,349.00,11.20,1.50,74.30,2.00,0.00,0.28,0.08,0.00,0.00,31.00,188.00,0.00,0.00,0.00,3.50,0.00,0.00,0.00,12.00,NULL,'四季',NULL,NULL,'提供能量和蛋白质','一般人群',NULL,'面粉,白面','xiaomaifeng','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(4,'GRAINS_001','燕麦','Oats',18,100.00,367.00,15.00,6.70,66.90,5.30,0.00,0.30,0.14,0.00,0.00,186.00,291.00,0.00,0.00,0.00,7.00,0.00,0.00,0.00,10.00,NULL,'四季',NULL,NULL,'降低胆固醇，稳定血糖，富含β-葡聚糖','心血管疾病患者,糖尿病患者',NULL,'麦片,燕麦片','yanmai','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(5,'GRAINS_002','玉米（鲜）','Sweet Corn',18,100.00,106.00,4.00,1.20,22.80,2.90,0.00,0.16,0.11,0.00,0.00,5.00,117.00,0.00,0.00,0.00,1.10,0.00,0.00,0.00,69.60,NULL,'夏秋',NULL,NULL,'富含膳食纤维和维生素E','一般人群',NULL,'嫩玉米,甜玉米','yumi','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(6,'POTATOES_001','红薯','Sweet Potato',19,100.00,99.00,1.10,0.20,24.70,1.60,0.00,0.04,0.04,0.00,0.00,18.00,39.00,0.00,0.00,0.00,0.50,0.00,0.00,0.00,72.80,NULL,'秋冬',NULL,NULL,'富含膳食纤维和β-胡萝卜素，促进肠道蠕动','一般人群,便秘患者',NULL,'地瓜,山芋,番薯','hongshu','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(7,'POTATOES_002','土豆','Potato',19,100.00,76.00,2.00,0.20,17.20,0.70,0.00,0.08,0.04,0.00,0.00,8.00,40.00,0.00,0.00,0.00,0.80,0.00,0.00,0.00,78.60,NULL,'四季',NULL,NULL,'提供能量，富含钾','一般人群',NULL,'马铃薯,洋芋','tudou','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(8,'VEG_001','大白菜','Chinese Cabbage',20,100.00,15.00,1.50,0.20,3.20,1.00,20.00,0.00,0.00,31.00,0.00,50.00,0.00,130.00,0.00,0.00,0.70,0.00,0.00,0.00,94.70,NULL,'冬春',NULL,NULL,'低热量，富含维生素C和膳食纤维','一般人群,减肥人群',NULL,'白菜,包菜','dabaicai','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(9,'VEG_002','菠菜','Spinach',20,100.00,24.00,2.60,0.30,4.50,1.70,487.00,0.00,0.00,32.00,0.00,66.00,0.00,311.00,0.00,0.00,2.90,0.00,0.00,0.00,91.20,NULL,'春秋',NULL,NULL,'富含铁质和叶酸，补血佳品','贫血患者,孕妇',NULL,'菠菜,波斯菜','bocai','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(10,'VEG_003','西红柿','Tomato',22,100.00,19.00,0.90,0.20,4.00,0.50,92.00,0.00,0.00,19.00,0.00,10.00,0.00,163.00,0.00,0.00,0.50,0.00,0.00,0.00,94.00,NULL,'夏秋',NULL,NULL,'富含番茄红素，抗氧化','一般人群',NULL,'番茄,洋柿子','xihongshi','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(11,'VEG_004','黄瓜','Cucumber',22,100.00,15.00,0.80,0.20,2.90,0.50,15.00,0.00,0.00,9.00,0.00,24.00,0.00,102.00,0.00,0.00,0.50,0.00,0.00,0.00,95.80,NULL,'夏',NULL,NULL,'清热解毒，补水利尿','一般人群,减肥人群',NULL,'青瓜','huanggua','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(12,'VEG_005','胡萝卜','Carrot',21,100.00,37.00,1.00,0.20,8.80,3.20,688.00,0.00,0.00,13.00,0.00,32.00,0.00,190.00,0.00,0.00,1.00,0.00,0.00,0.00,89.00,NULL,'秋冬',NULL,NULL,'富含β-胡萝卜素，保护视力','一般人群',NULL,'红萝卜','huluobo','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(13,'VEG_006','西兰花','Broccoli',22,100.00,36.00,4.10,0.60,6.60,3.60,7210.00,0.00,0.00,51.00,0.00,67.00,0.00,340.00,0.00,0.00,1.00,0.00,0.00,0.00,89.20,NULL,'秋冬春',NULL,NULL,'富含维生素C和K，抗癌佳品','一般人群',NULL,'绿花菜,青花菜','xilanhua','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(14,'FRUIT_001','苹果','Apple',29,100.00,53.00,0.20,0.20,13.70,1.20,0.00,0.00,0.00,4.00,0.00,4.00,0.00,119.00,0.00,0.00,0.00,0.00,0.00,0.00,85.00,NULL,'秋',NULL,NULL,'富含膳食纤维和维生素C','一般人群',NULL,'平果','pingguo','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(15,'FRUIT_002','香蕉','Banana',27,100.00,91.00,1.40,0.20,22.00,1.20,0.00,0.00,0.00,8.00,0.00,7.00,0.00,256.00,0.00,0.00,0.00,0.00,0.00,0.00,75.00,NULL,'四季',NULL,NULL,'快速补充能量，富含钾','一般人群,运动人群',NULL,'芭蕉','xiangjiao','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(16,'FRUIT_003','橙子','Orange',25,100.00,48.00,0.80,0.20,11.10,0.60,0.00,0.00,0.00,33.00,0.00,20.00,0.00,159.00,0.00,0.00,0.00,0.00,0.00,0.00,87.00,NULL,'冬春',NULL,NULL,'富含维生素C，增强免疫力','一般人群',NULL,'甜橙,柑橘','chengzi','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(17,'FRUIT_004','草莓','Strawberry',26,100.00,32.00,1.00,0.20,7.10,1.10,0.00,0.00,0.00,47.00,0.00,18.00,0.00,131.00,0.00,0.00,0.00,0.00,0.00,0.00,91.00,NULL,'春',NULL,NULL,'富含维生素C和抗氧化物','一般人群',NULL,'红莓','caomei','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(18,'FRUIT_005','西瓜','Watermelon',30,100.00,31.00,0.60,0.10,7.90,0.30,0.00,0.00,0.00,6.00,0.00,8.00,0.00,87.00,0.00,0.00,0.00,0.00,0.00,0.00,91.00,NULL,'夏',NULL,NULL,'补水解暑，利尿','一般人群',NULL,'寒瓜','xigua','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(19,'CHICKEN_001','鸡胸肉','Chicken Breast',34,100.00,133.00,19.40,5.00,2.50,0.00,0.00,0.05,0.09,0.00,0.00,9.00,156.00,0.00,0.00,0.00,1.30,1.09,0.00,58.00,72.00,NULL,NULL,NULL,NULL,'高蛋白低脂肪，健身佳品','一般人群,健身人群','痛风患者应适量','鸡脯肉','jixiongrou','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(20,'PORK_001','猪瘦肉','Lean Pork',31,100.00,143.00,20.30,6.20,1.50,0.00,0.00,0.54,0.16,0.00,0.00,6.00,162.00,0.00,0.00,0.00,3.00,2.88,0.00,81.00,70.00,NULL,NULL,NULL,NULL,'富含优质蛋白和铁','一般人群','高血脂患者应少食','猪里脊','zhushourou','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(21,'BEEF_001','牛肉','Beef',32,100.00,125.00,20.10,4.20,2.00,0.00,0.00,0.07,0.18,0.00,0.00,9.00,172.00,0.00,0.00,0.00,2.80,4.73,0.00,58.00,72.60,NULL,NULL,NULL,NULL,'富含优质蛋白和铁，补血佳品','贫血患者,健身人群','痛风患者应少食','牛瘦肉','niurou','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(22,'FISH_001','鲈鱼','Sea Bass',37,100.00,105.00,18.60,3.40,0.00,0.00,19.00,0.00,0.00,0.00,0.75,138.00,242.00,205.00,0.00,0.00,0.00,0.00,33.10,86.00,77.00,NULL,NULL,NULL,NULL,'富含DHA和EPA，有益心脑血管','一般人群,孕妇',NULL,'花鲈,七星鲈','luyu','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(23,'SHRIMP_001','虾','Shrimp',38,100.00,93.00,18.60,1.00,2.80,0.00,15.00,0.00,0.00,0.00,0.62,62.00,228.00,215.00,0.00,0.00,0.00,0.00,29.70,193.00,76.00,NULL,NULL,NULL,NULL,'高蛋白低脂肪，富含矿物质','一般人群',NULL,'对虾,明虾','xia','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(24,'TOFU_001','豆腐','Tofu',43,100.00,81.00,8.10,3.70,4.20,0.40,0.00,0.00,0.00,0.00,0.00,164.00,119.00,125.00,0.00,0.00,1.90,0.00,0.00,0.00,82.80,NULL,NULL,NULL,NULL,'富含优质植物蛋白和钙','一般人群,素食者',NULL,'黄豆腐','doufu','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(25,'SOYBEANS_001','黄豆','Soybean',42,100.00,359.00,35.00,16.00,34.20,15.50,0.00,0.00,0.00,0.00,0.00,191.00,465.00,1503.00,0.00,0.00,8.20,0.00,0.00,0.00,10.00,NULL,NULL,NULL,NULL,'富含蛋白质和异黄酮','一般人群',NULL,'大豆','huangdou','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(26,'NUTS_001','核桃','Walnut',48,100.00,646.00,14.90,58.80,19.10,9.50,0.00,0.00,0.00,0.00,43.21,56.00,0.00,0.00,0.00,131.00,0.00,3.12,0.00,0.00,3.50,NULL,NULL,NULL,NULL,'富含不饱和脂肪酸，健脑益智','一般人群','肥胖者应少食','胡桃','hetao','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(27,'NUTS_002','杏仁','Almond',48,100.00,578.00,21.30,50.60,20.50,11.80,0.00,0.00,0.00,0.00,25.63,248.00,0.00,0.00,0.00,275.00,0.00,3.36,0.00,0.00,4.70,NULL,NULL,NULL,NULL,'富含维生素E，抗氧化','一般人群','肥胖者应少食','巴旦木','xingren','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(28,'EGG_001','鸡蛋','Egg',7,100.00,147.00,13.30,8.80,2.80,0.00,234.00,0.00,0.27,0.00,0.00,56.00,130.00,0.00,0.00,0.00,2.00,0.00,0.00,585.00,71.50,NULL,NULL,NULL,NULL,'营养全面，蛋白质优质','一般人群','高胆固醇患者应适量','鸡子','jidan','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(29,'MILK_001','纯牛奶','Milk',45,100.00,54.00,3.00,3.20,3.40,0.00,24.00,0.00,0.14,0.00,0.00,104.00,73.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,87.50,NULL,NULL,NULL,NULL,'富含优质蛋白和钙','一般人群,儿童,老人',NULL,'鲜奶','chuniunai','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW()),
(30,'YOGURT_001','酸奶','Yogurt',46,100.00,72.00,2.50,2.70,9.30,0.00,26.00,0.00,0.15,0.00,0.00,118.00,85.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,84.40,NULL,NULL,NULL,NULL,'促进消化，富含益生菌','一般人群,便秘患者',NULL,'乳酪,优格','suannai','中国食物成分表',NULL,'ACTIVE',1,NOW(),NOW());
UNLOCK TABLES;

-- ------------------------------------------------------------------
-- 食物营养数据（简化版，前端展示用）
-- ------------------------------------------------------------------
LOCK TABLES `nutrition_data` WRITE;
INSERT INTO `nutrition_data` VALUES
(1,'米饭','Rice','谷物类','100g',116.00,2.60,25.90,0.30,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'manual',NOW(),NOW()),
(2,'全麦面包','Whole Wheat Bread','谷物类','100g',247.00,13.20,41.30,3.40,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'manual',NOW(),NOW()),
(3,'鸡胸肉','Chicken Breast','肉类','100g',165.00,31.00,0.00,3.60,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'manual',NOW(),NOW()),
(4,'牛肉','Beef','肉类','100g',250.00,26.00,0.00,15.00,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'manual',NOW(),NOW()),
(5,'三文鱼','Salmon','水产类','100g',208.00,20.00,0.00,13.00,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'manual',NOW(),NOW()),
(6,'鸡蛋','Egg','蛋奶类','1个（50g）',72.00,6.30,0.40,4.80,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'manual',NOW(),NOW()),
(7,'牛奶','Milk','蛋奶类','100ml',54.00,3.00,5.00,3.20,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'manual',NOW(),NOW()),
(8,'西蓝花','Broccoli','蔬菜类','100g',34.00,2.80,7.00,0.40,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'manual',NOW(),NOW()),
(9,'苹果','Apple','水果类','100g',52.00,0.30,14.00,0.20,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'manual',NOW(),NOW()),
(10,'香蕉','Banana','水果类','100g',89.00,1.10,23.00,0.30,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'manual',NOW(),NOW()),
(11,'豆腐','Tofu','豆制品','100g',76.00,8.10,1.90,4.20,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'manual',NOW(),NOW()),
(12,'核桃','Walnut','坚果类','100g',654.00,15.20,13.70,65.20,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'manual',NOW(),NOW());
UNLOCK TABLES;

-- ------------------------------------------------------------------
-- 系统配置（管理后台核心配置）
-- ------------------------------------------------------------------
LOCK TABLES `system_config` WRITE;
INSERT INTO `system_config` VALUES
(1,'ai.default_model','qwen-max','string','默认AI模型','ai',1,NOW(),NOW()),
(2,'ai.max_tokens','1000','number','AI响应的最大Token数量','ai',1,NOW(),NOW()),
(3,'ai.temperature','0.7','number','控制AI响应的随机性，范围0-1','ai',1,NOW(),NOW()),
(4,'system.site_name','NutriAI健康饮食规划助手','string','系统显示的网站名称','system',1,NOW(),NOW()),
(5,'system.maintenance_mode','false','boolean','是否开启系统维护模式','system',1,NOW(),NOW()),
(6,'member.free_chat_limit','10','number','免费用户每日对话限制','member',0,NOW(),NOW()),
(7,'member.bronze_chat_limit','50','number','青铜会员每日对话限制','member',0,NOW(),NOW()),
(8,'member.silver_chat_limit','200','number','白银会员每日对话限制','member',0,NOW(),NOW()),
(9,'member.gold_chat_limit','-1','number','黄金会员每日对话限制（-1表示无限制）','member',0,NOW(),NOW()),
(12,'system.site_description','智能营养分析 · 个性化饮食方案 · 健康管理','string','网站的简短描述','系统',1,NOW(),NOW()),
(13,'system.contact_email','support@nutriai.com','string','客服联系邮箱','系统',1,NOW(),NOW()),
(14,'system.support_phone','400-123-4567','string','客服联系电话','系统',1,NOW(),NOW()),
(15,'system.copyright_text','© 2026 NutriAI健康饮食规划助手. All rights reserved.','string','页脚显示的版权信息','系统',1,NOW(),NOW()),
(16,'system.icp_number','','string','网站ICP备案号','系统',1,NOW(),NOW()),
(18,'system.max_upload_size','10','number','文件上传的最大大小（MB）','系统',1,NOW(),NOW()),
(19,'system.enable_registration','true','string','是否允许新用户注册','系统',1,NOW(),NOW()),
(20,'ai.model','qwen-max','string','使用的AI模型名称','AI',0,NOW(),NOW()),
(23,'user.default_member_level','FREE','string','新用户注册时的默认会员等级','用户',0,NOW(),NOW()),
(24,'user.max_chat_history','100','number','用户可保存的最大对话历史数量','用户',0,NOW(),NOW()),
(25,'user.session_timeout','30','number','用户会话超时时间（分钟）','用户',0,NOW(),NOW()),
(26,'user.daily_ai_calls_limit','20','number','免费用户每日AI调用次数限制','用户',1,NOW(),NOW()),
(27,'user.enable_email_verification','false','string','是否要求用户验证邮箱','用户',0,NOW(),NOW()),
(28,'security.password_min_length','8','number','用户密码的最小长度','安全',0,NOW(),NOW()),
(29,'security.max_login_attempts','5','number','允许的最大登录失败次数','安全',0,NOW(),NOW()),
(30,'security.enable_captcha','false','string','是否启用验证码','安全',0,NOW(),NOW()),
(31,'notification.email_enabled','false','string','是否启用邮件通知','通知',0,NOW(),NOW()),
(32,'notification.sms_enabled','false','string','是否启用短信通知','通知',0,NOW(),NOW());
UNLOCK TABLES;

-- ------------------------------------------------------------------
-- 系统配置（旧版，兼容）
-- ------------------------------------------------------------------
LOCK TABLES `system_configs` WRITE;
INSERT INTO `system_configs` VALUES
(1,'system.name','NutriAI健康饮食规划助手','string','系统名称',1,NOW(),NOW()),
(2,'system.version','1.0.0','string','系统版本',1,NOW(),NOW()),
(3,'ai.daily_free_quota','3','number','普通用户每日AI咨询次数',1,NOW(),NOW()),
(4,'ai.silver_quota','10','number','白银会员每日AI咨询次数',1,NOW(),NOW()),
(5,'ai.gold_quota','20','number','黄金会员每日AI咨询次数',1,NOW(),NOW()),
(6,'growth.upgrade_silver','100','number','升级白银会员所需成长值',1,NOW(),NOW()),
(7,'growth.upgrade_gold','300','number','升级黄金会员所需成长值',1,NOW(),NOW());
UNLOCK TABLES;

-- ------------------------------------------------------------------
-- VIP套餐配置
-- ------------------------------------------------------------------
LOCK TABLES `vip_plans` WRITE;
INSERT INTO `vip_plans`
  (plan_code,plan_name,description,original_price,discount_price,duration_days,
   benefits,bonus_growth,is_active,sort_order,badge)
VALUES
('MONTHLY','营养月卡',
 '解锁AI营养师无限咨询，每日个性化营养分析，开启你的健康管理之旅',
 29.90,19.90,30,
 '{"maxAiQueries":-1,"maxFoodRecords":-1,"features":["AI营养咨询不限次","每日营养摄入报告","高级数据分析","个性化饮食建议","优先客服支持"],"level":"VIP"}',
 100,1,1,'热门'),
('QUARTERLY','营养季卡',
 '三个月深度健康管理，AI食谱定制、营养趋势分析一应俱全，比月卡节省44%',
 89.70,49.90,90,
 '{"maxAiQueries":-1,"maxFoodRecords":-1,"features":["所有月卡权益","AI定制食谱每日推荐","营养趋势深度分析","体重管理追踪","季度营养健康报告","专属健康顾问"],"level":"VIP_PLUS"}',
 300,1,2,'推荐'),
('YEARLY','营养年卡',
 '一年贴身AI营养师，无限咨询、真人营养顾问、线下健康活动全部解锁，最高性价比',
 358.80,168.00,365,
 '{"maxAiQueries":-1,"maxFoodRecords":-1,"features":["所有季卡权益","专属营养师线上指导","合作健康品牌9折优惠","线下健康活动参与资格","年度体检报告解读","成长值额外加成20%"],"level":"VIP_PRO"}',
 1000,1,3,'超值');
UNLOCK TABLES;

-- ------------------------------------------------------------------
-- 系统公告（初始欢迎通知）
-- ------------------------------------------------------------------
LOCK TABLES `system_announcement` WRITE;
INSERT INTO `system_announcement`
  (id,title,content,type,priority,is_active,start_time,end_time,created_by,created_at,updated_at)
VALUES
  (1,'欢迎使用NutriAI健康饮食规划助手',
   '感谢您使用 NutriAI！我们基于AI技术提供个性化健康饮食规划服务。如有任何问题，请随时联系我们的客服团队。',
   'info',1,1,NOW(),NULL,1,NOW(),NOW());
UNLOCK TABLES;

-- ------------------------------------------------------------------
-- Flyway 迁移历史（基线标记，Flyway 在生产环境已禁用，仅作记录）
-- ------------------------------------------------------------------
LOCK TABLES `flyway_schema_history` WRITE;
INSERT INTO `flyway_schema_history` VALUES
(1,'1','<< Flyway Baseline >>','BASELINE','<< Flyway Baseline >>',NULL,'init',NOW(),0,1),
(2,'5','Create Food Records Table','SQL','V5__Create_Food_Records_Table.sql',-1601675978,'init',NOW(),14,1),
(3,'6','Create Member Tables','SQL','V6__Create_Member_Tables.sql',NULL,'init',NOW(),0,1),
(4,'7','create food tables','SQL','V7__create_food_tables.sql',889647335,'init',NOW(),23,1),
(5,'8','init food categories','SQL','V8__init_food_categories.sql',-455914640,'init',NOW(),31,1),
(6,'9','init food data','SQL','V9__init_food_data.sql',1883822887,'init',NOW(),19,1),
(7,'10','update food data','SQL','V10__update_food_data.sql',1551254713,'init',NOW(),37,1),
(8,'11','Create food recognition history','SQL','V11__Create_food_recognition_history.sql',1782438989,'init',NOW(),29,1),
(9,'12','create task and history tables','SQL','V12__create_task_and_history_tables.sql',-584251532,'init',NOW(),24,1),
(10,'13','Create ai chat tables','SQL','V13__Create_ai_chat_tables.sql',29573929,'init',NOW(),20,1),
(11,'14','Create admin tables','SQL','V14__Create_admin_tables.sql',NULL,'init',NOW(),0,1),
(12,'15','create health profiles','SQL','V15__create_health_profiles.sql',NULL,'init',NOW(),0,1),
(13,'16','add is favorite to diet plan history','SQL','V16__add_is_favorite_to_diet_plan_history.sql',NULL,'init',NOW(),0,1),
(14,'17','add vip membership','SQL','V17__add_vip_membership.sql',NULL,'init',NOW(),0,1);
UNLOCK TABLES;

-- ====================================================================
-- 第四步：创建视图
-- ====================================================================

CREATE OR REPLACE VIEW `v_food_category_stats` AS
SELECT
  fc.`id`,
  fc.`category_code`,
  fc.`category_name`,
  fc.`level`,
  COUNT(fn.`id`) AS `food_count`
FROM `food_category` fc
LEFT JOIN `food_nutrition` fn ON fn.`category_id` = fc.`id`
GROUP BY fc.`id`, fc.`category_code`, fc.`category_name`, fc.`level`;

-- ====================================================================
-- 恢复原始设置
-- ====================================================================
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- ====================================================================
-- 初始化完成
-- 下一步:
--   1. 登录后台 (admin / 初始密码同开发环境)，立即修改管理员密码
--   2. 在系统配置中更新 system.site_name、联系方式等信息
--   3. 配置 TONGYI_API_KEY 等第三方服务凭证到 .env 文件
-- ====================================================================
