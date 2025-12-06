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
-- Table structure for table `admin_operation_log`
--

DROP TABLE IF EXISTS `admin_operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `admin_id` bigint NOT NULL COMMENT '管理员ID',
  `operation_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作类型',
  `operation_desc` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作描述',
  `target_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标类型：user/config/announcement',
  `target_id` bigint DEFAULT NULL COMMENT '目标ID',
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户代理',
  `request_params` text COLLATE utf8mb4_unicode_ci COMMENT '请求参数',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_admin_id` (`admin_id`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admin_operation_log`
--

LOCK TABLES `admin_operation_log` WRITE;
/*!40000 ALTER TABLE `admin_operation_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `admin_operation_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_chat_favorite`
--

DROP TABLE IF EXISTS `ai_chat_favorite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_favorite` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `message_content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '收藏的消息内容',
  `message_role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息角色：user/assistant',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天收藏表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_chat_favorite`
--

LOCK TABLES `ai_chat_favorite` WRITE;
/*!40000 ALTER TABLE `ai_chat_favorite` DISABLE KEYS */;
INSERT INTO `ai_chat_favorite` VALUES (1,1,'你好！有什么可以帮助你的吗？','assistant','2025-12-05 15:03:28');
/*!40000 ALTER TABLE `ai_chat_favorite` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_chat_history`
--

DROP TABLE IF EXISTS `ai_chat_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '对话标题',
  `messages` json NOT NULL COMMENT '消息列表（JSON格式）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天历史记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_chat_history`
--

LOCK TABLES `ai_chat_history` WRITE;
/*!40000 ALTER TABLE `ai_chat_history` DISABLE KEYS */;
INSERT INTO `ai_chat_history` VALUES (7,1,'你好','[{\"id\": \"msg_1764946203966_fp8yhdmlz\", \"file\": null, \"role\": \"user\", \"content\": \"你好\", \"timestamp\": 1764946203966}, {\"id\": \"msg_1764946203966_ynfvkgl5q\", \"role\": \"assistant\", \"content\": \"你好！有什么我可以帮助你的吗？\", \"loading\": false, \"timestamp\": 1764946203966}, {\"id\": \"msg_1764946209022_1clzyvs4q\", \"file\": null, \"role\": \"user\", \"content\": \"你好\", \"timestamp\": 1764946209022}, {\"id\": \"msg_1764946420497_dv14pz5mu\", \"file\": null, \"role\": \"user\", \"content\": \"你好\", \"timestamp\": 1764946420497}, {\"id\": \"msg_1764946420497_bftou3m4w\", \"role\": \"assistant\", \"content\": \"你好！有什么可以帮到你的吗？如果你有任何问题或需要什么信息，请告诉我。\", \"loading\": false, \"timestamp\": 1764946420497}]','2025-12-05 14:55:46','2025-12-05 15:00:23'),(8,1,'你好','[{\"id\": \"msg_1764946830330_1aeqxurt9\", \"file\": null, \"role\": \"user\", \"content\": \"你好\", \"timestamp\": 1764946830330}, {\"id\": \"msg_1764946830330_76j1x1vfx\", \"role\": \"assistant\", \"content\": \"你好！有什么可以帮助你的吗？\", \"loading\": false, \"favorite\": true, \"timestamp\": 1764946830330, \"favoriteId\": 1}]','2025-12-05 15:00:37','2025-12-05 15:03:31'),(9,1,'你好','[{\"id\": \"msg_1764986520768_3si8m390k\", \"file\": null, \"role\": \"user\", \"content\": \"你好\", \"timestamp\": 1764986520768}, {\"id\": \"msg_1764986520768_kfy5r3tv3\", \"role\": \"assistant\", \"content\": \"你好！有什么我可以帮助你的吗？\", \"loading\": false, \"timestamp\": 1764986520768}, {\"id\": \"msg_1764987303483_qunswgsor\", \"file\": null, \"role\": \"user\", \"content\": \"你能帮我做什么？\", \"timestamp\": 1764987303483}, {\"id\": \"msg_1764987303483_o2jgu4g6c\", \"role\": \"assistant\", \"content\": \"我很乐意帮助您！作为您的助手，我可以提供多种服务，包括但不限于：\\n\\n1. **信息查询**：提供天气预报、新闻资讯、历史事实等信息。\\n2. **学习辅导**：解答数学题、科学问题或提供语言学习的帮助。\\n3. **生活建议**：给出旅行规划建议、健康饮食推荐或是日常生活的实用小贴士。\\n4. **技术支持**：解答关于电脑软件使用、编程问题等方面的技术疑问。\\n5. **娱乐休闲**：分享笑话、故事、电影评论等，让您的闲暇时光更加丰富多彩。\\n6. **写作辅助**：帮您构思文章结构、润色文字或提供创意灵感。\\n7. **职业发展**：提供简历修改建议、面试技巧以及职业规划指导。\\n\\n如果您有具体的需求或者想了解更详细的信息，请告诉我，我会尽力为您提供帮助！\", \"loading\": false, \"timestamp\": 1764987303483}]','2025-12-06 02:02:07','2025-12-06 02:17:27');
/*!40000 ALTER TABLE `ai_chat_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_chat_log`
--

DROP TABLE IF EXISTS `ai_chat_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `session_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会话ID',
  `user_message` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户消息',
  `ai_response` text COLLATE utf8mb4_unicode_ci COMMENT 'AI回复',
  `model` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'AI模型',
  `tokens_used` int DEFAULT NULL COMMENT '使用的token数',
  `response_time` int DEFAULT NULL COMMENT '响应时间(ms)',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '状态：success/error',
  `error_message` text COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_chat_log`
--

LOCK TABLES `ai_chat_log` WRITE;
/*!40000 ALTER TABLE `ai_chat_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_chat_log` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Table structure for table `diet_plan_history`
--

DROP TABLE IF EXISTS `diet_plan_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `diet_plan_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_id` varchar(50) NOT NULL COMMENT '计划ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `title` varchar(200) NOT NULL COMMENT '计划标题',
  `days` int NOT NULL COMMENT '计划天数',
  `goal` varchar(50) DEFAULT NULL COMMENT '目标',
  `markdown_content` longtext COMMENT 'Markdown内容',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `plan_id` (`plan_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_plan_id` (`plan_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='饮食计划历史记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `diet_plan_history`
--

LOCK TABLES `diet_plan_history` WRITE;
/*!40000 ALTER TABLE `diet_plan_history` DISABLE KEYS */;
INSERT INTO `diet_plan_history` VALUES (5,'plan_1764922168756',1,'健康计划 - 7天',7,'维持健康','# 健康计划 - 7天\n\n## 1. 计划概述\n维持健康计划\n\n## 2. 营养目标\n- 每日总热量：2000 kcal\n- 蛋白质、碳水、脂肪比例合理分配\n\n## 3. 每日详细计划\n\n## 第1天 - 2025-12-05\n### 早餐\n- 燕麦：50克（干重），约含180kcal，富含膳食纤维、维生素B群。建议用水或低脂牛奶煮制，可加入少量蜂蜜增加风味。\n- 新鲜菠菜：100克，大约含有34kcal，提供丰富的铁质和钙质。推荐快速焯水后凉拌食用。\n- 土豆：小个头1个（约100g），约含77kca，是钾的良好来源。可以切成薄片用少量橄榄油煎至金黄。\n\n### 午餐\n- 糙米：100克（干重），煮熟后约含230kcal，比白米饭更富含膳食纤维。提前浸泡以减少烹饪时间。\n- 西红柿：中等大小2个，总共约含30kcal，富含抗氧化剂如番茄红素。建议切块后与少许盐一起炒制。\n- 大白菜：200克，生吃或轻微烹调均可，总热量约为14kcal，能够补充水分及维生素C。\n\n### 晚餐\n- 红薯：一个中等大小（约200g），约含100kcal，富含β-胡萝卜素。蒸煮是最好的准备方式。\n- 小麦粉（标准粉）：用于制作饺子皮，用量根据个人喜好调整，这里假设为100克干面粉，约含364kcal。注意控制油脂使用量。\n- 菠菜：作为馅料之一，取用新鲜菠菜100克，同早餐描述。\n\n### 加餐\n- 玉米（鲜）：一根中等大小的玉米棒，去壳后约含140kcal，含有多种维生素和矿物质。可以直接水煮或者烤制。\n- 苹果：一个中等大小的苹果，约含95kcal，有助于满足甜食欲望的同时提供纤维素。\n- 坚果混合物：一小把（约30克），包括杏仁、核桃等，总计约160kcal，提供优质脂肪和蛋白质。\n\n### 营养总计\n此日饮食计划总热量约为2000kcal左右，具体数值可能因食材个体差异而有所变化。该计划旨在通过均衡摄入各类营养素来维持身体健康，并且适合进行中度运动的人群。请注意保持足够的水分摄入，并根据实际情况适当调整食物分量。\n\n## 第2天 - 2025-12-06\n### 早餐\n- 燕麦：30克（约1杯），提供约100卡路里、4克蛋白质、1.5克脂肪、17克碳水化合物。富含膳食纤维，有助于促进肠胃蠕动。\n- 新鲜玉米粒：50克（约半杯），提供约80卡路里、2克蛋白质、1克脂肪、19克碳水化合物。含有丰富的维生素B群和钾元素。\n- 西红柿：1个中等大小，约含30卡路里、1克蛋白质、不到1克脂肪、7克碳水化合物。西红柿富含番茄红素及多种维生素。\n\n**烹饪提示**：将燕麦用水或牛奶煮沸后小火慢炖至软糯；玉米可以蒸熟后剥粒加入燕麦粥内；西红柿切片作为凉拌菜食用。\n\n### 午餐\n- 糙米饭：100克（干重），提供约350卡路里、7克蛋白质、1.5克脂肪、78克碳水化合物。糙米比白米更富含纤维和其他营养物质。\n- 大白菜：150克（生重），炒制后约为100克，提供约15卡路里、不到1克蛋白质、几乎无脂肪、3克碳水化合物。大白菜是低热量高水分的蔬菜。\n- 土豆：100克（去皮后），蒸熟后大约提供80卡路里、1.5克蛋白质、0.1克脂肪、18克碳水化合物。土豆是一种优质的碳水来源。\n\n**烹饪提示**：糙米提前浸泡几小时后再用电饭煲煮制；大白菜切丝与少量油盐快速翻炒；土豆切块蒸熟即可。\n\n### 晚餐\n- 小麦面条：干面50克，煮熟后约为150克，提供约150卡路里、5克蛋白质、不到1克脂肪、30克碳水化合物。选择全麦面粉制作的面条更加健康。\n- 菠菜：100克（生重），烫过后约为50克，提供约20卡路里、1克蛋白质、几乎无脂肪、4克碳水化合物。菠菜富含铁质及多种矿物质。\n- 红薯：1个小号（约100克），烤熟后提供约100卡路里、2克蛋白质、几乎无脂肪、24克碳水化合物。红薯中的β-胡萝卜素对人体有益。\n\n**烹饪提示**：面条用清水煮至八分熟捞出过冷水；菠菜稍微焯水去除草酸；红薯洗净后直接放入预热好的烤箱中低温慢烤。\n\n### 加餐\n- 苹果：1个中等大小，约含95卡路里、不到1克蛋白质、几乎无脂肪、25克碳水化合物。苹果富含抗氧化剂。\n- 坚果混合包（如杏仁+核桃）：总共不超过30克，根据种类不同能量值在150-200卡之间波动。坚果富含健康的单不饱和脂肪酸以及蛋白质。\n\n**烹饪提示**：苹果洗净后直接食用；坚果建议选择未加工过的原味产品。\n\n### 营养总计\n- 总热量：约2000kcal\n- 蛋白质：约60g\n- 脂肪：约50g\n- 碳水化合物：约280g\n\n以上饮食计划旨在提供均衡的营养摄入，并且考虑到您的运动量进行了适当的调整。请注意，实际数值可能会因具体食材的选择和个人差异而有所变化，请根据自身情况适当调整。\n\n## 第3天 - 2025-12-07\n### 早餐\n- **燕麦**：50克（约含180卡路里，蛋白质5g，脂肪3g，碳水化合物30g）\n  - 烹饪提示：将燕麦与牛奶或水混合后微波加热2分钟，可加入少量蜂蜜和坚果增加风味。\n- **西红柿**：1个中等大小（约含25卡路里，维生素C丰富）\n  - 烹饪提示：生吃或者做成简单的西红柿沙拉。\n- **鸡蛋**：1个（约含70卡路里，高质量蛋白质来源）\n  - 烹饪提示：煮蛋或煎蛋均可，注意控制油量。\n\n### 午餐\n- **糙米**：100克（未煮前）（约含360卡路里，富含膳食纤维）\n  - 烹饪提示：提前浸泡半小时再用电饭煲煮制。\n- **菠菜**：150克（约含50卡路里，铁质丰富）\n  - 烹饪提示：快速焯水后凉拌，可以加点蒜末提味。\n- **鸡胸肉**：100克（去皮）（约含165卡路里，低脂高蛋白）\n  - 烹饪提示：用橄榄油轻轻煎至两面金黄，撒上适量盐和黑胡椒调味。\n\n### 晚餐\n- **红薯**：1个中等大小（约含100卡路里，富含β-胡萝卜素）\n  - 烹饪提示：整个放入烤箱以180°C烘烤40分钟左右直到变软。\n- **大白菜**：200克（约含30卡路里，水分含量高利于消化）\n  - 烹饪提示：切片后与其他蔬菜一起清炒，少油少盐。\n- **豆腐**：100克（约含70卡路里，植物性优质蛋白）\n  - 烹饪提示：切成小块与蔬菜同炒，或做成汤品。\n\n### 加餐\n- **苹果**：1个（约含95卡路里，富含纤维素）\n  - 不需要特别处理，直接食用即可。\n- **无糖酸奶**：一小杯（约含70卡路里，含有益生菌）\n  - 可以选择原味的，根据个人口味添加少量新鲜水果。\n\n### 营养总计\n- 总热量约为1935卡路里，接近目标值2000kcal。此计划提供了丰富的膳食纤维、蛋白质以及多种维生素矿物质，有助于保持健康状态同时满足日常活动的能量需求。请注意调整食材份量来精确匹配您的具体需求。\n\n## 第4天 - 2025-12-08\n### 早餐\n- **食材**：燕麦 50g、牛奶 200ml、香蕉 1根\n  - **营养数据**：\n    - 燕麦：约190kcal，蛋白质7g，脂肪3g，碳水化合物32g\n    - 牛奶：约105kcal，蛋白质6.5g，脂肪4g，碳水化合物12g\n    - 香蕉：约90kcal，蛋白质1.3g，脂肪0.3g，碳水化合物23g\n  - **烹饪提示**：将燕麦与牛奶混合后微波加热或直接煮沸几分钟，加入切片香蕉即可享用。\n\n### 午餐\n- **食材**：糙米 100g、鸡胸肉 150g、菠菜 100g\n  - **营养数据**：\n    - 糙米：约230kcal，蛋白质5g，脂肪1g，碳水化合物50g\n    - 鸡胸肉（去皮）：约200kcal，蛋白质35g，脂肪3g，碳水化合物0g\n    - 菠菜：约30kcal，蛋白质2.5g，脂肪0.5g，碳水化合物5g\n  - **烹饪提示**：先将糙米洗净蒸熟；鸡胸肉用少许橄榄油煎至两面金黄；菠菜快速焯水去除草酸后拌入少量橄榄油和醋调味。\n\n### 晚餐\n- **食材**：红薯 1个(约150g)、大白菜 100g、西红柿 1个(约150g)\n  - **营养数据**：\n    - 红薯：约100kcal，蛋白质2g，脂肪0.2g，碳水化合物23g\n    - 大白菜：约17kcal，蛋白质1.5g，脂肪0.1g，碳水化合物3.5g\n    - 西红柿：约25kcal，蛋白质1g，脂肪0.2g，碳水化合物5g\n  - **烹饪提示**：红薯可选择烤制或蒸煮；大白菜与西红柿一起清炒，保持蔬菜的原汁原味。\n\n### 加餐\n- **食材**：酸奶 1杯(约200g)、坚果（如杏仁）10g\n  - **营养数据**：\n    - 酸奶：约100kcal，蛋白质6g，脂肪3g，碳水化合物12g\n    - 杏仁：约60kcal，蛋白质2g，脂肪5g，碳水化合物2g\n  - **烹饪提示**：选择无糖酸奶更健康；坚果提前浸泡软化口感更佳。\n\n### 营养总计\n- 总热量：约1987kcal\n- 蛋白质：约75g\n- 脂肪：约25g\n- 碳水化合物：约170g\n\n此计划旨在提供均衡的营养摄入，同时控制总热量在目标范围内。请注意根据个人口味适当调整食谱，并确保全天水分充足。\n\n## 第5天 - 2025-12-09\n\n### 早餐\n- **食材**：燕麦50克、牛奶200毫升、香蕉1根\n  - **营养数据**：约含热量400千卡，蛋白质16克，脂肪10克，碳水化合物60克。\n  - **烹饪提示**：将燕麦与牛奶混合后微波加热或直接用锅煮至软糯，最后加入切片香蕉即可享用。\n\n### 午餐\n- **食材**：糙米100克、鸡胸肉150克、菠菜150克\n  - **营养数据**：大约提供热量600千卡，蛋白质45克，脂肪12克，碳水化合物70克。\n  - **烹饪提示**：先用电饭煲蒸好糙米饭；鸡胸肉提前腌制（可使用少量酱油、姜蒜等调味），然后煎熟；菠菜快速焯水后凉拌食用。\n\n### 晩餐\n- **食材**：红薯1个（约200g）、大白菜300克、豆腐100克\n  - **营养数据**：总热量约为400千卡，含有蛋白质18克，脂肪10克，碳水化合物60克。\n  - **烹饪提示**：红薯洗净后整个放入锅中蒸熟；大白菜切段，与切成小块的豆腐一起炖煮，可以适量添加一些清汤底料增加风味。\n\n### 加餐\n- **食材**：玉米1根（约150g）、苹果1个\n  - **营养数据**：约含热量200千卡，蛋白质3克，脂肪几乎为零，碳水化合物50克。\n  - **烹饪提示**：玉米可以选择生吃或是稍微用水煮一下；苹果洗净直接食用。\n\n### 营养总计\n- **总热量**：约2000千卡\n- **蛋白质**：约82克\n- **脂肪**：约32克\n- **碳水化合物**：约240克\n\n此饮食计划旨在通过均衡搭配各类食物来满足日常所需能量及营养素摄入量，同时考虑到维持健康的目标以及中度运动的需求。请注意根据个人实际情况调整食谱内容。\n\n## 第6天 - 2025-12-10\n### 早餐\n- **燕麦**：50克（生）+ 牛奶200毫升 + 蜂蜜1茶匙\n  - **营养数据**：约含热量300kcal，蛋白质9g，脂肪8g，碳水化合物47g。\n  - **烹饪提示**：将燕麦与牛奶混合，在微波炉中加热至软糯，最后加入蜂蜜调味。\n\n- **香蕉**：1根\n  - **营养数据**：约含热量105kcal，钾元素丰富。\n  - **食用建议**：直接剥皮食用即可。\n\n- **鸡蛋**：1个\n  - **营养数据**：约含热量78kcal，高质量蛋白质来源。\n  - **烹饪方式**：水煮蛋或煎蛋均可。\n\n### 午餐\n- **糙米饭**：1碗（约150克熟饭）\n  - **营养数据**：约含热量200kcal，富含膳食纤维。\n  - **烹饪提示**：提前浸泡糙米数小时后再煮，口感更佳。\n\n- **西红柿炒鸡蛋**：使用鸡蛋2个、西红柿2个\n  - **营养数据**：约含热量250kcal，维生素C含量高。\n  - **烹饪步骤**：先炒鸡蛋盛出备用；再用少量油炒西红柿至出汁后回锅鸡蛋，加盐调味即可。\n\n- **清蒸大白菜**：半棵\n  - **营养数据**：约含热量30kcal，低卡路里蔬菜选择。\n  - **做法**：洗净切片的大白菜放入蒸锅中大火蒸制5分钟左右。\n\n### 晚餐\n- **红薯**：中等大小1个\n  - **营养数据**：约含热量100kcal，含有丰富的β-胡萝卜素。\n  - **烹饪方法**：可直接用水煮或者烤箱烘烤。\n\n- **菠菜豆腐汤**：菠菜1把、嫩豆腐半盒\n  - **营养数据**：约含热量150kcal，提供优质植物蛋白及铁质。\n  - **制作指南**：清水烧开后加入切成小块的豆腐稍煮片刻，随后加入洗净的菠菜快速烫熟，撒上少许盐调味。\n\n- **土豆丝**：土豆1个\n  - **营养数据**：约含热量100kcal，提供适量碳水化合物。\n  - **准备流程**：土豆去皮切成细丝，用冷水浸泡去除多余淀粉，然后快火翻炒至熟透。\n\n### 加餐\n- **苹果**：1个\n  - **营养数据**：约含热量95kcal，富含多种维生素和矿物质。\n  - **食用建议**：清洗干净后直接食用。\n\n- **坚果混合**：一小把（约30克）\n  - **营养数据**：约含热量180kcal，健康脂肪来源。\n  - **注意**：尽量选择未加工过的原味坚果。\n\n### 营养总计\n- 总热量约为2000kcal左右，符合目标要求。此计划提供了均衡的三大营养素比例，并且包含了足够的膳食纤维以及微量元素，有助于维持身体健康状态。\n\n## 第7天 - 2025-12-11\n### 早餐\n- 燕麦：30克（约1/4杯），提供大约108千卡能量，富含膳食纤维和B族维生素。建议用牛奶或水煮至软糯。\n- 新鲜蓝莓：半杯（约75克），含有约42千卡热量，是维生素C的良好来源。直接食用或加入燕麦中均可。\n- 坚果混合包（杏仁+核桃）：一小把（约30克），提供约170千卡的能量，同时含有健康的脂肪和蛋白质。可作为早餐的额外补充。\n\n### 午餐\n- 糙米饭：1碗（约150克未煮前），提供约180千卡的能量，比白米更富含纤维。使用电饭煲正常烹饪即可。\n- 西红柿炒鸡蛋：鸡蛋2个+西红柿1个，总共约含200千卡热量。先将鸡蛋打散后快速翻炒至熟，再加入切好的西红柿块继续翻炒几分钟。\n- 大白菜炖豆腐：大白菜100克+北豆腐100克，合计约有90千卡左右。将所有材料放入锅中加适量清水慢火炖煮直至入味。\n\n### 晚餐\n- 土豆泥：土豆1个（约200克），提供约160千卡热量，富含碳水化合物及少量蛋白质。蒸熟后压成泥状，可根据口味添加少量黄油提香。\n- 清蒸鲈鱼：一条（约200克），大约含有180千卡热量，并且是优质蛋白的好来源。只需简单地在鱼身上撒上一些姜丝、葱段，然后放入蒸锅中蒸制即可。\n- 菠菜汤：菠菜100克，约含30千卡热量。菠菜洗净后与适量清水一同煮沸即成，可适当调味。\n\n### 加餐\n- 红薯：一个小号（约150克），提供约100千卡热量，是一种很好的低GI食品选择。可以烤着吃或者蒸着吃都非常美味。\n\n### 营养总计\n根据上述食谱安排，全天摄入总热量约为1040千卡来自主食类（包括燕麦、糙米、土豆、红薯）、410千卡来自蛋白质源（如鸡蛋、豆腐、鲈鱼）、以及约550千卡来自其他食物（如坚果、蔬菜等）。这样的饮食结构既保证了足够的能量供给也兼顾了营养均衡，非常适合维持健康状态下的日常需求。请注意，这里提供的数值仅供参考，具体数值可能会因食材品种和个人差异而略有不同。此外，在实际操作过程中还需考虑个人活动量等因素调整食量以达到最佳效果。\n\n## 4. 饮食建议\n1. 保持规律的用餐时间\n2. 多喝水，每天至少8杯\n3. 控制油盐糖的摄入\n4. 适量运动，配合饮食计划\n5. 保证充足睡眠\n\n\n---\n\n# 🛒 采购清单（7天）\n\n## 🥬 蔬菜类\n- 新鲜菠菜：300克\n- 土豆：4个（约400克）\n- 番茄/西红柿：6个（中等大小）\n- 大白菜：800克\n- 菠菜：1把（约200克）\n- 鸡蛋：5个\n- 香蕉：3根\n- 蓝莓：半杯（约75克）\n- 西红柿：1个（中等大小）\n- 北豆腐：1盒（约200克）\n- 菠菜：100克\n\n## 🍎 水果类\n- 苹果：4个（中等大小）\n- 香蕉：3根\n- 蓝莓：半杯（约75克）\n\n## 🍖 肉类/蛋白质\n- 鸡胸肉：300克\n- 嫩豆腐：半盒（约200克）\n- 鲈鱼：1条（约200克）\n\n## 🍚 主食类\n- 燕麦：260克\n- 糙米：400克（干重）\n- 小麦粉（标准粉）：100克\n- 小麦面条：50克（干面）\n- 红薯：4个（中等大小，约800克）\n- 玉米棒：2根（中等大小）\n- 黄油：少量（用于土豆泥调味）\n\n## 🥛 其他\n- 牛奶：800毫升\n- 无糖酸奶：1小杯（约200克）\n- 坚果混合物（杏仁+核桃）：120克\n- 蜂蜜：适量\n- 橄榄油：适量\n- 盐：适量\n- 黑胡椒：适量\n- 酱油：少量\n- 姜蒜：适量\n- 清汤底料：适量\n\n## 💡 采购建议\n- **合并相同食材的数量**：在购买时尽量合并相同食材的数量，以减少浪费。\n- **考虑食材的保鲜期**：新鲜蔬菜和水果应尽早食用或冷藏保存。肉类和主食类可以适当冷冻保存，延长保质期。\n- **单位使用常见的购买单位**：如斤、个、包等，便于购买和计算。\n- **选择新鲜食材**：尽量选择新鲜的蔬菜和水果，保证营养和口感。\n- **分批采购**：部分易腐烂的食材如菠菜、香蕉等，可以分批次采购，避免一次性购买过多导致浪费。\n- **注意季节性**：根据季节选择当季食材，既经济又健康。\n- **提前规划**：根据每天的饮食计划提前规划好需要购买的食材，避免遗漏。\n\n希望这份采购清单能帮助你顺利完成7天的健康饮食计划！','2025-12-05 08:09:29','2025-12-05 08:09:29');
/*!40000 ALTER TABLE `diet_plan_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `diet_plan_tasks`
--

DROP TABLE IF EXISTS `diet_plan_tasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `diet_plan_tasks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` varchar(50) NOT NULL COMMENT '任务ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '任务状态: pending, running, completed, failed, cancelled',
  `progress` int DEFAULT '0' COMMENT '进度百分比',
  `total_days` int NOT NULL COMMENT '总天数',
  `current_day` int DEFAULT '0' COMMENT '当前生成到第几天',
  `plan_id` varchar(50) DEFAULT NULL COMMENT '生成的计划ID（完成后）',
  `request_data` text COMMENT '请求参数JSON',
  `error_message` text COMMENT '错误信息',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `task_id` (`task_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='饮食计划生成任务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `diet_plan_tasks`
--

LOCK TABLES `diet_plan_tasks` WRITE;
/*!40000 ALTER TABLE `diet_plan_tasks` DISABLE KEYS */;
INSERT INTO `diet_plan_tasks` VALUES (4,'task_1764920325614',1,'completed',100,7,7,'plan_1764920571413','{\"days\":7,\"goal\":\"maintain\",\"exerciseLevel\":\"medium\",\"preferences\":\"\",\"allergies\":\"\",\"includeNutrition\":true,\"includeShoppingList\":true,\"includeCookingTips\":true}',NULL,'2025-12-05 07:38:46','2025-12-05 07:42:51'),(8,'task_1764921590978',1,'completed',100,7,7,'plan_1764921822591','{\"days\":7,\"goal\":\"maintain\",\"exerciseLevel\":\"medium\",\"preferences\":\"\",\"allergies\":\"\",\"includeNutrition\":true,\"includeShoppingList\":true,\"includeCookingTips\":true}',NULL,'2025-12-05 07:59:51','2025-12-05 08:03:43'),(9,'task_1764921715488',1,'completed',100,7,7,'plan_1764921900522','{\"days\":7,\"goal\":\"maintain\",\"exerciseLevel\":\"medium\",\"preferences\":\"\",\"allergies\":\"\",\"includeNutrition\":true,\"includeShoppingList\":true,\"includeCookingTips\":true}',NULL,'2025-12-05 08:01:55','2025-12-05 08:05:01'),(10,'task_1764921813891',1,'completed',100,7,7,'plan_1764922045858','{\"days\":7,\"goal\":\"maintain\",\"exerciseLevel\":\"medium\",\"preferences\":\"\",\"allergies\":\"\",\"includeNutrition\":true,\"includeShoppingList\":true,\"includeCookingTips\":true}',NULL,'2025-12-05 08:03:34','2025-12-05 08:07:26'),(11,'task_1764921937042',1,'completed',100,7,7,'plan_1764922168756','{\"days\":7,\"goal\":\"maintain\",\"exerciseLevel\":\"medium\",\"preferences\":\"\",\"allergies\":\"\",\"includeNutrition\":true,\"includeShoppingList\":true,\"includeCookingTips\":true}',NULL,'2025-12-05 08:05:37','2025-12-05 08:09:29'),(17,'task_1764922773192',1,'cancelled',0,7,0,NULL,'{\"days\":7,\"goal\":\"maintain\",\"exerciseLevel\":\"medium\",\"preferences\":\"\",\"allergies\":\"\",\"includeNutrition\":true,\"includeShoppingList\":true,\"includeCookingTips\":true}',NULL,'2025-12-05 08:19:33','2025-12-05 08:19:55'),(18,'task_1764922819311',1,'completed',100,7,7,'plan_1764923125638','{\"days\":7,\"goal\":\"maintain\",\"exerciseLevel\":\"medium\",\"preferences\":\"\",\"allergies\":\"\",\"includeNutrition\":true,\"includeShoppingList\":true,\"includeCookingTips\":true}',NULL,'2025-12-05 08:20:19','2025-12-05 08:25:26'),(19,'task_1764923359078',1,'running',0,7,1,NULL,'{\"days\":7,\"goal\":\"maintain\",\"exerciseLevel\":\"medium\",\"preferences\":\"\",\"allergies\":\"\",\"includeNutrition\":true,\"includeShoppingList\":true,\"includeCookingTips\":true}',NULL,'2025-12-05 08:29:19','2025-12-05 08:29:19'),(20,'task_1764923542583',1,'cancelled',0,7,1,NULL,'{\"days\":7,\"goal\":\"maintain\",\"exerciseLevel\":\"medium\",\"preferences\":\"\",\"allergies\":\"\",\"includeNutrition\":true,\"includeShoppingList\":true,\"includeCookingTips\":true}',NULL,'2025-12-05 08:32:23','2025-12-05 08:32:42'),(21,'task_1764923753243',1,'cancelled',0,7,1,NULL,'{\"days\":7,\"goal\":\"maintain\",\"exerciseLevel\":\"medium\",\"preferences\":\"\",\"allergies\":\"\",\"includeNutrition\":true,\"includeShoppingList\":true,\"includeCookingTips\":true}',NULL,'2025-12-05 08:35:53','2025-12-05 08:36:09'),(22,'task_1764923928890',1,'cancelled',0,7,1,NULL,'{\"days\":7,\"goal\":\"maintain\",\"exerciseLevel\":\"medium\",\"preferences\":\"\",\"allergies\":\"\",\"includeNutrition\":true,\"includeShoppingList\":true,\"includeCookingTips\":true}',NULL,'2025-12-05 08:38:49','2025-12-05 08:39:01'),(23,'task_1764924059469',1,'cancelled',42,7,4,NULL,'{\"days\":7,\"goal\":\"maintain\",\"exerciseLevel\":\"medium\",\"preferences\":\"\",\"allergies\":\"\",\"includeNutrition\":true,\"includeShoppingList\":true,\"includeCookingTips\":true}',NULL,'2025-12-05 08:40:59','2025-12-05 08:43:04');
/*!40000 ALTER TABLE `diet_plan_tasks` ENABLE KEYS */;
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
INSERT INTO `flyway_schema_history` VALUES (1,'1','<< Flyway Baseline >>','BASELINE','<< Flyway Baseline >>',NULL,'root','2025-12-03 06:16:53',0,1),(2,'5','Create Food Records Table','SQL','V5__Create_Food_Records_Table.sql',-1601675978,'root','2025-12-03 06:16:54',14,1),(3,'6','Create Member Tables','SQL','V6__Create_Member_Tables.sql',NULL,'root','2025-12-03 06:22:39',0,1),(4,'7','create food tables','SQL','V7__create_food_tables.sql',889647335,'root','2025-12-03 08:34:39',23,1),(5,'8','init food categories','SQL','V8__init_food_categories.sql',-455914640,'root','2025-12-03 08:50:58',31,1),(6,'9','init food data','SQL','V9__init_food_data.sql',1883822887,'root','2025-12-03 08:50:58',19,1),(7,'10','update food data','SQL','V10__update_food_data.sql',1551254713,'root','2025-12-03 08:50:58',37,1),(8,'11','Create food recognition history','SQL','V11__Create_food_recognition_history.sql',1782438989,'root','2025-12-04 12:45:21',29,1),(9,'12','create task and history tables','SQL','V12__create_task_and_history_tables.sql',-584251532,'root','2025-12-05 07:46:51',24,1),(10,'13','Create ai chat tables','SQL','V13__Create_ai_chat_tables.sql',29573929,'root','2025-12-05 14:40:37',20,1),(11,'14','Create admin tables','SQL','V14__Create_admin_tables.sql',NULL,'root','2025-12-05 15:45:07',0,1);
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
) ENGINE=InnoDB AUTO_INCREMENT=104 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='食材分类表';
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
) ENGINE=InnoDB AUTO_INCREMENT=65 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='食材营养数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `food_nutrition`
--

LOCK TABLES `food_nutrition` WRITE;
/*!40000 ALTER TABLE `food_nutrition` DISABLE KEYS */;
INSERT INTO `food_nutrition` VALUES (1,'RICE_001','大米','Rice',16,100.00,346.00,7.40,0.80,77.90,0.70,0.00,0.11,0.05,0.00,0.00,13.00,110.00,0.00,0.00,0.00,1.30,0.00,0.00,0.00,13.30,NULL,'四季',NULL,NULL,'提供能量，易消化吸收','一般人群',NULL,'米饭,白米,粳米','dami','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:50:58'),(2,'RICE_002','糙米','Brown Rice',16,100.00,348.00,7.70,2.70,77.40,3.90,0.00,0.41,0.10,0.00,0.00,14.00,297.00,0.00,0.00,0.00,2.30,0.00,0.00,0.00,12.50,NULL,'四季',NULL,NULL,'富含膳食纤维，有助于控制血糖','糖尿病患者,减肥人群',NULL,'粗米,全米','zaomi','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:50:58'),(3,'FLOUR_001','小麦粉（标准粉）','Wheat Flour',17,100.00,349.00,11.20,1.50,74.30,2.00,0.00,0.28,0.08,0.00,0.00,31.00,188.00,0.00,0.00,0.00,3.50,0.00,0.00,0.00,12.00,NULL,'四季',NULL,NULL,'提供能量和蛋白质','一般人群',NULL,'面粉,白面','xiaomaifeng','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(4,'GRAINS_001','燕麦','Oats',18,100.00,367.00,15.00,6.70,66.90,5.30,0.00,0.30,0.14,0.00,0.00,186.00,291.00,0.00,0.00,0.00,7.00,0.00,0.00,0.00,10.00,NULL,'四季',NULL,NULL,'降低胆固醇，稳定血糖，富含β-葡聚糖','心血管疾病患者,糖尿病患者',NULL,'麦片,燕麦片','yanmai','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(5,'GRAINS_002','玉米（鲜）','Sweet Corn',18,100.00,106.00,4.00,1.20,22.80,2.90,0.00,0.16,0.11,0.00,0.00,5.00,117.00,0.00,0.00,0.00,1.10,0.00,0.00,0.00,69.60,NULL,'夏秋',NULL,NULL,'富含膳食纤维和维生素E','一般人群',NULL,'嫩玉米,甜玉米','yumi','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(6,'POTATOES_001','红薯','Sweet Potato',19,100.00,99.00,1.10,0.20,24.70,1.60,0.00,0.04,0.04,0.00,0.00,18.00,39.00,0.00,0.00,0.00,0.50,0.00,0.00,0.00,72.80,NULL,'秋冬',NULL,NULL,'富含膳食纤维和β-胡萝卜素，促进肠道蠕动','一般人群,便秘患者',NULL,'地瓜,山芋,番薯','hongshu','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(7,'POTATOES_002','土豆','Potato',19,100.00,76.00,2.00,0.20,17.20,0.70,0.00,0.08,0.04,0.00,0.00,8.00,40.00,0.00,0.00,0.00,0.80,0.00,0.00,0.00,78.60,NULL,'四季',NULL,NULL,'提供能量，富含钾','一般人群',NULL,'马铃薯,洋芋','tudou','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(8,'VEG_001','大白菜','Chinese Cabbage',20,100.00,15.00,1.50,0.20,3.20,1.00,20.00,0.00,0.00,31.00,0.00,50.00,0.00,130.00,0.00,0.00,0.70,0.00,0.00,0.00,94.70,NULL,'冬春',NULL,NULL,'低热量，富含维生素C和膳食纤维','一般人群,减肥人群',NULL,'白菜,包菜','dabaicai','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(9,'VEG_002','菠菜','Spinach',20,100.00,24.00,2.60,0.30,4.50,1.70,487.00,0.00,0.00,32.00,0.00,66.00,0.00,311.00,0.00,0.00,2.90,0.00,0.00,0.00,91.20,NULL,'春秋',NULL,NULL,'富含铁质和叶酸，补血佳品','贫血患者,孕妇',NULL,'菠菜,波斯菜','bocai','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(10,'VEG_003','西红柿','Tomato',22,100.00,19.00,0.90,0.20,4.00,0.50,92.00,0.00,0.00,19.00,0.00,10.00,0.00,163.00,0.00,0.00,0.50,0.00,0.00,0.00,94.00,NULL,'夏秋',NULL,NULL,'富含番茄红素，抗氧化','一般人群',NULL,'番茄,洋柿子','xihongshi','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(11,'VEG_004','黄瓜','Cucumber',22,100.00,15.00,0.80,0.20,2.90,0.50,15.00,0.00,0.00,9.00,0.00,24.00,0.00,102.00,0.00,0.00,0.50,0.00,0.00,0.00,95.80,NULL,'夏',NULL,NULL,'清热解毒，补水利尿','一般人群,减肥人群',NULL,'青瓜','huanggua','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(12,'VEG_005','胡萝卜','Carrot',21,100.00,37.00,1.00,0.20,8.80,3.20,688.00,0.00,0.00,13.00,0.00,32.00,0.00,190.00,0.00,0.00,1.00,0.00,0.00,0.00,89.00,NULL,'秋冬',NULL,NULL,'富含β-胡萝卜素，保护视力','一般人群',NULL,'红萝卜','huluobo','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(13,'VEG_006','西兰花','Broccoli',22,100.00,36.00,4.10,0.60,6.60,3.60,7210.00,0.00,0.00,51.00,0.00,67.00,0.00,340.00,0.00,0.00,1.00,0.00,0.00,0.00,89.20,NULL,'秋冬春',NULL,NULL,'富含维生素C和K，抗癌佳品','一般人群',NULL,'绿花菜,青花菜','xilanhua','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(14,'FRUIT_001','苹果','Apple',29,100.00,53.00,0.20,0.20,13.70,1.20,0.00,0.00,0.00,4.00,0.00,4.00,0.00,119.00,0.00,0.00,0.00,0.00,0.00,0.00,85.00,NULL,'秋',NULL,NULL,'富含膳食纤维和维生素C','一般人群',NULL,'平果','pingguo','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(15,'FRUIT_002','香蕉','Banana',27,100.00,91.00,1.40,0.20,22.00,1.20,0.00,0.00,0.00,8.00,0.00,7.00,0.00,256.00,0.00,0.00,0.00,0.00,0.00,0.00,75.00,NULL,'四季',NULL,NULL,'快速补充能量，富含钾','一般人群,运动人群',NULL,'芭蕉','xiangjiao','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(16,'FRUIT_003','橙子','Orange',25,100.00,48.00,0.80,0.20,11.10,0.60,0.00,0.00,0.00,33.00,0.00,20.00,0.00,159.00,0.00,0.00,0.00,0.00,0.00,0.00,87.00,NULL,'冬春',NULL,NULL,'富含维生素C，增强免疫力','一般人群',NULL,'甜橙,柑橘','chengzi','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(17,'FRUIT_004','草莓','Strawberry',26,100.00,32.00,1.00,0.20,7.10,1.10,0.00,0.00,0.00,47.00,0.00,18.00,0.00,131.00,0.00,0.00,0.00,0.00,0.00,0.00,91.00,NULL,'春',NULL,NULL,'富含维生素C和抗氧化物','一般人群',NULL,'红莓','caomei','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(18,'FRUIT_005','西瓜','Watermelon',30,100.00,31.00,0.60,0.10,7.90,0.30,0.00,0.00,0.00,6.00,0.00,8.00,0.00,87.00,0.00,0.00,0.00,0.00,0.00,0.00,91.00,NULL,'夏',NULL,NULL,'补水解暑，利尿','一般人群',NULL,'寒瓜','xigua','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(19,'CHICKEN_001','鸡胸肉','Chicken Breast',34,100.00,133.00,19.40,5.00,2.50,0.00,0.00,0.05,0.09,0.00,0.00,9.00,156.00,0.00,0.00,0.00,1.30,1.09,0.00,58.00,72.00,NULL,NULL,NULL,NULL,'高蛋白低脂肪，健身佳品','一般人群,健身人群','痛风患者应适量','鸡脯肉','jixiongrou','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(20,'PORK_001','猪瘦肉','Lean Pork',31,100.00,143.00,20.30,6.20,1.50,0.00,0.00,0.54,0.16,0.00,0.00,6.00,162.00,0.00,0.00,0.00,3.00,2.88,0.00,81.00,70.00,NULL,NULL,NULL,NULL,'富含优质蛋白和铁','一般人群','高血脂患者应少食','猪里脊','zhushourou','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(21,'BEEF_001','牛肉','Beef',32,100.00,125.00,20.10,4.20,2.00,0.00,0.00,0.07,0.18,0.00,0.00,9.00,172.00,0.00,0.00,0.00,2.80,4.73,0.00,58.00,72.60,NULL,NULL,NULL,NULL,'富含优质蛋白和铁，补血佳品','贫血患者,健身人群','痛风患者应少食','牛瘦肉','niurou','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(22,'FISH_001','鲈鱼','Sea Bass',37,100.00,105.00,18.60,3.40,0.00,0.00,19.00,0.00,0.00,0.00,0.75,138.00,242.00,205.00,0.00,0.00,0.00,0.00,33.10,86.00,77.00,NULL,NULL,NULL,NULL,'富含DHA和EPA，有益心脑血管','一般人群,孕妇',NULL,'花鲈,七星鲈','luyu','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(23,'SHRIMP_001','虾','Shrimp',38,100.00,93.00,18.60,1.00,2.80,0.00,15.00,0.00,0.00,0.00,0.62,62.00,228.00,215.00,0.00,0.00,0.00,0.00,29.70,193.00,76.00,NULL,NULL,NULL,NULL,'高蛋白低脂肪，富含矿物质','一般人群',NULL,'对虾,明虾','xia','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:18:54','2025-12-03 08:18:54'),(24,'TOFU_001','豆腐','Tofu',43,100.00,81.00,8.10,3.70,4.20,0.40,0.00,0.00,0.00,0.00,0.00,164.00,119.00,125.00,0.00,0.00,1.90,0.00,0.00,0.00,82.80,NULL,NULL,NULL,NULL,'富含优质植物蛋白和钙','一般人群,素食者',NULL,'黄豆腐','doufu','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:19:36','2025-12-03 08:19:36'),(25,'SOYBEANS_001','黄豆','Soybean',42,100.00,359.00,35.00,16.00,34.20,15.50,0.00,0.00,0.00,0.00,0.00,191.00,465.00,1503.00,0.00,0.00,8.20,0.00,0.00,0.00,10.00,NULL,NULL,NULL,NULL,'富含蛋白质和异黄酮','一般人群',NULL,'大豆','huangdou','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:19:36','2025-12-03 08:19:36'),(26,'NUTS_001','核桃','Walnut',48,100.00,646.00,14.90,58.80,19.10,9.50,0.00,0.00,0.00,0.00,43.21,56.00,0.00,0.00,0.00,131.00,0.00,3.12,0.00,0.00,3.50,NULL,NULL,NULL,NULL,'富含不饱和脂肪酸，健脑益智','一般人群','肥胖者应少食','胡桃','hetao','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:19:45','2025-12-03 08:19:45'),(27,'NUTS_002','杏仁','Almond',48,100.00,578.00,21.30,50.60,20.50,11.80,0.00,0.00,0.00,0.00,25.63,248.00,0.00,0.00,0.00,275.00,0.00,3.36,0.00,0.00,4.70,NULL,NULL,NULL,NULL,'富含维生素E，抗氧化','一般人群','肥胖者应少食','巴旦木','xingren','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:19:45','2025-12-03 08:19:45'),(28,'EGG_001','鸡蛋','Egg',7,100.00,147.00,13.30,8.80,2.80,0.00,234.00,0.00,0.27,0.00,0.00,56.00,130.00,0.00,0.00,0.00,2.00,0.00,0.00,585.00,71.50,NULL,NULL,NULL,NULL,'营养全面，蛋白质优质','一般人群','高胆固醇患者应适量','鸡子','jidan','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:22:43','2025-12-03 08:22:43'),(29,'MILK_001','纯牛奶','Milk',45,100.00,54.00,3.00,3.20,3.40,0.00,24.00,0.00,0.14,0.00,0.00,104.00,73.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,87.50,NULL,NULL,NULL,NULL,'富含优质蛋白和钙','一般人群,儿童,老人',NULL,'鲜奶','chuniunai','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:22:43','2025-12-03 08:22:43'),(30,'YOGURT_001','酸奶','Yogurt',46,100.00,72.00,2.50,2.70,9.30,0.00,26.00,0.00,0.15,0.00,0.00,118.00,85.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,84.40,NULL,NULL,NULL,NULL,'促进消化，富含益生菌','一般人群,便秘患者',NULL,'乳酪,优格','suannai','中国食物成分表',NULL,'ACTIVE',1,'2025-12-03 08:22:43','2025-12-03 08:22:43');
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
-- Table structure for table `food_recognition_history`
--

DROP TABLE IF EXISTS `food_recognition_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `food_recognition_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `recognition_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'TEXT' COMMENT '识别方式：TEXT-文本识别, IMAGE-图片识别',
  `input_text` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '输入的文本内容（文本识别时使用）',
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片URL',
  `recognition_result` json DEFAULT NULL COMMENT '识别结果（JSON格式）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_recognition_type` (`recognition_type`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食物识别历史记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `food_recognition_history`
--

LOCK TABLES `food_recognition_history` WRITE;
/*!40000 ALTER TABLE `food_recognition_history` DISABLE KEYS */;
INSERT INTO `food_recognition_history` VALUES (2,1,'TEXT',NULL,NULL,'{\"foods\": [{\"name\": \"苹果\", \"nutrition\": {\"fat\": 0.2, \"energy\": 52.0, \"source\": \"estimated\", \"protein\": 0.3, \"carbohydrate\": 13.8}, \"confidence\": 0.75}], \"totalCount\": 1, \"recognitionTime\": 1445}','2025-12-04 12:55:35');
/*!40000 ALTER TABLE `food_recognition_history` ENABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='饮食记录表';
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
-- Table structure for table `system_announcement`
--

DROP TABLE IF EXISTS `system_announcement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_announcement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告标题',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告内容',
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告类型：info/warning/error',
  `priority` int DEFAULT '0' COMMENT '优先级（数字越大越优先）',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `start_time` timestamp NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` timestamp NULL DEFAULT NULL COMMENT '结束时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_priority` (`priority`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统公告表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_announcement`
--

LOCK TABLES `system_announcement` WRITE;
/*!40000 ALTER TABLE `system_announcement` DISABLE KEYS */;
INSERT INTO `system_announcement` VALUES (1,'欢迎使用AI健康饮食助手','感谢您使用我们的服务！如有任何问题，请随时联系客服。','info',1,1,'2025-12-05 15:33:31',NULL,NULL,'2025-12-05 15:33:31','2025-12-06 02:11:32'),(2,'系统维护通知','系统将于本周六凌晨2:00-4:00进行维护升级，期间服务可能暂时中断，敬请谅解。','warning',2,1,'2025-12-05 15:33:31',NULL,NULL,'2025-12-05 15:33:31','2025-12-06 02:13:41'),(5,'1','1','info',1,1,NULL,NULL,1,'2025-12-06 02:08:00','2025-12-06 02:11:32');
/*!40000 ALTER TABLE `system_announcement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_config`
--

DROP TABLE IF EXISTS `system_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键',
  `config_value` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置值',
  `config_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置类型：string/number/boolean/json',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配置描述',
  `category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配置分类',
  `is_public` tinyint(1) DEFAULT '0' COMMENT '是否公开（前端可访问）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `config_key` (`config_key`),
  KEY `idx_category` (`category`),
  KEY `idx_is_public` (`is_public`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_config`
--

LOCK TABLES `system_config` WRITE;
/*!40000 ALTER TABLE `system_config` DISABLE KEYS */;
INSERT INTO `system_config` VALUES (1,'ai.default_model','qwen-max','string','默认AI模型','ai',1,'2025-12-05 15:33:31','2025-12-05 15:33:31'),(2,'ai.max_tokens','1000','number','AI响应的最大Token数量','ai',1,'2025-12-05 15:33:31','2025-12-06 04:39:30'),(3,'ai.temperature','0.7','number','控制AI响应的随机性，范围0-1','ai',1,'2025-12-05 15:33:31','2025-12-06 04:39:30'),(4,'system.site_name','AI健康饮食规划助手','string','系统显示的网站名称','system',1,'2025-12-05 15:33:31','2025-12-06 04:39:29'),(5,'system.maintenance_mode','false','boolean','是否开启系统维护模式','system',1,'2025-12-05 15:33:31','2025-12-06 04:39:30'),(6,'member.free_chat_limit','10','number','免费用户每日对话限制','member',0,'2025-12-05 15:33:31','2025-12-05 15:33:31'),(7,'member.bronze_chat_limit','50','number','青铜会员每日对话限制','member',0,'2025-12-05 15:33:31','2025-12-05 15:33:31'),(8,'member.silver_chat_limit','200','number','白银会员每日对话限制','member',0,'2025-12-05 15:33:31','2025-12-05 15:33:31'),(9,'member.gold_chat_limit','-1','number','黄金会员每日对话限制（-1表示无限制）','member',0,'2025-12-05 15:33:31','2025-12-05 15:33:31'),(12,'system.site_description','智能营养分析 · 个性化饮食方案 · 健康管理','string','网站的简短描述','系统',1,'2025-12-06 04:39:29','2025-12-06 04:39:29'),(13,'system.contact_email','support@nutriai.com','string','客服联系邮箱','系统',1,'2025-12-06 04:39:29','2025-12-06 04:39:29'),(14,'system.support_phone','400-123-4567','string','客服联系电话','系统',1,'2025-12-06 04:39:30','2025-12-06 04:39:30'),(15,'system.copyright_text','© 2025 AI健康饮食规划助手. All rights reserved.','string','页脚显示的版权信息','系统',1,'2025-12-06 04:39:30','2025-12-06 04:39:30'),(16,'system.icp_number','','string','网站ICP备案号','系统',1,'2025-12-06 04:39:30','2025-12-06 04:39:30'),(18,'system.max_upload_size','10','number','文件上传的最大大小（MB）','系统',1,'2025-12-06 04:39:30','2025-12-06 04:39:30'),(19,'system.enable_registration','true','string','是否允许新用户注册','系统',1,'2025-12-06 04:39:30','2025-12-06 04:39:30'),(20,'ai.model','qwen-max','string','使用的AI模型名称','AI',0,'2025-12-06 04:39:30','2025-12-06 04:39:30'),(23,'user.default_member_level','FREE','string','新用户注册时的默认会员等级','用户',0,'2025-12-06 04:39:30','2025-12-06 04:39:30'),(24,'user.max_chat_history','100','number','用户可保存的最大对话历史数量','用户',0,'2025-12-06 04:39:30','2025-12-06 04:39:30'),(25,'user.session_timeout','30','number','用户会话超时时间（分钟）','用户',0,'2025-12-06 04:39:30','2025-12-06 04:39:30'),(26,'user.daily_ai_calls_limit','20','number','免费用户每日AI调用次数限制','用户',1,'2025-12-06 04:39:30','2025-12-06 04:39:30'),(27,'user.enable_email_verification','false','string','是否要求用户验证邮箱','用户',0,'2025-12-06 04:39:30','2025-12-06 04:39:30'),(28,'security.password_min_length','8','number','用户密码的最小长度','安全',0,'2025-12-06 04:39:30','2025-12-06 04:39:30'),(29,'security.max_login_attempts','5','number','允许的最大登录失败次数','安全',0,'2025-12-06 04:39:30','2025-12-06 04:39:30'),(30,'security.enable_captcha','false','string','是否启用验证码','安全',0,'2025-12-06 04:39:30','2025-12-06 04:39:30'),(31,'notification.email_enabled','false','string','是否启用邮件通知','通知',0,'2025-12-06 04:39:30','2025-12-06 04:39:30'),(32,'notification.sms_enabled','false','string','是否启用短信通知','通知',0,'2025-12-06 04:39:30','2025-12-06 04:39:30');
/*!40000 ALTER TABLE `system_config` ENABLE KEYS */;
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
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE',
  `member_level` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'FREE',
  `growth_value` int DEFAULT '0',
  `member_expire_time` timestamp NULL DEFAULT NULL,
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
INSERT INTO `users` VALUES (1,'admin','$2a$10$N7TG/HHY9rfCcY5/eX6rDu6BN.eFsSwsC2dvwwJQqb/e4UPpjp02a','admin@nutriai.com',NULL,'http://localhost:8080/api/uploads/avatars/avatar_1e5c41e4-921e-4284-a421-f004055bb85a.jpg','admin','ADMIN','ACTIVE','GOLD',0,NULL,'2025-12-06 13:22:48','0:0:0:0:0:0:0:1','2025-12-02 15:51:27','2025-12-06 13:22:48'),(2,'Test','$2a$10$/1pN0SqTPUHocMLkstR6VeYYfURqzLoH1ZTqduYxnoi1G8sBEtIHu','Test@qq.com',NULL,NULL,'Test','USER','ACTIVE','FREE',0,NULL,'2025-12-02 20:06:15','0:0:0:0:0:0:0:1','2025-12-02 19:55:43','2025-12-06 12:19:01'),(3,'test_user1','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z58QRVa7G4G7.MU8QlHb14m6','test1@example.com',NULL,NULL,NULL,'USER','ACTIVE','FREE',0,NULL,NULL,NULL,'2025-11-08 15:08:48','2025-11-08 15:08:48'),(4,'test_user2','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z58QRVa7G4G7.MU8QlHb14m6','test2@example.com',NULL,NULL,NULL,'USER','ACTIVE','FREE',0,NULL,NULL,NULL,'2025-11-18 15:08:48','2025-11-18 15:08:48'),(5,'test_user3','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z58QRVa7G4G7.MU8QlHb14m6','test3@example.com',NULL,NULL,NULL,'USER','ACTIVE','FREE',0,NULL,NULL,NULL,'2025-11-26 15:08:48','2025-11-26 15:08:48');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `v_food_category_stats`
--

DROP TABLE IF EXISTS `v_food_category_stats`;
/*!50001 DROP VIEW IF EXISTS `v_food_category_stats`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_food_category_stats` AS SELECT 
 1 AS `id`,
 1 AS `category_code`,
 1 AS `category_name`,
 1 AS `level`,
 1 AS `food_count`*/;
SET character_set_client = @saved_cs_client;

--
-- Final view structure for view `v_food_category_stats`
--

/*!50001 DROP VIEW IF EXISTS `v_food_category_stats`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_food_category_stats` AS select `fc`.`id` AS `id`,`fc`.`category_code` AS `category_code`,`fc`.`category_name` AS `category_name`,`fc`.`level` AS `level`,count(`fn`.`id`) AS `food_count` from (`food_category` `fc` left join `food_nutrition` `fn` on((`fn`.`category_id` = `fc`.`id`))) group by `fc`.`id`,`fc`.`category_code`,`fc`.`category_name`,`fc`.`level` order by `fc`.`level`,`fc`.`sort_order` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-12-06 13:26:21
