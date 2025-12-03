-- 插入常见食材营养数据（示例数据，每100g）
-- 数据来源：中国食物成分表
-- 使用INSERT IGNORE避免重复插入

-- 谷薯类
INSERT IGNORE INTO food_nutrition (
    food_code, food_name, food_name_en, category_id, 
    energy, protein, fat, carbohydrate, dietary_fiber,
    vitamin_b1, vitamin_b2, calcium, phosphorus, iron,
    water, search_keywords, pinyin, season, health_benefits,
    suitable_people, data_source, status, is_verified
) VALUES
-- 大米
('RICE_001', '大米', 'Rice', (SELECT id FROM food_category WHERE category_code = 'RICE'),
 346, 7.4, 0.8, 77.9, 0.7,
 0.11, 0.05, 13, 110, 1.3,
 13.3, '米饭,白米,粳米', 'dami', '四季',
 '提供能量，易消化吸收',
 '一般人群', '中国食物成分表', 'ACTIVE', true),

-- 糙米
('RICE_002', '糙米', 'Brown Rice', (SELECT id FROM food_category WHERE category_code = 'RICE'),
 348, 7.7, 2.7, 77.4, 3.9,
 0.41, 0.10, 14, 297, 2.3,
 12.5, '粗米,全米', 'zaomi', '四季',
 '富含膳食纤维，有助于控制血糖',
 '糖尿病患者,减肥人群', '中国食物成分表', 'ACTIVE', true),

-- 小麦粉
('FLOUR_001', '小麦粉（标准粉）', 'Wheat Flour', (SELECT id FROM food_category WHERE category_code = 'FLOUR'),
 349, 11.2, 1.5, 74.3, 2.0,
 0.28, 0.08, 31, 188, 3.5,
 12.0, '面粉,白面', 'xiaomaifeng', '四季',
 '提供能量和蛋白质',
 '一般人群', '中国食物成分表', 'ACTIVE', true),

-- 燕麦
('GRAINS_001', '燕麦', 'Oats', (SELECT id FROM food_category WHERE category_code = 'GRAINS'),
 367, 15.0, 6.7, 66.9, 5.3,
 0.30, 0.14, 186, 291, 7.0,
 10.0, '麦片,燕麦片', 'yanmai', '四季',
 '降低胆固醇，稳定血糖，富含β-葡聚糖',
 '心血管疾病患者,糖尿病患者', '中国食物成分表', 'ACTIVE', true),

-- 玉米
('GRAINS_002', '玉米（鲜）', 'Sweet Corn', (SELECT id FROM food_category WHERE category_code = 'GRAINS'),
 106, 4.0, 1.2, 22.8, 2.9,
 0.16, 0.11, 5, 117, 1.1,
 69.6, '嫩玉米,甜玉米', 'yumi', '夏秋',
 '富含膳食纤维和维生素E',
 '一般人群', '中国食物成分表', 'ACTIVE', true),

-- 红薯
('POTATOES_001', '红薯', 'Sweet Potato', (SELECT id FROM food_category WHERE category_code = 'POTATOES'),
 99, 1.1, 0.2, 24.7, 1.6,
 0.04, 0.04, 18, 39, 0.5,
 72.8, '地瓜,山芋,番薯', 'hongshu', '秋冬',
 '富含膳食纤维和β-胡萝卜素，促进肠道蠕动',
 '一般人群,便秘患者', '中国食物成分表', 'ACTIVE', true),

-- 土豆
('POTATOES_002', '土豆', 'Potato', (SELECT id FROM food_category WHERE category_code = 'POTATOES'),
 76, 2.0, 0.2, 17.2, 0.7,
 0.08, 0.04, 8, 40, 0.8,
 78.6, '马铃薯,洋芋', 'tudou', '四季',
 '提供能量，富含钾',
 '一般人群', '中国食物成分表', 'ACTIVE', true);

-- 蔬菜类
INSERT IGNORE INTO food_nutrition (
    food_code, food_name, food_name_en, category_id, 
    energy, protein, fat, carbohydrate, dietary_fiber,
    vitamin_a, vitamin_c, calcium, potassium, iron,
    water, search_keywords, pinyin, season, health_benefits,
    suitable_people, data_source, status, is_verified
) VALUES
-- 白菜
('VEG_001', '大白菜', 'Chinese Cabbage', (SELECT id FROM food_category WHERE category_code = 'LEAFY_VEGETABLES'),
 15, 1.5, 0.2, 3.2, 1.0,
 20, 31, 50, 130, 0.7,
 94.7, '白菜,包菜', 'dabaicai', '冬春',
 '低热量，富含维生素C和膳食纤维',
 '一般人群,减肥人群', '中国食物成分表', 'ACTIVE', true),

-- 菠菜
('VEG_002', '菠菜', 'Spinach', (SELECT id FROM food_category WHERE category_code = 'LEAFY_VEGETABLES'),
 24, 2.6, 0.3, 4.5, 1.7,
 487, 32, 66, 311, 2.9,
 91.2, '菠菜,波斯菜', 'bocai', '春秋',
 '富含铁质和叶酸，补血佳品',
 '贫血患者,孕妇', '中国食物成分表', 'ACTIVE', true),

-- 西红柿
('VEG_003', '西红柿', 'Tomato', (SELECT id FROM food_category WHERE category_code = 'FRUIT_VEGETABLES'),
 19, 0.9, 0.2, 4.0, 0.5,
 92, 19, 10, 163, 0.5,
 94.0, '番茄,洋柿子', 'xihongshi', '夏秋',
 '富含番茄红素，抗氧化',
 '一般人群', '中国食物成分表', 'ACTIVE', true),

-- 黄瓜
('VEG_004', '黄瓜', 'Cucumber', (SELECT id FROM food_category WHERE category_code = 'FRUIT_VEGETABLES'),
 15, 0.8, 0.2, 2.9, 0.5,
 15, 9, 24, 102, 0.5,
 95.8, '青瓜', 'huanggua', '夏',
 '清热解毒，补水利尿',
 '一般人群,减肥人群', '中国食物成分表', 'ACTIVE', true),

-- 胡萝卜
('VEG_005', '胡萝卜', 'Carrot', (SELECT id FROM food_category WHERE category_code = 'ROOT_VEGETABLES'),
 37, 1.0, 0.2, 8.8, 3.2,
 688, 13, 32, 190, 1.0,
 89.0, '红萝卜', 'huluobo', '秋冬',
 '富含β-胡萝卜素，保护视力',
 '一般人群', '中国食物成分表', 'ACTIVE', true),

-- 西兰花
('VEG_006', '西兰花', 'Broccoli', (SELECT id FROM food_category WHERE category_code = 'FRUIT_VEGETABLES'),
 36, 4.1, 0.6, 6.6, 3.6,
 7210, 51, 67, 340, 1.0,
 89.2, '绿花菜,青花菜', 'xilanhua', '秋冬春',
 '富含维生素C和K，抗癌佳品',
 '一般人群', '中国食物成分表', 'ACTIVE', true);

-- 水果类
INSERT IGNORE INTO food_nutrition (
    food_code, food_name, food_name_en, category_id, 
    energy, protein, fat, carbohydrate, dietary_fiber,
    vitamin_c, calcium, potassium,
    water, search_keywords, pinyin, season, health_benefits,
    suitable_people, data_source, status, is_verified
) VALUES
-- 苹果
('FRUIT_001', '苹果', 'Apple', (SELECT id FROM food_category WHERE category_code = 'POME_FRUITS'),
 53, 0.2, 0.2, 13.7, 1.2,
 4, 4, 119,
 85.0, '平果', 'pingguo', '秋',
 '富含膳食纤维和维生素C',
 '一般人群', '中国食物成分表', 'ACTIVE', true),

-- 香蕉
('FRUIT_002', '香蕉', 'Banana', (SELECT id FROM food_category WHERE category_code = 'TROPICAL'),
 91, 1.4, 0.2, 22.0, 1.2,
 8, 7, 256,
 75.0, '芭蕉', 'xiangjiao', '四季',
 '快速补充能量，富含钾',
 '一般人群,运动人群', '中国食物成分表', 'ACTIVE', true),

-- 橙子
('FRUIT_003', '橙子', 'Orange', (SELECT id FROM food_category WHERE category_code = 'CITRUS'),
 48, 0.8, 0.2, 11.1, 0.6,
 33, 20, 159,
 87.0, '甜橙,柑橘', 'chengzi', '冬春',
 '富含维生素C，增强免疫力',
 '一般人群', '中国食物成分表', 'ACTIVE', true),

-- 草莓
('FRUIT_004', '草莓', 'Strawberry', (SELECT id FROM food_category WHERE category_code = 'BERRIES'),
 32, 1.0, 0.2, 7.1, 1.1,
 47, 18, 131,
 91.0, '红莓', 'caomei', '春',
 '富含维生素C和抗氧化物',
 '一般人群', '中国食物成分表', 'ACTIVE', true),

-- 西瓜
('FRUIT_005', '西瓜', 'Watermelon', (SELECT id FROM food_category WHERE category_code = 'MELON'),
 31, 0.6, 0.1, 7.9, 0.3,
 6, 8, 87,
 91.0, '寒瓜', 'xigua', '夏',
 '补水解暑，利尿',
 '一般人群', '中国食物成分表', 'ACTIVE', true);

-- 肉类
INSERT IGNORE INTO food_nutrition (
    food_code, food_name, food_name_en, category_id, 
    energy, protein, fat, carbohydrate,
    vitamin_b1, vitamin_b2, calcium, phosphorus, iron, zinc,
    cholesterol, water, search_keywords, pinyin, health_benefits,
    suitable_people, unsuitable_people, data_source, status, is_verified
) VALUES
-- 鸡胸肉
('CHICKEN_001', '鸡胸肉', 'Chicken Breast', (SELECT id FROM food_category WHERE category_code = 'CHICKEN'),
 133, 19.4, 5.0, 2.5,
 0.05, 0.09, 9, 156, 1.3, 1.09,
 58, 72.0, '鸡脯肉', 'jixiongrou',
 '高蛋白低脂肪，健身佳品',
 '一般人群,健身人群', '痛风患者应适量', '中国食物成分表', 'ACTIVE', true),

-- 猪瘦肉
('PORK_001', '猪瘦肉', 'Lean Pork', (SELECT id FROM food_category WHERE category_code = 'PORK'),
 143, 20.3, 6.2, 1.5,
 0.54, 0.16, 6, 162, 3.0, 2.88,
 81, 70.0, '猪里脊', 'zhushourou',
 '富含优质蛋白和铁',
 '一般人群', '高血脂患者应少食', '中国食物成分表', 'ACTIVE', true),

-- 牛肉
('BEEF_001', '牛肉', 'Beef', (SELECT id FROM food_category WHERE category_code = 'BEEF'),
 125, 20.1, 4.2, 2.0,
 0.07, 0.18, 9, 172, 2.8, 4.73,
 58, 72.6, '牛瘦肉', 'niurou',
 '富含优质蛋白和铁，补血佳品',
 '贫血患者,健身人群', '痛风患者应少食', '中国食物成分表', 'ACTIVE', true);

-- 水产类
INSERT IGNORE INTO food_nutrition (
    food_code, food_name, food_name_en, category_id, 
    energy, protein, fat, carbohydrate,
    vitamin_a, vitamin_e, calcium, phosphorus, potassium, selenium,
    cholesterol, water, search_keywords, pinyin, health_benefits,
    suitable_people, data_source, status, is_verified
) VALUES
-- 鲈鱼
('FISH_001', '鲈鱼', 'Sea Bass', (SELECT id FROM food_category WHERE category_code = 'FISH'),
 105, 18.6, 3.4, 0.0,
 19, 0.75, 138, 242, 205, 33.1,
 86, 77.0, '花鲈,七星鲈', 'luyu',
 '富含DHA和EPA，有益心脑血管',
 '一般人群,孕妇', '中国食物成分表', 'ACTIVE', true),

-- 虾
('SHRIMP_001', '虾', 'Shrimp', (SELECT id FROM food_category WHERE category_code = 'SHRIMP'),
 93, 18.6, 1.0, 2.8,
 15, 0.62, 62, 228, 215, 29.7,
 193, 76.0, '对虾,明虾', 'xia',
 '高蛋白低脂肪，富含矿物质',
 '一般人群', '中国食物成分表', 'ACTIVE', true);

-- 蛋类
INSERT IGNORE INTO food_nutrition (
    food_code, food_name, food_name_en, category_id, 
    energy, protein, fat, carbohydrate,
    vitamin_a, vitamin_b2, calcium, phosphorus, iron,
    cholesterol, water, search_keywords, pinyin, health_benefits,
    suitable_people, unsuitable_people, data_source, status, is_verified
) VALUES
-- 鸡蛋
('EGG_001', '鸡蛋', 'Egg', (SELECT id FROM food_category WHERE category_code = 'EGGS'),
 147, 13.3, 8.8, 2.8,
 234, 0.27, 56, 130, 2.0,
 585, 71.5, '鸡子', 'jidan',
 '营养全面，蛋白质优质',
 '一般人群', '高胆固醇患者应适量', '中国食物成分表', 'ACTIVE', true);

-- 奶类
INSERT IGNORE INTO food_nutrition (
    food_code, food_name, food_name_en, category_id, 
    energy, protein, fat, carbohydrate,
    vitamin_a, vitamin_b2, calcium, phosphorus,
    water, search_keywords, pinyin, health_benefits,
    suitable_people, data_source, status, is_verified
) VALUES
-- 纯牛奶
('MILK_001', '纯牛奶', 'Milk', (SELECT id FROM food_category WHERE category_code = 'MILK'),
 54, 3.0, 3.2, 3.4,
 24, 0.14, 104, 73,
 87.5, '鲜奶', 'chuniunai',
 '富含优质蛋白和钙',
 '一般人群,儿童,老人', '中国食物成分表', 'ACTIVE', true),

-- 酸奶
('YOGURT_001', '酸奶', 'Yogurt', (SELECT id FROM food_category WHERE category_code = 'YOGURT'),
 72, 2.5, 2.7, 9.3,
 26, 0.15, 118, 85,
 84.4, '乳酪,优格', 'suannai',
 '促进消化，富含益生菌',
 '一般人群,便秘患者', '中国食物成分表', 'ACTIVE', true);

-- 豆类
INSERT IGNORE INTO food_nutrition (
    food_code, food_name, food_name_en, category_id, 
    energy, protein, fat, carbohydrate, dietary_fiber,
    calcium, phosphorus, iron, potassium,
    water, search_keywords, pinyin, health_benefits,
    suitable_people, data_source, status, is_verified
) VALUES
-- 豆腐
('TOFU_001', '豆腐', 'Tofu', (SELECT id FROM food_category WHERE category_code = 'TOFU'),
 81, 8.1, 3.7, 4.2, 0.4,
 164, 119, 1.9, 125,
 82.8, '黄豆腐', 'doufu',
 '富含优质植物蛋白和钙',
 '一般人群,素食者', '中国食物成分表', 'ACTIVE', true),

-- 黄豆
('SOYBEANS_001', '黄豆', 'Soybean', (SELECT id FROM food_category WHERE category_code = 'SOYBEANS'),
 359, 35.0, 16.0, 34.2, 15.5,
 191, 465, 8.2, 1503,
 10.0, '大豆', 'huangdou',
 '富含蛋白质和异黄酮',
 '一般人群', '中国食物成分表', 'ACTIVE', true);

-- 坚果类
INSERT IGNORE INTO food_nutrition (
    food_code, food_name, food_name_en, category_id, 
    energy, protein, fat, carbohydrate, dietary_fiber,
    vitamin_e, calcium, magnesium, zinc,
    water, search_keywords, pinyin, health_benefits,
    suitable_people, unsuitable_people, data_source, status, is_verified
) VALUES
-- 核桃
('NUTS_001', '核桃', 'Walnut', (SELECT id FROM food_category WHERE category_code = 'TREE_NUTS'),
 646, 14.9, 58.8, 19.1, 9.5,
 43.21, 56, 131, 3.12,
 3.5, '胡桃', 'hetao',
 '富含不饱和脂肪酸，健脑益智',
 '一般人群', '肥胖者应少食', '中国食物成分表', 'ACTIVE', true),

-- 杏仁
('NUTS_002', '杏仁', 'Almond', (SELECT id FROM food_category WHERE category_code = 'TREE_NUTS'),
 578, 21.3, 50.6, 20.5, 11.8,
 25.63, 248, 275, 3.36,
 4.7, '巴旦木', 'xingren',
 '富含维生素E，抗氧化',
 '一般人群', '肥胖者应少食', '中国食物成分表', 'ACTIVE', true);
