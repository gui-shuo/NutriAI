-- 营养餐订单增加门店关联字段
ALTER TABLE meal_orders ADD COLUMN merchant_id BIGINT DEFAULT NULL COMMENT '门店ID' AFTER user_id;
ALTER TABLE meal_orders ADD COLUMN merchant_name VARCHAR(200) DEFAULT NULL COMMENT '门店名称(冗余)' AFTER merchant_id;
ALTER TABLE meal_orders ADD INDEX idx_meal_order_merchant (merchant_id);

-- 确保merchants表有坐标索引，加速距离查询
ALTER TABLE merchants ADD INDEX idx_merchant_lat_lng (latitude, longitude);

-- 插入示例门店数据（含经纬度，用于测试距离排序）
INSERT INTO merchants (name, logo, phone, address, latitude, longitude, description, business_hours, status, type, sort_order, created_at, updated_at) VALUES
('NutriAI 旗舰店', NULL, '400-888-0001', '北京市海淀区中关村大街1号', 39.984120, 116.307490, '旗舰总店，全品类营养餐供应', '08:00-21:00', 'ACTIVE', 'RESTAURANT', 1, NOW(), NOW()),
('NutriAI 望京店', NULL, '400-888-0002', '北京市朝阳区望京SOHO B座1层', 39.998860, 116.474780, '望京地区核心门店', '09:00-20:00', 'ACTIVE', 'RESTAURANT', 2, NOW(), NOW()),
('NutriAI 国贸店', NULL, '400-888-0003', '北京市朝阳区建国门外大街甲6号', 39.908720, 116.459380, '国贸CBD白领营养餐', '07:30-20:30', 'ACTIVE', 'RESTAURANT', 3, NOW(), NOW()),
('NutriAI 西单店', NULL, '400-888-0004', '北京市西城区西单北大街180号', 39.912680, 116.377010, '西单商圈便捷取餐', '08:30-21:00', 'ACTIVE', 'RESTAURANT', 4, NOW(), NOW()),
('NutriAI 五道口店', NULL, '400-888-0005', '北京市海淀区成府路28号', 39.992540, 116.338390, '学生与白领的健康选择', '08:00-22:00', 'ACTIVE', 'RESTAURANT', 5, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);
