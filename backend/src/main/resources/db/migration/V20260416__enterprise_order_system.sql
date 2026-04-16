-- ============================================================
-- 企业级订单系统扩展 V20260416
-- ============================================================

-- 1. 购物车
CREATE TABLE IF NOT EXISTS cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    item_type VARCHAR(20) NOT NULL COMMENT 'MEAL=营养餐 PRODUCT=产品',
    item_id BIGINT NOT NULL COMMENT '商品ID',
    quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    selected TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否选中',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_item (user_id, item_type, item_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车';

-- 2. 优惠券模板
CREATE TABLE IF NOT EXISTS coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) COMMENT '优惠码(为空则管理员发放)',
    name VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    type VARCHAR(20) NOT NULL COMMENT 'REDUCE=满减 DISCOUNT=折扣',
    discount_value DECIMAL(10,2) NOT NULL COMMENT '减免金额或折扣率(0.8=8折)',
    min_order_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '最低订单金额',
    max_discount_amount DECIMAL(10,2) COMMENT '最高减免金额(折扣券)',
    applicable_type VARCHAR(20) NOT NULL DEFAULT 'ALL' COMMENT 'ALL=全场 MEAL=营养餐 PRODUCT=产品',
    total_count INT NOT NULL DEFAULT -1 COMMENT '总发行量(-1=不限)',
    used_count INT NOT NULL DEFAULT 0 COMMENT '已使用数量',
    per_user_limit INT NOT NULL DEFAULT 1 COMMENT '每人限领',
    valid_days INT NOT NULL DEFAULT 30 COMMENT '领取后有效天数',
    start_time DATETIME COMMENT '活动开始时间',
    end_time DATETIME COMMENT '活动结束时间',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    description VARCHAR(500) COMMENT '使用说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_code (code),
    INDEX idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券';

-- 3. 用户优惠券
CREATE TABLE IF NOT EXISTS user_coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    coupon_id BIGINT NOT NULL COMMENT '优惠券ID',
    status VARCHAR(20) NOT NULL DEFAULT 'UNUSED' COMMENT 'UNUSED=未使用 USED=已使用 EXPIRED=已过期',
    used_at DATETIME COMMENT '使用时间',
    used_order_no VARCHAR(32) COMMENT '使用的订单号',
    expire_at DATETIME NOT NULL COMMENT '过期时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_coupon (user_id, coupon_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_expire (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户优惠券';

-- 4. 退款申请
CREATE TABLE IF NOT EXISTS refund_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    refund_no VARCHAR(32) NOT NULL UNIQUE COMMENT '退款单号',
    order_no VARCHAR(32) NOT NULL COMMENT '原订单号',
    order_type VARCHAR(20) NOT NULL COMMENT 'MEAL/PRODUCT',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    refund_amount DECIMAL(10,2) NOT NULL COMMENT '退款金额',
    reason VARCHAR(500) NOT NULL COMMENT '退款原因',
    images JSON COMMENT '图片凭证',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING=待处理 APPROVED=已批准 REJECTED=已拒绝 COMPLETED=已完成',
    admin_remark VARCHAR(500) COMMENT '管理员备注',
    processed_by BIGINT COMMENT '处理人ID',
    processed_at DATETIME COMMENT '处理时间',
    completed_at DATETIME COMMENT '退款到账时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_no (order_no),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退款申请';

-- 5. 扩展 product_orders 表字段
ALTER TABLE product_orders
    ADD COLUMN IF NOT EXISTS coupon_id BIGINT COMMENT '使用的优惠券ID',
    ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    ADD COLUMN IF NOT EXISTS shipping_company VARCHAR(50) COMMENT '物流公司',
    ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(200) COMMENT '取消原因',
    ADD COLUMN IF NOT EXISTS cancelled_at DATETIME COMMENT '取消时间',
    ADD COLUMN IF NOT EXISTS auto_confirm_at DATETIME COMMENT '自动确认收货时间',
    ADD COLUMN IF NOT EXISTS fulfillment_type VARCHAR(20) NOT NULL DEFAULT 'EXPRESS' COMMENT 'EXPRESS=快递 PICKUP=自提',
    ADD COLUMN IF NOT EXISTS pickup_code VARCHAR(6) COMMENT '自提码';

-- 6. 扩展 meal_orders 表字段
ALTER TABLE meal_orders
    ADD COLUMN IF NOT EXISTS coupon_id BIGINT COMMENT '使用的优惠券ID',
    ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额';

-- 7. NutritionProduct 添加版本字段用于乐观锁
ALTER TABLE nutrition_products
    ADD COLUMN IF NOT EXISTS version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';

-- 8. meal_items 添加版本字段
ALTER TABLE meal_items
    ADD COLUMN IF NOT EXISTS version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';

-- 9. 插入示例优惠券
INSERT IGNORE INTO coupons (name, type, discount_value, min_order_amount, applicable_type, total_count, valid_days, description)
VALUES
('新用户专享5元券', 'REDUCE', 5.00, 20.00, 'ALL', 1000, 30, '新用户注册后领取，满20元可用'),
('营养餐8折券', 'DISCOUNT', 0.80, 30.00, 'MEAL', 500, 7, '营养餐满30元打8折，最高优惠20元'),
('产品满100减15', 'REDUCE', 15.00, 100.00, 'PRODUCT', 300, 14, '购买产品满100元减15元');

UPDATE coupons SET max_discount_amount = 20.00 WHERE type = 'DISCOUNT' AND applicable_type = 'MEAL';
