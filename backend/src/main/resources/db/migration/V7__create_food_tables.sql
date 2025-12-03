-- 食材分类表
CREATE TABLE IF NOT EXISTS food_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    category_code VARCHAR(50) NOT NULL UNIQUE COMMENT '分类代码',
    category_name VARCHAR(100) NOT NULL COMMENT '分类名称',
    parent_id BIGINT DEFAULT NULL COMMENT '父分类ID',
    level INT DEFAULT 1 COMMENT '分类层级（1-3）',
    sort_order INT DEFAULT 0 COMMENT '排序',
    icon VARCHAR(200) COMMENT '图标URL',
    description TEXT COMMENT '描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id),
    INDEX idx_category_code (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食材分类表';

-- 食材营养数据表
CREATE TABLE IF NOT EXISTS food_nutrition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '食材ID',
    food_code VARCHAR(50) NOT NULL UNIQUE COMMENT '食材代码',
    food_name VARCHAR(200) NOT NULL COMMENT '食材名称',
    food_name_en VARCHAR(200) COMMENT '英文名称',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    
    -- 基本信息
    edible_portion DECIMAL(5,2) DEFAULT 100.00 COMMENT '可食部分（%）',
    
    -- 能量（每100g）
    energy DECIMAL(10,2) DEFAULT 0 COMMENT '能量（kcal）',
    
    -- 三大营养素（每100g）
    protein DECIMAL(10,2) DEFAULT 0 COMMENT '蛋白质（g）',
    fat DECIMAL(10,2) DEFAULT 0 COMMENT '脂肪（g）',
    carbohydrate DECIMAL(10,2) DEFAULT 0 COMMENT '碳水化合物（g）',
    dietary_fiber DECIMAL(10,2) DEFAULT 0 COMMENT '膳食纤维（g）',
    
    -- 维生素（每100g）
    vitamin_a DECIMAL(10,2) DEFAULT 0 COMMENT '维生素A（μg）',
    vitamin_b1 DECIMAL(10,2) DEFAULT 0 COMMENT '维生素B1（mg）',
    vitamin_b2 DECIMAL(10,2) DEFAULT 0 COMMENT '维生素B2（mg）',
    vitamin_c DECIMAL(10,2) DEFAULT 0 COMMENT '维生素C（mg）',
    vitamin_e DECIMAL(10,2) DEFAULT 0 COMMENT '维生素E（mg）',
    
    -- 矿物质（每100g）
    calcium DECIMAL(10,2) DEFAULT 0 COMMENT '钙（mg）',
    phosphorus DECIMAL(10,2) DEFAULT 0 COMMENT '磷（mg）',
    potassium DECIMAL(10,2) DEFAULT 0 COMMENT '钾（mg）',
    sodium DECIMAL(10,2) DEFAULT 0 COMMENT '钠（mg）',
    magnesium DECIMAL(10,2) DEFAULT 0 COMMENT '镁（mg）',
    iron DECIMAL(10,2) DEFAULT 0 COMMENT '铁（mg）',
    zinc DECIMAL(10,2) DEFAULT 0 COMMENT '锌（mg）',
    selenium DECIMAL(10,2) DEFAULT 0 COMMENT '硒（μg）',
    
    -- 其他营养素
    cholesterol DECIMAL(10,2) DEFAULT 0 COMMENT '胆固醇（mg）',
    water DECIMAL(10,2) DEFAULT 0 COMMENT '水分（g）',
    
    -- 标签和属性
    tags VARCHAR(500) COMMENT '标签（JSON数组）',
    season VARCHAR(100) COMMENT '季节（春/夏/秋/冬）',
    storage_method VARCHAR(200) COMMENT '储存方法',
    cooking_methods VARCHAR(500) COMMENT '烹饪方法（JSON数组）',
    
    -- 健康信息
    health_benefits TEXT COMMENT '健康益处',
    suitable_people VARCHAR(500) COMMENT '适宜人群',
    unsuitable_people VARCHAR(500) COMMENT '不适宜人群',
    
    -- 搜索优化
    search_keywords VARCHAR(500) COMMENT '搜索关键词',
    pinyin VARCHAR(200) COMMENT '拼音',
    
    -- 数据来源
    data_source VARCHAR(100) COMMENT '数据来源',
    reference_url VARCHAR(500) COMMENT '参考链接',
    
    -- 状态
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态（ACTIVE/INACTIVE）',
    is_verified BOOLEAN DEFAULT FALSE COMMENT '是否已验证',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_category_id (category_id),
    INDEX idx_food_code (food_code),
    INDEX idx_food_name (food_name),
    INDEX idx_status (status),
    FULLTEXT INDEX ft_search (food_name, search_keywords, pinyin) COMMENT '全文搜索索引',
    
    FOREIGN KEY (category_id) REFERENCES food_category(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食材营养数据表';

-- 常见食材份量参考表
CREATE TABLE IF NOT EXISTS food_portion_reference (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    food_id BIGINT NOT NULL COMMENT '食材ID',
    portion_name VARCHAR(100) NOT NULL COMMENT '份量名称（如：一个、一碗、一份）',
    portion_weight DECIMAL(10,2) NOT NULL COMMENT '份量重量（g）',
    description VARCHAR(200) COMMENT '描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_food_id (food_id),
    FOREIGN KEY (food_id) REFERENCES food_nutrition(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食材份量参考表';

-- 食材搜索日志表（用于优化搜索）
CREATE TABLE IF NOT EXISTS food_search_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT COMMENT '用户ID',
    search_keyword VARCHAR(200) NOT NULL COMMENT '搜索关键词',
    result_count INT DEFAULT 0 COMMENT '结果数量',
    clicked_food_id BIGINT COMMENT '点击的食材ID',
    search_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_search_keyword (search_keyword),
    INDEX idx_search_time (search_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食材搜索日志表';
