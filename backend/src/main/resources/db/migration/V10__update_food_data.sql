-- V10: 更新食材分类和营养数据
-- 这个脚本用于在V8和V9已执行的基础上进行数据更新
-- 创建时间: 2025-12-03

-- 1. 更新food_category表，添加IGNORE确保幂等性
-- 对于已存在的分类，更新其描述信息

-- 更新一级分类描述
UPDATE food_category SET description = '包括大米、面粉、杂粮、薯类等主食' WHERE category_code = 'CEREALS';
UPDATE food_category SET description = '各种新鲜蔬菜、菌菇类' WHERE category_code = 'VEGETABLES';
UPDATE food_category SET description = '各种新鲜水果、干果' WHERE category_code = 'FRUITS';
UPDATE food_category SET description = '猪肉、牛肉、羊肉等' WHERE category_code = 'MEAT';
UPDATE food_category SET description = '鸡肉、鸭肉等禽类' WHERE category_code = 'POULTRY';
UPDATE food_category SET description = '鱼类、虾类、贝类等' WHERE category_code = 'SEAFOOD';
UPDATE food_category SET description = '鸡蛋、鸭蛋等' WHERE category_code = 'EGGS';
UPDATE food_category SET description = '牛奶、酸奶、奶酪等' WHERE category_code = 'DAIRY';
UPDATE food_category SET description = '大豆、豆制品等' WHERE category_code = 'BEANS';
UPDATE food_category SET description = '核桃、杏仁、花生等' WHERE category_code = 'NUTS';
UPDATE food_category SET description = '食用油、黄油等' WHERE category_code = 'OILS';
UPDATE food_category SET description = '茶、咖啡、果汁等' WHERE category_code = 'BEVERAGES';
UPDATE food_category SET description = '盐、酱油、香料等' WHERE category_code = 'CONDIMENTS';
UPDATE food_category SET description = '罐头、腌制品等' WHERE category_code = 'PROCESSED';
UPDATE food_category SET description = '饼干、糖果、膨化食品等' WHERE category_code = 'SNACKS';

-- 2. 补充可能缺失的二级分类
-- 使用INSERT IGNORE确保不会因主键冲突而失败

-- 谷薯类二级分类
INSERT IGNORE INTO food_category (category_code, category_name, parent_id, level, sort_order, description) 
SELECT 'RICE', '米类', id, 2, 1, '大米、糙米、黑米等' FROM food_category WHERE category_code = 'CEREALS';

INSERT IGNORE INTO food_category (category_code, category_name, parent_id, level, sort_order, description) 
SELECT 'FLOUR', '面类', id, 2, 2, '面粉、面条、面包等' FROM food_category WHERE category_code = 'CEREALS';

INSERT IGNORE INTO food_category (category_code, category_name, parent_id, level, sort_order, description) 
SELECT 'GRAINS', '杂粮', id, 2, 3, '小米、玉米、燕麦等' FROM food_category WHERE category_code = 'CEREALS';

INSERT IGNORE INTO food_category (category_code, category_name, parent_id, level, sort_order, description) 
SELECT 'POTATOES', '薯类', id, 2, 4, '土豆、红薯、山药等' FROM food_category WHERE category_code = 'CEREALS';

-- 蔬菜类二级分类
INSERT IGNORE INTO food_category (category_code, category_name, parent_id, level, sort_order, description) 
SELECT 'LEAFY_VEGETABLES', '叶菜类', id, 2, 1, '白菜、菠菜、生菜等' FROM food_category WHERE category_code = 'VEGETABLES';

INSERT IGNORE INTO food_category (category_code, category_name, parent_id, level, sort_order, description) 
SELECT 'ROOT_VEGETABLES', '根茎类', id, 2, 2, '萝卜、胡萝卜、莲藕等' FROM food_category WHERE category_code = 'VEGETABLES';

INSERT IGNORE INTO food_category (category_code, category_name, parent_id, level, sort_order, description) 
SELECT 'FRUIT_VEGETABLES', '瓜果类', id, 2, 3, '番茄、黄瓜、茄子等' FROM food_category WHERE category_code = 'VEGETABLES';

INSERT IGNORE INTO food_category (category_code, category_name, parent_id, level, sort_order, description) 
SELECT 'MUSHROOMS', '菌菇类', id, 2, 4, '香菇、平菇、木耳等' FROM food_category WHERE category_code = 'VEGETABLES';

INSERT IGNORE INTO food_category (category_code, category_name, parent_id, level, sort_order, description) 
SELECT 'BEAN_VEGETABLES', '豆类蔬菜', id, 2, 5, '豆角、豌豆、毛豆等' FROM food_category WHERE category_code = 'VEGETABLES';

-- 3. 补充可能缺失的食材数据
-- 使用ON DUPLICATE KEY UPDATE确保幂等性

-- 谷薯类食材
INSERT INTO food_nutrition (
    food_code, food_name, food_name_en, category_id, 
    energy, protein, fat, carbohydrate, dietary_fiber,
    vitamin_b1, vitamin_b2, calcium, phosphorus, iron,
    water, search_keywords, pinyin, season, health_benefits,
    suitable_people, data_source, status, is_verified
) 
SELECT 
    'RICE_001', '大米', 'Rice', fc.id,
    346, 7.4, 0.8, 77.9, 0.7,
    0.11, 0.05, 13, 110, 1.3,
    13.3, '米饭,白米,粳米', 'dami', '四季',
    '提供能量，易消化吸收',
    '一般人群', '中国食物成分表', 'ACTIVE', true
FROM food_category fc WHERE fc.category_code = 'RICE'
ON DUPLICATE KEY UPDATE
    food_name = VALUES(food_name),
    energy = VALUES(energy),
    protein = VALUES(protein),
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO food_nutrition (
    food_code, food_name, food_name_en, category_id, 
    energy, protein, fat, carbohydrate, dietary_fiber,
    vitamin_b1, vitamin_b2, calcium, phosphorus, iron,
    water, search_keywords, pinyin, season, health_benefits,
    suitable_people, data_source, status, is_verified
) 
SELECT 
    'RICE_002', '糙米', 'Brown Rice', fc.id,
    348, 7.7, 2.7, 77.4, 3.9,
    0.41, 0.10, 14, 297, 2.3,
    12.5, '粗米,全米', 'zaomi', '四季',
    '富含膳食纤维，有助于控制血糖',
    '糖尿病患者,减肥人群', '中国食物成分表', 'ACTIVE', true
FROM food_category fc WHERE fc.category_code = 'RICE'
ON DUPLICATE KEY UPDATE
    food_name = VALUES(food_name),
    energy = VALUES(energy),
    protein = VALUES(protein),
    updated_at = CURRENT_TIMESTAMP;

-- 4. 验证数据完整性
-- 检查是否所有必需的分类都已存在
SELECT 
    CASE 
        WHEN COUNT(*) >= 15 THEN '✅ 一级分类数据完整'
        ELSE CONCAT('⚠️  缺失一级分类，当前数量: ', COUNT(*))
    END AS category_check
FROM food_category 
WHERE parent_id IS NULL;

-- 检查是否有食材数据
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN CONCAT('✅ 食材数据已导入，数量: ', COUNT(*))
        ELSE '⚠️  食材数据为空'
    END AS food_check
FROM food_nutrition;

-- 5. 创建统计视图（可选）
CREATE OR REPLACE VIEW v_food_category_stats AS
SELECT 
    fc.id,
    fc.category_code,
    fc.category_name,
    fc.level,
    COUNT(fn.id) as food_count
FROM food_category fc
LEFT JOIN food_nutrition fn ON fn.category_id = fc.id
GROUP BY fc.id, fc.category_code, fc.category_name, fc.level
ORDER BY fc.level, fc.sort_order;

-- 6. 添加注释说明
-- 本迁移脚本的目的：
-- 1. 更新已存在的分类描述
-- 2. 补充可能缺失的二级分类
-- 3. 使用ON DUPLICATE KEY UPDATE确保食材数据幂等性
-- 4. 验证数据完整性
-- 5. 不会因重复执行而报错

-- 完成标记
SELECT '✅ V10 数据迁移完成' as migration_status;
