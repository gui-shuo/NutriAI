-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: nutriai
-- ------------------------------------------------------
-- Server version	8.0.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `api_logs`
--

DROP TABLE IF EXISTS `api_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `api_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `api_type` enum('tongyi','oss','image_recognition','other') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'API类型',
  `api_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'API名称',
  `request_data` text COLLATE utf8mb4_unicode_ci COMMENT '请求数据',
  `response_data` text COLLATE utf8mb4_unicode_ci COMMENT '响应数据',
  `tokens_used` int DEFAULT NULL COMMENT '使用tokens数',
  `cost` decimal(10,4) DEFAULT NULL COMMENT '费用（元）',
  `status_code` int DEFAULT NULL COMMENT 'HTTP状态码',
  `response_time` int DEFAULT NULL COMMENT '响应时间（ms）',
  `error_message` text COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_api_type` (`api_type`),
  KEY `idx_status_code` (`status_code`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='第三方API调用日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `api_logs`
--

LOCK TABLES `api_logs` WRITE;
/*!40000 ALTER TABLE `api_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `api_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `chat_messages`
--

DROP TABLE IF EXISTS `chat_messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_messages` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `session_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话ID',
  `role` enum('user','assistant','system') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `tokens_used` int DEFAULT NULL COMMENT '使用tokens数',
  `model` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '使用模型',
  `response_time` int DEFAULT NULL COMMENT '响应时间（ms）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_session` (`user_id`,`session_id`),
  KEY `idx_role` (`role`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_chat_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chat_messages`
--

LOCK TABLES `chat_messages` WRITE;
/*!40000 ALTER TABLE `chat_messages` DISABLE KEYS */;
/*!40000 ALTER TABLE `chat_messages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `diet_plans`
--

DROP TABLE IF EXISTS `diet_plans`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `diet_plans` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '计划标题',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '计划描述',
  `plan_type` enum('weight_loss','muscle_gain','balanced','custom') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '计划类型',
  `duration_days` int NOT NULL COMMENT '计划天数',
  `daily_budget` decimal(10,2) DEFAULT NULL COMMENT '每日预算（元）',
  `plan_content` json NOT NULL COMMENT '计划内容（JSON结构）',
  `is_favorite` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否收藏',
  `status` enum('active','completed','cancelled') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active' COMMENT '计划状态',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_plan_type` (`plan_type`),
  KEY `idx_status` (`status`),
  KEY `idx_is_favorite` (`is_favorite`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_plan_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI生成饮食计划表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `diet_plans`
--

LOCK TABLES `diet_plans` WRITE;
/*!40000 ALTER TABLE `diet_plans` DISABLE KEYS */;
/*!40000 ALTER TABLE `diet_plans` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `diet_records`
--

DROP TABLE IF EXISTS `diet_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `diet_records` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `record_date` date NOT NULL COMMENT '记录日期',
  `meal_type` enum('breakfast','lunch','dinner','snack') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '餐次',
  `meal_time` time DEFAULT NULL COMMENT '用餐时间',
  `food_items` json NOT NULL COMMENT '食物列表（JSON数组）',
  `total_calories` int DEFAULT NULL COMMENT '总热量（kcal）',
  `total_protein` decimal(8,2) DEFAULT NULL COMMENT '总蛋白质（g）',
  `total_carbs` decimal(8,2) DEFAULT NULL COMMENT '总碳水化合物（g）',
  `total_fat` decimal(8,2) DEFAULT NULL COMMENT '总脂肪（g）',
  `images` json DEFAULT NULL COMMENT '照片URL列表（JSON数组）',
  `notes` text COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`,`record_date`),
  KEY `idx_meal_type` (`meal_type`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_diet_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='饮食记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `diet_records`
--

LOCK TABLES `diet_records` WRITE;
/*!40000 ALTER TABLE `diet_records` DISABLE KEYS */;
/*!40000 ALTER TABLE `diet_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `favorites`
--

DROP TABLE IF EXISTS `favorites`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `favorites` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `item_type` enum('diet_plan','recipe','food','chat_message') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '收藏类型',
  `item_id` bigint NOT NULL COMMENT '收藏项ID',
  `notes` text COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_item` (`user_id`,`item_type`,`item_id`),
  KEY `idx_item_type` (`item_type`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `favorites`
--

LOCK TABLES `favorites` WRITE;
/*!40000 ALTER TABLE `favorites` DISABLE KEYS */;
/*!40000 ALTER TABLE `favorites` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `script` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
INSERT INTO `flyway_schema_history` VALUES (1,'1','<< Flyway Baseline >>','BASELINE','<< Flyway Baseline >>',NULL,'root','2025-12-03 06:16:53',0,1),(2,'5','Create Food Records Table','SQL','V5__Create_Food_Records_Table.sql',-1601675978,'root','2025-12-03 06:16:54',14,1),(3,'6','Create Member Tables','SQL','V6__Create_Member_Tables.sql',NULL,'root','2025-12-03 06:22:39',0,1);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `food_categories`
--

DROP TABLE IF EXISTS `food_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `food_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `name_en` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '英文名称',
  `parent_id` bigint DEFAULT NULL COMMENT '父分类ID',
  `icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类图标',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食物分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `food_categories`
--

LOCK TABLES `food_categories` WRITE;
/*!40000 ALTER TABLE `food_categories` DISABLE KEYS */;
INSERT INTO `food_categories` VALUES (1,'谷物类','Grains',NULL,'🌾',1,'2025-12-02 15:51:27'),(2,'蔬菜类','Vegetables',NULL,'🥬',2,'2025-12-02 15:51:27'),(3,'水果类','Fruits',NULL,'🍎',3,'2025-12-02 15:51:27'),(4,'肉类','Meat',NULL,'🥩',4,'2025-12-02 15:51:27'),(5,'水产类','Seafood',NULL,'🐟',5,'2025-12-02 15:51:27'),(6,'蛋奶类','Dairy',NULL,'🥛',6,'2025-12-02 15:51:27'),(7,'豆制品','Legumes',NULL,'🫘',7,'2025-12-02 15:51:27'),(8,'坚果类','Nuts',NULL,'🌰',8,'2025-12-02 15:51:27');
/*!40000 ALTER TABLE `food_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `food_category`
--

DROP TABLE IF EXISTS `food_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `food_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `category_code` varchar(50) NOT NULL COMMENT '分类代码',
  `category_name` varchar(100) NOT NULL COMMENT '分类名称',
  `parent_id` bigint DEFAULT NULL COMMENT '父分类ID',
  `level` int DEFAULT '1' COMMENT '分类层级（1-3）',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `icon` varchar(200) DEFAULT NULL COMMENT '图标URL',
  `description` text COMMENT '描述',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `category_code` (`category_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_category_code` (`category_code`)
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='食材分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `food_category`
--

LOCK TABLES `food_category` WRITE;
/*!40000 ALTER TABLE `food_category` DISABLE KEYS */;
INSERT INTO `food_category` VALUES (1,'CEREALS','谷薯类',NULL,1,1,NULL,'包括大米、面粉、杂粮、薯类等主食','2025-12-03 08:18:39','2025-12-03 08:18:39'),(2,'VEGETABLES','蔬菜类',NULL,1,2,NULL,'各种新鲜蔬菜、菌菇类','2025-12-03 08:18:39','2025-12-03 08:18:39'),(3,'FRUITS','水果类',NULL,1,3,NULL,'各种新鲜水果、干果','2025-12-03 08:18:39','2025-12-03 08:18:39'),(4,'MEAT','畜肉类',NULL,1,4,NULL,'猪肉、牛肉、羊肉等','2025-12-03 08:18:39','2025-12-03 08:18:39'),(5,'POULTRY','禽肉类',NULL,1,5,NULL,'鸡肉、鸭肉等禽类','2025-12-03 08:18:39','2025-12-03 08:18:39'),(6,'SEAFOOD','水产类',NULL,1,6,NULL,'鱼类、虾类、贝类等','2025-12-03 08:18:39','2025-12-03 08:18:39'),(7,'EGGS','蛋类',NULL,1,7,NULL,'鸡蛋、鸭蛋等','2025-12-03 08:18:39','2025-12-03 08:18:39'),(8,'DAIRY','奶类',NULL,1,8,NULL,'牛奶、酸奶、奶酪等','2025-12-03 08:18:39','2025-12-03 08:18:39'),(9,'BEANS','豆类',NULL,1,9,NULL,'大豆、豆制品等','2025-12-03 08:18:39','2025-12-03 08:18:39'),(10,'NUTS','坚果类',NULL,1,10,NULL,'核桃、杏仁、花生等','2025-12-03 08:18:39','2025-12-03 08:18:39'),(11,'OILS','油脂类',NULL,1,11,NULL,'食用油、黄油等','2025-12-03 08:18:39','2025-12-03 08:18:39'),(12,'BEVERAGES','饮品类',NULL,1,12,NULL,'茶、咖啡、果汁等','2025-12-03 08:18:39','2025-12-03 08:18:39'),(13,'CONDIMENTS','调味品',NULL,1,13,NULL,'盐、酱油、香料等','2025-12-03 08:18:39','2025-12-03 08:18:39'),(14,'PROCESSED','加工食品',NULL,1,14,NULL,'罐头、腌制品等','2025-12-03 08:18:39','2025-12-03 08:18:39'),(15,'SNACKS','零食类',NULL,1,15,NULL,'饼干、糖果、膨化食品等','2025-12-03 08:18:39','2025-12-03 08:18:39'),(16,'RICE','米类',1,2,1,NULL,'大米、糙米、黑米等','2025-12-03 08:18:39','2025-12-03 08:18:39'),(17,'FLOUR','面类',1,2,2,NULL,'面粉、面条、面包等','2025-12-03 08:18:39','2025-12-03 08:18:39'),(18,'GRAINS','杂粮',1,2,3,NULL,'小米、玉米、燕麦等','2025-12-03 08:18:39','2025-12-03 08:18:39'),(19,'POTATOES','薯类',1,2,4,NULL,'土豆、红薯、山药等','2025-12-03 08:18:39','2025-12-03 08:18:39'),(20,'LEAFY_VEGETABLES','叶菜类',2,2,1,NULL,'白菜、菠菜、生菜等','2025-12-03 08:18:40','2025-12-03 08:18:40'),(21,'ROOT_VEGETABLES','根茎类',2,2,2,NULL,'萝卜、胡萝卜、莲藕等','2025-12-03 08:18:40','2025-12-03 08:18:40'),(22,'FRUIT_VEGETABLES','瓜果类',2,2,3,NULL,'番茄、黄瓜、茄子等','2025-12-03 08:18:40','2025-12-03 08:18:40'),(23,'MUSHROOMS','菌菇类',2,2,4,NULL,'香菇、平菇、木耳等','2025-12-03 08:18:40','2025-12-03 08:18:40'),(24,'BEAN_VEGETABLES','豆类蔬菜',2,2,5,NULL,'豆角、豌豆、毛豆等','2025-12-03 08:18:40','2025-12-03 08:18:40'),(25,'CITRUS','柑橘类',3,2,1,NULL,'橙子、柚子、柠檬等','2025-12-03 08:18:40','2025-12-03 08:18:40'),(26,'BERRIES','浆果类',3,2,2,NULL,'草莓、蓝莓、葡萄等','2025-12-03 08:18:40','2025-12-03 08:18:40'),(27,'TROPICAL','热带水果',3,2,3,NULL,'香蕉、芒果、菠萝等','2025-12-03 08:18:40','2025-12-03 08:18:40'),(28,'STONE_FRUITS','核果类',3,2,4,NULL,'桃子、李子、樱桃等','2025-12-03 08:18:40','2025-12-03 08:18:40'),(29,'POME_FRUITS','仁果类',3,2,5,NULL,'苹果、梨等','2025-12-03 08:18:40','2025-12-03 08:18:40'),(30,'MELON','瓜类',3,2,6,NULL,'西瓜、哈密瓜等','2025-12-03 08:18:40','2025-12-03 08:18:40'),(31,'PORK','猪肉',4,2,1,NULL,'猪肉及制品','2025-12-03 08:18:40','2025-12-03 08:18:40'),(32,'BEEF','牛肉',4,2,2,NULL,'牛肉及制品','2025-12-03 08:18:40','2025-12-03 08:18:40'),(33,'LAMB','羊肉',4,2,3,NULL,'羊肉及制品','2025-12-03 08:18:40','2025-12-03 08:18:40'),(34,'CHICKEN','鸡肉',5,2,1,NULL,'鸡肉及制品','2025-12-03 08:18:40','2025-12-03 08:18:40'),(35,'DUCK','鸭肉',5,2,2,NULL,'鸭肉及制品','2025-12-03 08:18:40','2025-12-03 08:18:40'),(36,'OTHER_POULTRY','其他禽类',5,2,3,NULL,'鹅肉、鸽肉等','2025-12-03 08:18:40','2025-12-03 08:18:40'),(37,'FISH','鱼类',6,2,1,NULL,'各种淡水鱼、海水鱼','2025-12-03 08:18:40','2025-12-03 08:18:40'),(38,'SHRIMP','虾类',6,2,2,NULL,'对虾、基围虾等','2025-12-03 08:18:40','2025-12-03 08:18:40'),(39,'CRAB','蟹类',6,2,3,NULL,'梭子蟹、大闸蟹等','2025-12-03 08:18:40','2025-12-03 08:18:40'),(40,'SHELLFISH','贝类',6,2,4,NULL,'蛤蜊、扇贝等','2025-12-03 08:18:40','2025-12-03 08:18:40'),(41,'SEAWEED','海藻类',6,2,5,NULL,'海带、紫菜等','2025-12-03 08:18:40','2025-12-03 08:18:40'),(42,'SOYBEANS','大豆类',9,2,1,NULL,'黄豆、黑豆等','2025-12-03 08:18:40','2025-12-03 08:18:40'),(43,'TOFU','豆制品',9,2,2,NULL,'豆腐、豆浆、豆干等','2025-12-03 08:18:40','2025-12-03 08:18:40'),(44,'OTHER_BEANS','其他豆类',9,2,3,NULL,'绿豆、红豆等','2025-12-03 08:18:40','2025-12-03 08:18:40'),(45,'MILK','鲜奶',8,2,1,NULL,'纯牛奶、脱脂奶等','2025-12-03 08:18:40','2025-12-03 08:18:40'),(46,'YOGURT','酸奶',8,2,2,NULL,'各种酸奶、乳酸菌饮料','2025-12-03 08:18:40','2025-12-03 08:18:40'),(47,'CHEESE','奶酪',8,2,3,NULL,'各种奶酪制品','2025-12-03 08:18:40','2025-12-03 08:18:40'),(48,'TREE_NUTS','树坚果',10,2,1,NULL,'核桃、杏仁、腰果等','2025-12-03 08:18:40','2025-12-03 08:18:40'),(49,'PEANUTS','花生',10,2,2,NULL,'花生及花生制品','2025-12-03 08:18:40','2025-12-03 08:18:40'),(50,'SEEDS','种子类',10,2,3,NULL,'瓜子、芝麻等','2025-12-03 08:18:40','2025-12-03 08:18:40');
/*!40000 ALTER TABLE `food_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `food_nutrition`
--

DROP TABLE IF EXISTS `food_nutrition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `food_nutrition` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '食材ID',
  `food_code` varchar(50) NOT NULL COMMENT '食材代码',
  `food_name` varchar(200) NOT NULL COMMENT '食材名称',
  `food_name_en` varchar(200) DEFAULT NULL COMMENT '英文名称',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `edible_portion` decimal(5,2) DEFAULT '100.00' COMMENT '可食部分（%）',
  `energy` decimal(10,2) DEFAULT '0.00' COMMENT '能量（kcal）',
  `protein` decimal(10,2) DEFAULT '0.00' COMMENT '蛋白质（g）',
  `fat` decimal(10,2) DEFAULT '0.00' COMMENT '脂肪（g）',
  `carbohydrate` decimal(10,2) DEFAULT '0.00' COMMENT '碳水化合物（g）',
  `dietary_fiber` decimal(10,2) DEFAULT '0.00' COMMENT '膳食纤维（g）',
  `vitamin_a` decimal(10,2) DEFAULT '0.00' COMMENT '维生素A（μg）',
  `vitamin_b1` decimal(10,2) DEFAULT '0.00' COMMENT '维生素B1（mg）',
  `vitamin_b2` decimal(10,2) DEFAULT '0.00' COMMENT '维生素B2（mg）',
  `vitamin_c` decimal(10,2) DEFAULT '0.00' COMMENT '维生素C（mg）',
  `vitamin_e` decimal(10,2) DEFAULT '0.00' COMMENT '维生素E（mg）',
  `calcium` decimal(10,2) DEFAULT '0.00' COMMENT '钙（mg）',
  `phosphorus` decimal(10,2) DEFAULT '0.00' COMMENT '磷（mg）',
  `potassium` decimal(10,2) DEFAULT '0.00' COMMENT '钾（mg）',
  `sodium` decimal(10,2) DEFAULT '0.00' COMMENT '钠（mg）',
  `magnesium` decimal(10,2) DEFAULT '0.00' COMMENT '镁（mg）',
  `iron` decimal(10,2) DEFAULT '0.00' COMMENT '铁（mg）',
  `zinc` decimal(10,2) DEFAULT '0.00' COMMENT '锌（mg）',
  `selenium` decimal(10,2) DEFAULT '0.00' COMMENT '硒（μg）',
  `cholesterol` decimal(10,2) DEFAULT '0.00' COMMENT '胆固醇（mg）',
  `water` decimal(10,2) DEFAULT '0.00' COMMENT '水分（g）',
  `tags` varchar(500) DEFAULT NULL COMMENT '标签（JSON数组）',
  `season` varchar(100) DEFAULT NULL COMMENT '季节（春/夏/秋/冬）',
  `storage_method` varchar(200) DEFAULT NULL COMMENT '储存方法',
  `cooking_methods` varchar(500) DEFAULT NULL COMMENT '烹饪方法（JSON数组）',
  `health_benefits` text COMMENT '健康益处',
  `suitable_people` varchar(500) DEFAULT NULL COMMENT '适宜人群',
  `unsuitable_people` varchar(500) DEFAULT NULL COMMENT '不适宜人群',
  `search_keywords` varchar(500) DEFAULT NULL COMMENT '搜索关键词',
  `pinyin` varchar(200) DEFAULT NULL COMMENT '拼音',
  `data_source` varchar(100) DEFAULT NULL COMMENT '数据来源',
  `reference_url` varchar(500) DEFAULT NULL COMMENT '参考链接',
  `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '状态（ACTIVE/INACTIVE）',
  `is_verified` tinyint(1) DEFAULT '0' COMMENT '是否已验证',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `food_code` (`food_code`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_food_code` (`food_code`),
  KEY `idx_food_name` (`food_name`),
  KEY `idx_status` (`status`),
  FULLTEXT KEY `ft_search` (`food_name`,`search_keywords`,`pinyin`) COMMENT '全文搜索索引',
  CONSTRAINT `food_nutrition_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `food_category` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='食材营养数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `food_nutrition`
--

LOCK TABLES `food_nutrition` WRITE;
/*!40000 ALTER TABLE `food_nutrition` DISABLE KEYS */;
INSERT INTO `food_nutrition` VALUES (1,'RICE_001','大米','Rice',16,100.00,346.00,7.40,0.80,77.90,0.70,0.00,0.11,0.05,0.00,0.00,13.00,110.00,0.00,0.00,0.00,1.30,0.00,0.00,0.00,13.30,NULL,'四季',NULL,NULL,'提供能量，易消化吸收','一般人群',NULL,'米饭,白米,粳米','dami','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(2,'RICE_002','糙米','Brown Rice',16,100.00,348.00,7.70,2.70,77.40,3.90,0.00,0.41,0.10,0.00,0.00,14.00,297.00,0.00,0.00,0.00,2.30,0.00,0.00,0.00,12.50,NULL,'四季',NULL,NULL,'富含膳食纤维，有助于控制血糖','糖尿病患者,减肥人群',NULL,'粗米,全米','zaomi','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(3,'FLOUR_001','小麦粉（标准粉）','Wheat Flour',17,100.00,349.00,11.20,1.50,74.30,2.00,0.00,0.28,0.08,0.00,0.00,31.00,188.00,0.00,0.00,0.00,3.50,0.00,0.00,0.00,12.00,NULL,'四季',NULL,NULL,'提供能量和蛋白质','一般人群',NULL,'面粉,白面','xiaomaifeng','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(4,'GRAINS_001','燕麦','Oats',18,100.00,367.00,15.00,6.70,66.90,5.30,0.00,0.30,0.14,0.00,0.00,186.00,291.00,0.00,0.00,0.00,7.00,0.00,0.00,0.00,10.00,NULL,'四季',NULL,NULL,'降低胆固醇，稳定血糖，富含β-葡聚糖','心血管疾病患者,糖尿病患者',NULL,'麦片,燕麦片','yanmai','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(5,'GRAINS_002','玉米（鲜）','Sweet Corn',18,100.00,106.00,4.00,1.20,22.80,2.90,0.00,0.16,0.11,0.00,0.00,5.00,117.00,0.00,0.00,0.00,1.10,0.00,0.00,0.00,69.60,NULL,'夏秋',NULL,NULL,'富含膳食纤维和维生素E','一般人群',NULL,'嫩玉米,甜玉米','yumi','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(6,'POTATOES_001','红薯','Sweet Potato',19,100.00,99.00,1.10,0.20,24.70,1.60,0.00,0.04,0.04,0.00,0.00,18.00,39.00,0.00,0.00,0.00,0.50,0.00,0.00,0.00,72.80,NULL,'秋冬',NULL,NULL,'富含膳食纤维和β-胡萝卜素，促进肠道蠕动','一般人群,便秘患者',NULL,'地瓜,山芋,番薯','hongshu','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(7,'POTATOES_002','土豆','Potato',19,100.00,76.00,2.00,0.20,17.20,0.70,0.00,0.08,0.04,0.00,0.00,8.00,40.00,0.00,0.00,0.00,0.80,0.00,0.00,0.00,78.60,NULL,'四季',NULL,NULL,'提供能量，富含钾','一般人群',NULL,'马铃薯,洋芋','tudou','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(8,'VEG_001','大白菜','Chinese Cabbage',20,100.00,15.00,1.50,0.20,3.20,1.00,20.00,0.00,0.00,31.00,0.00,50.00,0.00,130.00,0.00,0.00,0.70,0.00,0.00,0.00,94.70,NULL,'冬春',NULL,NULL,'低热量，富含维生素C和膳食纤维','一般人群,减肥人群',NULL,'白菜,包菜','dabaicai','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(9,'VEG_002','菠菜','Spinach',20,100.00,24.00,2.60,0.30,4.50,1.70,487.00,0.00,0.00,32.00,0.00,66.00,0.00,311.00,0.00,0.00,2.90,0.00,0.00,0.00,91.20,NULL,'春秋',NULL,NULL,'富含铁质和叶酸，补血佳品','贫血患者,孕妇',NULL,'菠菜,波斯菜','bocai','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(10,'VEG_003','西红柿','Tomato',22,100.00,19.00,0.90,0.20,4.00,0.50,92.00,0.00,0.00,19.00,0.00,10.00,0.00,163.00,0.00,0.00,0.50,0.00,0.00,0.00,94.00,NULL,'夏秋',NULL,NULL,'富含番茄红素，抗氧化','一般人群',NULL,'番茄,洋柿子','xihongshi','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(11,'VEG_004','黄瓜','Cucumber',22,100.00,15.00,0.80,0.20,2.90,0.50,15.00,0.00,0.00,9.00,0.00,24.00,0.00,102.00,0.00,0.00,0.50,0.00,0.00,0.00,95.80,NULL,'夏',NULL,NULL,'清热解毒，补水利尿','一般人群,减肥人群',NULL,'青瓜','huanggua','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(12,'VEG_005','胡萝卜','Carrot',21,100.00,37.00,1.00,0.20,8.80,3.20,688.00,0.00,0.00,13.00,0.00,32.00,0.00,190.00,0.00,0.00,1.00,0.00,0.00,0.00,89.00,NULL,'秋冬',NULL,NULL,'富含β-胡萝卜素，保护视力','一般人群',NULL,'红萝卜','huluobo','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(13,'VEG_006','西兰花','Broccoli',22,100.00,36.00,4.10,0.60,6.60,3.60,7210.00,0.00,0.00,51.00,0.00,67.00,0.00,340.00,0.00,0.00,1.00,0.00,0.00,0.00,89.20,NULL,'秋冬春',NULL,NULL,'富含维生素C和K，抗癌佳品','一般人群',NULL,'绿花菜,青花菜','xilanhua','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(14,'FRUIT_001','苹果','Apple',29,100.00,53.00,0.20,0.20,13.70,1.20,0.00,0.00,0.00,4.00,0.00,4.00,0.00,119.00,0.00,0.00,0.00,0.00,0.00,0.00,85.00,NULL,'秋',NULL,NULL,'富含膳食纤维和维生素C','一般人群',NULL,'平果','pingguo','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(15,'FRUIT_002','香蕉','Banana',27,100.00,91.00,1.40,0.20,22.00,1.20,0.00,0.00,0.00,8.00,0.00,7.00,0.00,256.00,0.00,0.00,0.00,0.00,0.00,0.00,75.00,NULL,'四季',NULL,NULL,'快速补充能量，富含钾','一般人群,运动人群',NULL,'芭蕉','xiangjiao','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(16,'FRUIT_003','橙子','Orange',25,100.00,48.00,0.80,0.20,11.10,0.60,0.00,0.00,0.00,33.00,0.00,20.00,0.00,159.00,0.00,0.00,0.00,0.00,0.00,0.00,87.00,NULL,'冬春',NULL,NULL,'富含维生素C，增强免疫力','一般人群',NULL,'甜橙,柑橘','chengzi','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(17,'FRUIT_004','草莓','Strawberry',26,100.00,32.00,1.00,0.20,7.10,1.10,0.00,0.00,0.00,47.00,0.00,18.00,0.00,131.00,0.00,0.00,0.00,0.00,0.00,0.00,91.00,NULL,'春',NULL,NULL,'富含维生素C和抗氧化物','一般人群',NULL,'红莓','caomei','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(18,'FRUIT_005','西瓜','Watermelon',30,100.00,31.00,0.60,0.10,7.90,0.30,0.00,0.00,0.00,6.00,0.00,8.00,0.00,87.00,0.00,0.00,0.00,0.00,0.00,0.00,91.00,NULL,'夏',NULL,NULL,'补水解暑，利尿','一般人群',NULL,'寒瓜','xigua','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(19,'CHICKEN_001','鸡胸肉','Chicken Breast',34,100.00,133.00,19.40,5.00,2.50,0.00,0.00,0.05,0.09,0.00,0.00,9.00,156.00,0.00,0.00,0.00,1.30,1.09,0.00,58.00,72.00,NULL,NULL,NULL,NULL,'高蛋白低脂肪，健身佳品','一般人群,健身人群','痛风患者应适量','鸡脯肉','jixiongrou','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(20,'PORK_001','猪瘦肉','Lean Pork',31,100.00,143.00,20.30,6.20,1.50,0.00,0.00,0.54,0.16,0.00,0.00,6.00,162.00,0.00,0.00,0.00,3.00,2.88,0.00,81.00,70.00,NULL,NULL,NULL,NULL,'富含优质蛋白和铁','一般人群','高血脂患者应少食','猪里脊','zhushourou','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(21,'BEEF_001','牛肉','Beef',32,100.00,125.00,20.10,4.20,2.00,0.00,0.00,0.07,0.18,0.00,0.00,9.00,172.00,0.00,0.00,0.00,2.80,4.73,0.00,58.00,72.60,NULL,NULL,NULL,NULL,'富含优质蛋白和铁，补血佳品','贫血患者,健身人群','痛风患者应少食','牛瘦肉','niurou','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(22,'FISH_001','鲈鱼','Sea Bass',37,100.00,105.00,18.60,3.40,0.00,0.00,19.00,0.00,0.00,0.00,0.75,138.00,242.00,205.00,0.00,0.00,0.00,0.00,33.10,86.00,77.00,NULL,NULL,NULL,NULL,'富含DHA和EPA，有益心脑血管','一般人群,孕妇',NULL,'花鲈,七星鲈','luyu','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(23,'SHRIMP_001','虾','Shrimp',38,100.00,93.00,18.60,1.00,2.80,0.00,15.00,0.00,0.00,0.00,0.62,62.00,228.00,215.00,0.00,0.00,0.00,0.00,29.70,193.00,76.00,NULL,NULL,NULL,NULL,'高蛋白低脂肪，富含矿物质','一般人群',NULL,'对虾,明虾','xia','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(24,'TOFU_001','豆腐','Tofu',43,100.00,81.00,8.10,3.70,4.20,0.40,0.00,0.00,0.00,0.00,0.00,164.00,119.00,125.00,0.00,0.00,1.90,0.00,0.00,0.00,82.80,NULL,NULL,NULL,NULL,'富含优质植物蛋白和钙','一般人群,素食者',NULL,'黄豆腐','doufu','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:19:36','2025-12-03 08:19:36'),(25,'SOYBEANS_001','黄豆','Soybean',42,100.00,359.00,35.00,16.00,34.20,15.50,0.00,0.00,0.00,0.00,0.00,191.00,465.00,1503.00,0.00,0.00,8.20,0.00,0.00,0.00,10.00,NULL,NULL,NULL,NULL,'富含蛋白质和异黄酮','一般人群',NULL,'大豆','huangdou','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:19:36','2025-12-03 08:19:36'),(26,'NUTS_001','核桃','Walnut',48,100.00,646.00,14.90,58.80,19.10,9.50,0.00,0.00,0.00,0.00,43.21,56.00,0.00,0.00,0.00,131.00,0.00,3.12,0.00,0.00,3.50,NULL,NULL,NULL,NULL,'富含不饱和脂肪酸，健脑益智','一般人群','肥胖者应少食','胡桃','hetao','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:19:45','2025-12-03 08:19:45'),(27,'NUTS_002','杏仁','Almond',48,100.00,578.00,21.30,50.60,20.50,11.80,0.00,0.00,0.00,0.00,25.63,248.00,0.00,0.00,0.00,275.00,0.00,3.36,0.00,0.00,4.70,NULL,NULL,NULL,NULL,'富含维生素E，抗氧化','一般人群','肥胖者应少食','巴旦木','xingren','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:19:45','2025-12-03 08:19:45'),(28,'EGG_001','鸡蛋','Egg',7,100.00,147.00,13.30,8.80,2.80,0.00,234.00,0.00,0.27,0.00,0.00,56.00,130.00,0.00,0.00,0.00,2.00,0.00,0.00,585.00,71.50,NULL,NULL,NULL,NULL,'营养全面，蛋白质优质','一般人群','高胆固醇患者应适量','鸡子','jidan','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:22:43','2025-12-03 08:22:43'),(29,'MILK_001','纯牛奶','Milk',45,100.00,54.00,3.00,3.20,3.40,0.00,24.00,0.00,0.14,0.00,0.00,104.00,73.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,87.50,NULL,NULL,NULL,NULL,'富含优质蛋白和钙','一般人群,儿童,老人',NULL,'鲜奶','chuniunai','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:22:43','2025-12-03 08:22:43'),(30,'YOGURT_001','酸奶','Yogurt',46,100.00,72.00,2.50,2.70,9.30,0.00,26.00,0.00,0.15,0.00,0.00,118.00,85.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,84.40,NULL,NULL,NULL,NULL,'促进消化，富含益生菌','一般人群,便秘患者',NULL,'乳酪,优格','suannai','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:22:43','2025-12-03 08:22:43');
/*!40000 ALTER TABLE `food_nutrition` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `food_portion_reference`
--

DROP TABLE IF EXISTS `food_portion_reference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `food_portion_reference` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `food_id` bigint NOT NULL COMMENT '食材ID',
  `portion_name` varchar(100) NOT NULL COMMENT '份量名称（如：一个、一碗、一份）',
  `portion_weight` decimal(10,2) NOT NULL COMMENT '份量重量（g）',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_food_id` (`food_id`),
  CONSTRAINT `food_portion_reference_ibfk_1` FOREIGN KEY (`food_id`) REFERENCES `food_nutrition` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='食材份量参考表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `food_portion_reference`
--

LOCK TABLES `food_portion_reference` WRITE;
/*!40000 ALTER TABLE `food_portion_reference` DISABLE KEYS */;
/*!40000 ALTER TABLE `food_portion_reference` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `food_records`
--

DROP TABLE IF EXISTS `food_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `food_records` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `meal_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '餐次类型：BREAKFAST/LUNCH/DINNER/SNACK',
  `food_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '食物名称',
  `photo_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '食物照片URL',
  `portion` decimal(8,2) DEFAULT NULL COMMENT '份量（克）',
  `calories` decimal(8,2) DEFAULT NULL COMMENT '卡路里（千卡）',
  `protein` decimal(8,2) DEFAULT NULL COMMENT '蛋白质（克）',
  `carbohydrates` decimal(8,2) DEFAULT NULL COMMENT '碳水化合物（克）',
  `fat` decimal(8,2) DEFAULT NULL COMMENT '脂肪（克）',
  `fiber` decimal(8,2) DEFAULT NULL COMMENT '纤维（克）',
  `record_time` datetime NOT NULL COMMENT '记录时间',
  `notes` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_record_time` (`record_time`),
  KEY `idx_user_meal` (`user_id`,`meal_type`),
  KEY `idx_user_time` (`user_id`,`record_time`),
  CONSTRAINT `fk_food_record_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='饮食记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `food_records`
--

LOCK TABLES `food_records` WRITE;
/*!40000 ALTER TABLE `food_records` DISABLE KEYS */;
/*!40000 ALTER TABLE `food_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `food_search_log`
--

DROP TABLE IF EXISTS `food_search_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `food_search_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `search_keyword` varchar(200) NOT NULL COMMENT '搜索关键词',
  `result_count` int DEFAULT '0' COMMENT '结果数量',
  `clicked_food_id` bigint DEFAULT NULL COMMENT '点击的食材ID',
  `search_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_search_keyword` (`search_keyword`),
  KEY `idx_search_time` (`search_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='食材搜索日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `food_search_log`
--

LOCK TABLES `food_search_log` WRITE;
/*!40000 ALTER TABLE `food_search_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `food_search_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `growth_records`
--

DROP TABLE IF EXISTS `growth_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `growth_records` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `member_id` bigint NOT NULL COMMENT '会员ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `growth_value` int NOT NULL COMMENT '成长值',
  `growth_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '成长值类型',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `related_id` bigint DEFAULT NULL COMMENT '关联业务ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_member_id` (`member_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_growth_type` (`growth_type`),
  CONSTRAINT `growth_records_ibfk_1` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`) ON DELETE CASCADE,
  CONSTRAINT `growth_records_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成长值记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `growth_records`
--

LOCK TABLES `growth_records` WRITE;
/*!40000 ALTER TABLE `growth_records` DISABLE KEYS */;
INSERT INTO `growth_records` VALUES (1,2,1,10,'DAILY_LOGIN','每日登录奖励',NULL,'2025-11-03 07:08:48'),(2,2,1,5,'AI_CHAT','完成AI咨询',NULL,'2025-11-03 07:08:48'),(3,2,1,10,'DAILY_LOGIN','每日登录奖励',NULL,'2025-11-05 07:08:48'),(4,2,1,10,'DAILY_LOGIN','每日登录奖励',NULL,'2025-11-07 07:08:48'),(5,2,1,5,'AI_CHAT','完成AI咨询',NULL,'2025-11-07 07:08:48'),(6,2,1,10,'DAILY_LOGIN','每日登录奖励',NULL,'2025-11-08 07:08:48'),(7,2,1,50,'INVITE_REGISTER','邀请好友注册',NULL,'2025-11-08 07:08:48'),(8,2,1,10,'DAILY_LOGIN','每日登录奖励',NULL,'2025-11-10 07:08:48'),(9,2,1,10,'DAILY_LOGIN','每日登录奖励',NULL,'2025-11-11 07:08:48'),(10,2,1,5,'AI_CHAT','完成AI咨询',NULL,'2025-11-11 07:08:48'),(11,2,1,10,'DAILY_LOGIN','每日登录奖励',NULL,'2025-11-13 07:08:48'),(12,2,1,5,'FOOD_RECORD','添加饮食记录',NULL,'2025-11-13 07:08:48'),(13,2,1,10,'DAILY_LOGIN','每日登录奖励',NULL,'2025-11-15 07:08:48'),(14,2,1,5,'AI_CHAT','完成AI咨询',NULL,'2025-11-15 07:08:48'),(15,2,1,10,'DAILY_LOGIN','每日登录奖励',NULL,'2025-11-17 07:08:48'),(16,2,1,10,'DAILY_LOGIN','每日登录奖励',NULL,'2025-11-18 07:08:48'),(17,2,1,50,'INVITE_REGISTER','邀请好友注册',NULL,'2025-11-18 07:08:48'),(18,2,1,10,'DAILY_LOGIN','每日登录奖励',NULL,'2025-11-19 07:08:48'),(19,2,1,5,'AI_CHAT','完成AI咨询',NULL,'2025-11-19 07:08:48'),(20,2,1,10,'DAILY_LOGIN','每日登录奖励',NULL,'2025-11-21 07:08:48'),(21,2,1,5,'FOOD_RECORD','添加饮食记录',NULL,'2025-11-21 07:08:48'),(22,2,1,10,'DAILY_LOGIN','每日登录奖励',NULL,'2025-11-23 07:08:48'),(23,2,1,10,'DAILY_LOGIN','每日登录奖励',NULL,'2025-11-25 07:08:48'),(24,2,1,5,'AI_CHAT','完成AI咨询',NULL,'2025-11-25 07:08:48'),(25,2,1,10,'DAILY_LOGIN','每日登录奖励',NULL,'2025-11-26 07:08:48'),(26,2,1,50,'INVITE_ACTIVE','邀请好友激活',NULL,'2025-11-26 07:08:48'),(27,2,1,10,'DAILY_LOGIN','每日登录奖励',NULL,'2025-11-28 07:08:48'),(28,2,1,5,'AI_CHAT','完成AI咨询',NULL,'2025-11-28 07:08:48'),(29,2,1,10,'DAILY_LOGIN','每日登录奖励',NULL,'2025-11-29 07:08:48'),(30,2,1,10,'DAILY_LOGIN','每日登录奖励',NULL,'2025-11-30 07:08:48'),(31,2,1,5,'FOOD_RECORD','添加饮食记录',NULL,'2025-11-30 07:08:48'),(32,2,1,10,'DAILY_LOGIN','每日登录奖励',NULL,'2025-12-01 07:08:48'),(33,2,1,5,'AI_CHAT','完成AI咨询',NULL,'2025-12-01 07:08:48'),(34,2,1,10,'DAILY_LOGIN','每日登录奖励',NULL,'2025-12-02 07:08:48'),(35,2,1,10,'DAILY_LOGIN','每日登录奖励',NULL,'2025-12-03 07:08:48'),(36,2,1,5,'AI_CHAT','完成AI咨询',NULL,'2025-12-03 07:08:48'),(37,2,1,0,'LEVEL_UP','升级到青铜会员',NULL,'2025-12-03 07:31:15');
/*!40000 ALTER TABLE `growth_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `invitations`
--

DROP TABLE IF EXISTS `invitations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `invitations` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '邀请ID',
  `inviter_id` bigint NOT NULL COMMENT '邀请人ID',
  `invitee_id` bigint DEFAULT NULL COMMENT '被邀请人ID',
  `invitation_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '邀请码',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING' COMMENT '状态: PENDING, ACCEPTED, EXPIRED',
  `invited_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '邀请时间',
  `accepted_at` timestamp NULL DEFAULT NULL COMMENT '接受时间',
  `reward_growth` int DEFAULT '0' COMMENT '奖励成长值',
  `is_rewarded` tinyint(1) DEFAULT '0' COMMENT '是否已发放奖励',
  PRIMARY KEY (`id`),
  KEY `idx_inviter_id` (`inviter_id`),
  KEY `idx_invitee_id` (`invitee_id`),
  KEY `idx_invitation_code` (`invitation_code`),
  KEY `idx_status` (`status`),
  CONSTRAINT `invitations_ibfk_1` FOREIGN KEY (`inviter_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `invitations_ibfk_2` FOREIGN KEY (`invitee_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `invitations`
--

LOCK TABLES `invitations` WRITE;
/*!40000 ALTER TABLE `invitations` DISABLE KEYS */;
INSERT INTO `invitations` VALUES (1,1,3,'INV000001dfd585','ACCEPTED','2025-11-08 07:08:48','2025-11-08 07:08:48',50,1),(2,1,4,'INV000001dfd585','ACCEPTED','2025-11-18 07:08:48','2025-11-18 07:08:48',50,1),(3,1,5,'INV000001dfd585','ACCEPTED','2025-11-26 07:08:48','2025-11-26 07:08:48',50,1);
/*!40000 ALTER TABLE `invitations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `member_levels`
--

DROP TABLE IF EXISTS `member_levels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_levels` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '等级ID',
  `level_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '等级代码',
  `level_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '等级名称',
  `level_order` int NOT NULL COMMENT '等级顺序',
  `growth_required` int NOT NULL COMMENT '所需成长值',
  `benefits` json DEFAULT NULL COMMENT '权益配置',
  `icon_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '等级图标',
  `color` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '等级颜色',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `level_code` (`level_code`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员等级配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `member_levels`
--

LOCK TABLES `member_levels` WRITE;
/*!40000 ALTER TABLE `member_levels` DISABLE KEYS */;
INSERT INTO `member_levels` VALUES (1,'ROOKIE','新手会员',1,0,'{\"features\": [\"基础营养记录\", \"AI咨询(3次/天)\", \"基础数据分析\"], \"maxAiQueries\": 3, \"maxFoodRecords\": 10}','/icons/level-rookie.png','#95a5a6','2025-12-03 04:09:27','2025-12-03 04:09:27'),(2,'BRONZE','青铜会员',2,100,'{\"features\": [\"营养记录无限\", \"AI咨询(10次/天)\", \"高级数据分析\", \"自定义目标\"], \"maxAiQueries\": 10, \"maxFoodRecords\": -1}','/icons/level-bronze.png','#cd7f32','2025-12-03 04:09:27','2025-12-03 04:09:27'),(3,'SILVER','白银会员',3,500,'{\"features\": [\"所有青铜权益\", \"AI咨询(30次/天)\", \"专属营养报告\", \"优先客服\"], \"maxAiQueries\": 30, \"maxFoodRecords\": -1}','/icons/level-silver.png','#c0c0c0','2025-12-03 04:09:27','2025-12-03 04:09:27'),(4,'GOLD','黄金会员',4,2000,'{\"features\": [\"所有白银权益\", \"AI咨询无限\", \"个性化食谱\", \"健康顾问\", \"数据导出\"], \"maxAiQueries\": -1, \"maxFoodRecords\": -1}','/icons/level-gold.png','#ffd700','2025-12-03 04:09:27','2025-12-03 04:09:27'),(5,'PLATINUM','铂金会员',5,5000,'{\"features\": [\"所有黄金权益\", \"专属营养师\", \"线下活动\", \"合作商家折扣\", \"终身成长值加成\"], \"growthBonus\": 1.5, \"maxAiQueries\": -1, \"maxFoodRecords\": -1}','/icons/level-platinum.png','#e5e4e2','2025-12-03 04:09:27','2025-12-03 04:09:27');
/*!40000 ALTER TABLE `member_levels` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `members`
--

DROP TABLE IF EXISTS `members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `members` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会员ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `level_id` bigint NOT NULL COMMENT '当前等级ID',
  `total_growth` int DEFAULT '0' COMMENT '总成长值',
  `current_growth` int DEFAULT '0' COMMENT '当前等级成长值',
  `invitation_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '邀请码',
  `invited_by` bigint DEFAULT NULL COMMENT '邀请人ID',
  `invitation_count` int DEFAULT '0' COMMENT '邀请人数',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否激活',
  `activated_at` timestamp NULL DEFAULT NULL COMMENT '激活时间',
  `expire_at` timestamp NULL DEFAULT NULL COMMENT '过期时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  UNIQUE KEY `invitation_code` (`invitation_code`),
  KEY `level_id` (`level_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_invitation_code` (`invitation_code`),
  KEY `idx_invited_by` (`invited_by`),
  CONSTRAINT `members_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `members_ibfk_2` FOREIGN KEY (`level_id`) REFERENCES `member_levels` (`id`),
  CONSTRAINT `members_ibfk_3` FOREIGN KEY (`invited_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `members`
--

LOCK TABLES `members` WRITE;
/*!40000 ALTER TABLE `members` DISABLE KEYS */;
INSERT INTO `members` VALUES (1,2,1,0,0,'INV00000224d36a',NULL,0,1,NULL,NULL,'2025-12-03 04:09:27','2025-12-03 04:09:27'),(2,1,2,420,320,'INV000001dfd585',NULL,3,1,NULL,NULL,'2025-12-03 04:09:27','2025-12-03 07:31:15'),(4,3,1,0,0,'INV000003T1',1,0,1,'2025-11-08 07:08:48',NULL,'2025-11-08 07:08:48','2025-12-03 07:08:48'),(5,4,1,0,0,'INV000004T2',1,0,1,'2025-11-18 07:08:48',NULL,'2025-11-18 07:08:48','2025-12-03 07:08:48'),(6,5,1,0,0,'INV000005T3',1,0,1,'2025-11-26 07:08:48',NULL,'2025-11-26 07:08:48','2025-12-03 07:08:48');
/*!40000 ALTER TABLE `members` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `memberships`
--

DROP TABLE IF EXISTS `memberships`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `memberships` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会员ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `level` enum('free','silver','gold') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'free' COMMENT '会员等级',
  `growth_points` int NOT NULL DEFAULT '0' COMMENT '成长值',
  `expire_date` date DEFAULT NULL COMMENT '会员到期日期',
  `ai_quota_used` int NOT NULL DEFAULT '0' COMMENT '今日已使用AI次数',
  `ai_quota_total` int NOT NULL DEFAULT '3' COMMENT '今日AI总配额',
  `last_quota_reset_date` date DEFAULT NULL COMMENT '配额重置日期',
  `invite_code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邀请码',
  `invited_by` bigint DEFAULT NULL COMMENT '邀请人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_invite_code` (`invite_code`),
  KEY `idx_level` (`level`),
  KEY `idx_growth_points` (`growth_points`),
  KEY `idx_invited_by` (`invited_by`),
  CONSTRAINT `fk_membership_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `memberships`
--

LOCK TABLES `memberships` WRITE;
/*!40000 ALTER TABLE `memberships` DISABLE KEYS */;
/*!40000 ALTER TABLE `memberships` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nutrition_data`
--

DROP TABLE IF EXISTS `nutrition_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nutrition_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '食物ID',
  `food_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '食物名称',
  `food_name_en` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '英文名称',
  `category` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '食物分类',
  `serving_size` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标准份量',
  `calories` decimal(8,2) NOT NULL COMMENT '热量（kcal/100g）',
  `protein` decimal(8,2) NOT NULL COMMENT '蛋白质（g/100g）',
  `carbs` decimal(8,2) NOT NULL COMMENT '碳水化合物（g/100g）',
  `fat` decimal(8,2) NOT NULL COMMENT '脂肪（g/100g）',
  `fiber` decimal(8,2) DEFAULT NULL COMMENT '膳食纤维（g/100g）',
  `sodium` decimal(8,2) DEFAULT NULL COMMENT '钠（mg/100g）',
  `vitamin_c` decimal(8,2) DEFAULT NULL COMMENT '维生素C（mg/100g）',
  `calcium` decimal(8,2) DEFAULT NULL COMMENT '钙（mg/100g）',
  `iron` decimal(8,2) DEFAULT NULL COMMENT '铁（mg/100g）',
  `image_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '食物图片URL',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '食物描述',
  `common_allergens` json DEFAULT NULL COMMENT '常见过敏源（JSON数组）',
  `source` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'manual' COMMENT '数据来源',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_food_name` (`food_name`),
  KEY `idx_category` (`category`),
  KEY `idx_calories` (`calories`),
  FULLTEXT KEY `ft_food_name` (`food_name`,`food_name_en`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食物营养数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nutrition_data`
--

LOCK TABLES `nutrition_data` WRITE;
/*!40000 ALTER TABLE `nutrition_data` DISABLE KEYS */;
INSERT INTO `nutrition_data` VALUES (1,'米饭','Rice','谷物类','100g',116.00,2.60,25.90,0.30,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'manual','2025-12-02 15:51:27','2025-12-02 15:51:27'),(2,'全麦面包','Whole Wheat Bread','谷物类','100g',247.00,13.20,41.30,3.40,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'manual','2025-12-02 15:51:27','2025-12-02 15:51:27'),(3,'鸡胸肉','Chicken Breast','肉类','100g',165.00,31.00,0.00,3.60,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'manual','2025-12-02 15:51:27','2025-12-02 15:51:27'),(4,'牛肉','Beef','肉类','100g',250.00,26.00,0.00,15.00,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'manual','2025-12-02 15:51:27','2025-12-02 15:51:27'),(5,'三文鱼','Salmon','水产类','100g',208.00,20.00,0.00,13.00,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'manual','2025-12-02 15:51:27','2025-12-02 15:51:27'),(6,'鸡蛋','Egg','蛋奶类','1个（50g）',72.00,6.30,0.40,4.80,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'manual','2025-12-02 15:51:27','2025-12-02 15:51:27'),(7,'牛奶','Milk','蛋奶类','100ml',54.00,3.00,5.00,3.20,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'manual','2025-12-02 15:51:27','2025-12-02 15:51:27'),(8,'西蓝花','Broccoli','蔬菜类','100g',34.00,2.80,7.00,0.40,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'manual','2025-12-02 15:51:27','2025-12-02 15:51:27'),(9,'苹果','Apple','水果类','100g',52.00,0.30,14.00,0.20,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'manual','2025-12-02 15:51:27','2025-12-02 15:51:27'),(10,'香蕉','Banana','水果类','100g',89.00,1.10,23.00,0.30,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'manual','2025-12-02 15:51:27','2025-12-02 15:51:27'),(11,'豆腐','Tofu','豆制品','100g',76.00,8.10,1.90,4.20,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'manual','2025-12-02 15:51:27','2025-12-02 15:51:27'),(12,'核桃','Walnut','坚果类','100g',654.00,15.20,13.70,65.20,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'manual','2025-12-02 15:51:27','2025-12-02 15:51:27');
/*!40000 ALTER TABLE `nutrition_data` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `operation_logs`
--

DROP TABLE IF EXISTS `operation_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `operation_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `operation` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作类型',
  `module` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模块名称',
  `method` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '方法名',
  `params` text COLLATE utf8mb4_unicode_ci COMMENT '请求参数',
  `result` text COLLATE utf8mb4_unicode_ci COMMENT '返回结果',
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'User Agent',
  `status` enum('success','failure') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'success' COMMENT '操作状态',
  `error_message` text COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
  `execution_time` int DEFAULT NULL COMMENT '执行时间（ms）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_operation` (`operation`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `operation_logs`
--

LOCK TABLES `operation_logs` WRITE;
/*!40000 ALTER TABLE `operation_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `operation_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_configs`
--

DROP TABLE IF EXISTS `system_configs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_configs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键',
  `config_value` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置值',
  `config_type` enum('string','number','boolean','json') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'string' COMMENT '配置类型',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配置描述',
  `is_public` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否公开',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_configs`
--

LOCK TABLES `system_configs` WRITE;
/*!40000 ALTER TABLE `system_configs` DISABLE KEYS */;
INSERT INTO `system_configs` VALUES (1,'system.name','AI健康饮食规划助手','string','系统名称',1,'2025-12-02 15:51:27','2025-12-02 15:51:27'),(2,'system.version','1.0.0','string','系统版本',1,'2025-12-02 15:51:27','2025-12-02 15:51:27'),(3,'ai.daily_free_quota','3','number','普通用户每日AI咨询次数',1,'2025-12-02 15:51:27','2025-12-02 15:51:27'),(4,'ai.silver_quota','10','number','白银会员每日AI咨询次数',1,'2025-12-02 15:51:27','2025-12-02 15:51:27'),(5,'ai.gold_quota','20','number','黄金会员每日AI咨询次数',1,'2025-12-02 15:51:27','2025-12-02 15:51:27'),(6,'growth.upgrade_silver','100','number','升级白银会员所需成长值',1,'2025-12-02 15:51:27','2025-12-02 15:51:27'),(7,'growth.upgrade_gold','300','number','升级黄金会员所需成长值',1,'2025-12-02 15:51:27','2025-12-02 15:51:27');
/*!40000 ALTER TABLE `system_configs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_profiles`
--

DROP TABLE IF EXISTS `user_profiles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_profiles` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '档案ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `gender` enum('male','female','other') COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '性别',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `height` decimal(5,2) DEFAULT NULL COMMENT '身高（cm）',
  `weight` decimal(5,2) DEFAULT NULL COMMENT '体重（kg）',
  `bmi` decimal(5,2) DEFAULT NULL COMMENT 'BMI指数',
  `health_goal` enum('lose_weight','gain_muscle','maintain','improve_health') COLLATE utf8mb4_unicode_ci DEFAULT 'maintain' COMMENT '健康目标',
  `activity_level` enum('sedentary','light','moderate','active','very_active') COLLATE utf8mb4_unicode_ci DEFAULT 'moderate' COMMENT '活动水平',
  `allergies` json DEFAULT NULL COMMENT '过敏源列表（JSON数组）',
  `dietary_restrictions` json DEFAULT NULL COMMENT '饮食限制（JSON数组）',
  `medical_conditions` text COLLATE utf8mb4_unicode_ci COMMENT '健康状况说明',
  `daily_calorie_target` int DEFAULT NULL COMMENT '每日热量目标（kcal）',
  `daily_protein_target` int DEFAULT NULL COMMENT '每日蛋白质目标（g）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_health_goal` (`health_goal`),
  KEY `idx_bmi` (`bmi`),
  CONSTRAINT `fk_profile_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户健康档案表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_profiles`
--

LOCK TABLES `user_profiles` WRITE;
/*!40000 ALTER TABLE `user_profiles` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_profiles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（BCrypt加密）',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像URL',
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '昵称',
  `role` enum('USER','ADMIN','SUPER_ADMIN') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'USER',
  `status` enum('ACTIVE','INACTIVE','BANNED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最后登录IP',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_role` (`role`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户基础表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'admin','$2a$10$N7TG/HHY9rfCcY5/eX6rDu6BN.eFsSwsC2dvwwJQqb/e4UPpjp02a','admin@nutriai.com',NULL,'http://localhost:8080/api/uploads/avatars/avatar_1e5c41e4-921e-4284-a421-f004055bb85a.jpg','admin','SUPER_ADMIN','ACTIVE','2025-12-03 15:38:57','0:0:0:0:0:0:0:1','2025-12-02 15:51:27','2025-12-03 15:38:57'),(2,'Test','$2a$10$/1pN0SqTPUHocMLkstR6VeYYfURqzLoH1ZTqduYxnoi1G8sBEtIHu','Test@qq.com',NULL,NULL,'Test','USER','ACTIVE','2025-12-02 20:06:15','0:0:0:0:0:0:0:1','2025-12-02 19:55:43','2025-12-02 20:06:15'),(3,'test_user1','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z58QRVa7G4G7.MU8QlHb14m6','test1@example.com',NULL,NULL,NULL,'USER','ACTIVE',NULL,NULL,'2025-11-08 15:08:48','2025-11-08 15:08:48'),(4,'test_user2','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z58QRVa7G4G7.MU8QlHb14m6','test2@example.com',NULL,NULL,NULL,'USER','ACTIVE',NULL,NULL,'2025-11-18 15:08:48','2025-11-18 15:08:48'),(5,'test_user3','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z58QRVa7G4G7.MU8QlHb14m6','test3@example.com',NULL,NULL,NULL,'USER','ACTIVE',NULL,NULL,'2025-11-26 15:08:48','2025-11-26 15:08:48');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-12-03 16:24:05
