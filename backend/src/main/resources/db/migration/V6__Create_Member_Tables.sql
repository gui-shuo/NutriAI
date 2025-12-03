-- Sprint 6: 会员系统数据库表
-- 创建时间: 2025-12-03

-- 1. 会员等级配置表
CREATE TABLE IF NOT EXISTS member_levels (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '等级ID',
    level_code VARCHAR(20) NOT NULL UNIQUE COMMENT '等级代码',
    level_name VARCHAR(50) NOT NULL COMMENT '等级名称',
    level_order INT NOT NULL COMMENT '等级顺序',
    growth_required INT NOT NULL COMMENT '所需成长值',
    benefits JSON COMMENT '权益配置',
    icon_url VARCHAR(255) COMMENT '等级图标',
    color VARCHAR(20) COMMENT '等级颜色',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '会员等级配置表';

-- 2. 会员信息表
CREATE TABLE IF NOT EXISTS members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会员ID',
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    level_id BIGINT NOT NULL COMMENT '当前等级ID',
    total_growth INT DEFAULT 0 COMMENT '总成长值',
    current_growth INT DEFAULT 0 COMMENT '当前等级成长值',
    invitation_code VARCHAR(20) NOT NULL UNIQUE COMMENT '邀请码',
    invited_by BIGINT COMMENT '邀请人ID',
    invitation_count INT DEFAULT 0 COMMENT '邀请人数',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    activated_at TIMESTAMP COMMENT '激活时间',
    expire_at TIMESTAMP COMMENT '过期时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (level_id) REFERENCES member_levels(id),
    FOREIGN KEY (invited_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_invitation_code (invitation_code),
    INDEX idx_invited_by (invited_by)
) COMMENT '会员信息表';

-- 3. 成长值记录表
CREATE TABLE IF NOT EXISTS growth_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    member_id BIGINT NOT NULL COMMENT '会员ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    growth_value INT NOT NULL COMMENT '成长值',
    growth_type VARCHAR(50) NOT NULL COMMENT '成长值类型',
    description VARCHAR(255) COMMENT '描述',
    related_id BIGINT COMMENT '关联业务ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_member_id (member_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_growth_type (growth_type)
) COMMENT '成长值记录表';

-- 4. 邀请记录表
CREATE TABLE IF NOT EXISTS invitations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '邀请ID',
    inviter_id BIGINT NOT NULL COMMENT '邀请人ID',
    invitee_id BIGINT COMMENT '被邀请人ID',
    invitation_code VARCHAR(20) NOT NULL COMMENT '邀请码',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING, ACCEPTED, EXPIRED',
    invited_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '邀请时间',
    accepted_at TIMESTAMP COMMENT '接受时间',
    reward_growth INT DEFAULT 0 COMMENT '奖励成长值',
    is_rewarded BOOLEAN DEFAULT FALSE COMMENT '是否已发放奖励',
    FOREIGN KEY (inviter_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (invitee_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_inviter_id (inviter_id),
    INDEX idx_invitee_id (invitee_id),
    INDEX idx_invitation_code (invitation_code),
    INDEX idx_status (status)
) COMMENT '邀请记录表';

-- 初始化会员等级数据
INSERT INTO member_levels (level_code, level_name, level_order, growth_required, benefits, icon_url, color) VALUES
('ROOKIE', '新手会员', 1, 0, 
 '{"features": ["基础营养记录", "AI咨询(3次/天)", "基础数据分析"], "maxAiQueries": 3, "maxFoodRecords": 10}',
 '/icons/level-rookie.png', '#95a5a6'),
('BRONZE', '青铜会员', 2, 100,
 '{"features": ["营养记录无限", "AI咨询(10次/天)", "高级数据分析", "自定义目标"], "maxAiQueries": 10, "maxFoodRecords": -1}',
 '/icons/level-bronze.png', '#cd7f32'),
('SILVER', '白银会员', 3, 500,
 '{"features": ["所有青铜权益", "AI咨询(30次/天)", "专属营养报告", "优先客服"], "maxAiQueries": 30, "maxFoodRecords": -1}',
 '/icons/level-silver.png', '#c0c0c0'),
('GOLD', '黄金会员', 4, 2000,
 '{"features": ["所有白银权益", "AI咨询无限", "个性化食谱", "健康顾问", "数据导出"], "maxAiQueries": -1, "maxFoodRecords": -1}',
 '/icons/level-gold.png', '#ffd700'),
('PLATINUM', '铂金会员', 5, 5000,
 '{"features": ["所有黄金权益", "专属营养师", "线下活动", "合作商家折扣", "终身成长值加成"], "maxAiQueries": -1, "maxFoodRecords": -1, "growthBonus": 1.5}',
 '/icons/level-platinum.png', '#e5e4e2');

-- 为现有用户创建会员信息（默认为新手会员）
INSERT INTO members (user_id, level_id, invitation_code)
SELECT 
    u.id,
    (SELECT id FROM member_levels WHERE level_code = 'ROOKIE' LIMIT 1),
    CONCAT('INV', LPAD(u.id, 6, '0'), SUBSTRING(MD5(CONCAT(u.id, NOW())), 1, 6))
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM members m WHERE m.user_id = u.id);
