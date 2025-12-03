-- 插入一级分类（主要食材类别）
-- 使用INSERT IGNORE避免重复插入
INSERT IGNORE INTO food_category (id, category_code, category_name, parent_id, level, sort_order, description) VALUES
(1, 'CEREALS', '谷薯类', NULL, 1, 1, '包括大米、面粉、杂粮、薯类等主食'),
(2, 'VEGETABLES', '蔬菜类', NULL, 1, 2, '各种新鲜蔬菜、菌菇类'),
(3, 'FRUITS', '水果类', NULL, 1, 3, '各种新鲜水果、干果'),
(4, 'MEAT', '畜肉类', NULL, 1, 4, '猪肉、牛肉、羊肉等'),
(5, 'POULTRY', '禽肉类', NULL, 1, 5, '鸡肉、鸭肉等禽类'),
(6, 'SEAFOOD', '水产类', NULL, 1, 6, '鱼类、虾类、贝类等'),
(7, 'EGGS', '蛋类', NULL, 1, 7, '鸡蛋、鸭蛋等'),
(8, 'DAIRY', '奶类', NULL, 1, 8, '牛奶、酸奶、奶酪等'),
(9, 'BEANS', '豆类', NULL, 1, 9, '大豆、豆制品等'),
(10, 'NUTS', '坚果类', NULL, 1, 10, '核桃、杏仁、花生等'),
(11, 'OILS', '油脂类', NULL, 1, 11, '食用油、黄油等'),
(12, 'BEVERAGES', '饮品类', NULL, 1, 12, '茶、咖啡、果汁等'),
(13, 'CONDIMENTS', '调味品', NULL, 1, 13, '盐、酱油、香料等'),
(14, 'PROCESSED', '加工食品', NULL, 1, 14, '罐头、腌制品等'),
(15, 'SNACKS', '零食类', NULL, 1, 15, '饼干、糖果、膨化食品等');

-- 插入二级分类：谷薯类
INSERT IGNORE INTO food_category (category_code, category_name, parent_id, level, sort_order, description) VALUES
('RICE', '米类', 1, 2, 1, '大米、糙米、黑米等'),
('FLOUR', '面类', 1, 2, 2, '面粉、面条、面包等'),
('GRAINS', '杂粮', 1, 2, 3, '小米、玉米、燕麦等'),
('POTATOES', '薯类', 1, 2, 4, '土豆、红薯、山药等');

-- 插入二级分类：蔬菜类
INSERT IGNORE INTO food_category (category_code, category_name, parent_id, level, sort_order, description) VALUES
('LEAFY_VEGETABLES', '叶菜类', 2, 2, 1, '白菜、菠菜、生菜等'),
('ROOT_VEGETABLES', '根茎类', 2, 2, 2, '萝卜、胡萝卜、莲藕等'),
('FRUIT_VEGETABLES', '瓜果类', 2, 2, 3, '番茄、黄瓜、茄子等'),
('MUSHROOMS', '菌菇类', 2, 2, 4, '香菇、平菇、木耳等'),
('BEAN_VEGETABLES', '豆类蔬菜', 2, 2, 5, '豆角、豌豆、毛豆等');

-- 插入二级分类：水果类
INSERT IGNORE INTO food_category (category_code, category_name, parent_id, level, sort_order, description) VALUES
('CITRUS', '柑橘类', 3, 2, 1, '橙子、柚子、柠檬等'),
('BERRIES', '浆果类', 3, 2, 2, '草莓、蓝莓、葡萄等'),
('TROPICAL', '热带水果', 3, 2, 3, '香蕉、芒果、菠萝等'),
('STONE_FRUITS', '核果类', 3, 2, 4, '桃子、李子、樱桃等'),
('POME_FRUITS', '仁果类', 3, 2, 5, '苹果、梨等'),
('MELON', '瓜类', 3, 2, 6, '西瓜、哈密瓜等');

-- 插入二级分类：畜肉类
INSERT IGNORE INTO food_category (category_code, category_name, parent_id, level, sort_order, description) VALUES
('PORK', '猪肉', 4, 2, 1, '猪肉及制品'),
('BEEF', '牛肉', 4, 2, 2, '牛肉及制品'),
('LAMB', '羊肉', 4, 2, 3, '羊肉及制品');

-- 插入二级分类：禽肉类
INSERT IGNORE INTO food_category (category_code, category_name, parent_id, level, sort_order, description) VALUES
('CHICKEN', '鸡肉', 5, 2, 1, '鸡肉及制品'),
('DUCK', '鸭肉', 5, 2, 2, '鸭肉及制品'),
('OTHER_POULTRY', '其他禽类', 5, 2, 3, '鹅肉、鸽肉等');

-- 插入二级分类：水产类
INSERT IGNORE INTO food_category (category_code, category_name, parent_id, level, sort_order, description) VALUES
('FISH', '鱼类', 6, 2, 1, '各种淡水鱼、海水鱼'),
('SHRIMP', '虾类', 6, 2, 2, '对虾、基围虾等'),
('CRAB', '蟹类', 6, 2, 3, '梭子蟹、大闸蟹等'),
('SHELLFISH', '贝类', 6, 2, 4, '蛤蜊、扇贝等'),
('SEAWEED', '海藻类', 6, 2, 5, '海带、紫菜等');

-- 插入二级分类：豆类
INSERT IGNORE INTO food_category (category_code, category_name, parent_id, level, sort_order, description) VALUES
('SOYBEANS', '大豆类', 9, 2, 1, '黄豆、黑豆等'),
('TOFU', '豆制品', 9, 2, 2, '豆腐、豆浆、豆干等'),
('OTHER_BEANS', '其他豆类', 9, 2, 3, '绿豆、红豆等');

-- 插入二级分类：奶类
INSERT IGNORE INTO food_category (category_code, category_name, parent_id, level, sort_order, description) VALUES
('MILK', '鲜奶', 8, 2, 1, '纯牛奶、脱脂奶等'),
('YOGURT', '酸奶', 8, 2, 2, '各种酸奶、乳酸菌饮料'),
('CHEESE', '奶酪', 8, 2, 3, '各种奶酪制品');

-- 插入二级分类：坚果类
INSERT IGNORE INTO food_category (category_code, category_name, parent_id, level, sort_order, description) VALUES
('TREE_NUTS', '树坚果', 10, 2, 1, '核桃、杏仁、腰果等'),
('PEANUTS', '花生', 10, 2, 2, '花生及花生制品'),
('SEEDS', '种子类', 10, 2, 3, '瓜子、芝麻等');
