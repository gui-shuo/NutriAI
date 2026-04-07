package com.nutriai.service;

import com.nutriai.dto.admin.ConfigOptionDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 配置选项服务 - 提供所有可用的配置项（与 SystemConfigInitializer 保持一致）
 */
@Service
public class ConfigOptionsService {
    
    /**
     * 获取所有可用的配置选项
     */
    public List<ConfigOptionDTO> getAllConfigOptions() {
        List<ConfigOptionDTO> options = new ArrayList<>();

        // ─── AI 配置 ───
        options.add(select("ai.api_key", "AI API密钥", "AI大模型服务的API密钥", "AI", ""));
        options.add(text("ai.base_url", "AI API基础URL", "AI大模型服务的API地址（OpenAI兼容）", "AI", "https://ark.cn-beijing.volces.com/api/coding/v3"));
        options.add(ConfigOptionDTO.builder()
                .key("ai.model").name("默认AI模型").description("火山方舟Coding套餐支持的模型")
                .category("AI").valueType("select").defaultValue("kimi-k2.5").required(true)
                .options(Arrays.asList(
                        opt("Kimi K2.5（推荐）", "kimi-k2.5"),
                        opt("DeepSeek V3.2", "deepseek-v3.2"),
                        opt("GLM 4.7", "glm-4.7"),
                        opt("MiniMax M2.5", "minimax-m2.5"),
                        opt("豆包 Seed 2.0 Pro", "doubao-seed-2.0-pro"),
                        opt("豆包 Seed 2.0 Lite", "doubao-seed-2.0-lite"),
                        opt("豆包 Seed Code", "doubao-seed-code"),
                        opt("豆包 Seed 2.0 Code", "doubao-seed-2.0-code")
                )).build());
        options.add(ConfigOptionDTO.builder()
                .key("ai.diet_plan_model").name("饮食计划AI模型").description("饮食计划生成专用模型")
                .category("AI").valueType("select").defaultValue("doubao-seed-2.0-lite").required(true)
                .options(Arrays.asList(
                        opt("豆包 Seed 2.0 Lite（推荐·快）", "doubao-seed-2.0-lite"),
                        opt("豆包 Seed 2.0 Pro", "doubao-seed-2.0-pro"),
                        opt("Kimi K2.5", "kimi-k2.5"),
                        opt("DeepSeek V3.2", "deepseek-v3.2"),
                        opt("GLM 4.7", "glm-4.7")
                )).build());
        options.add(number("ai.max_tokens", "最大Token数", "AI响应的最大Token数量", "AI", "2000"));
        options.add(number("ai.temperature", "温度参数", "控制AI响应的随机性，范围0-1", "AI", "0.7"));
        options.add(number("ai.top_p", "Top-P参数", "控制AI响应的多样性，范围0-1", "AI", "0.9"));
        options.add(number("ai.timeout", "AI请求超时(秒)", "AI请求超时时间", "AI", "180"));

        // ─── 系统配置 ───
        options.add(text("system.site_name", "网站名称", "系统显示的网站名称", "系统", "NutriAI饮食规划助手"));
        options.add(text("system.site_description", "网站描述", "网站的简短描述", "系统", "智能营养分析 · 个性化饮食方案 · 科学膳食管理"));
        options.add(text("system.contact_email", "客服邮箱", "客服联系邮箱", "系统", ""));
        options.add(text("system.contact_phone", "客服电话", "客服联系电话", "系统", ""));
        options.add(text("system.copyright", "版权信息", "页脚显示的版权文本", "系统", "© 2025 NutriAI饮食规划助手. All rights reserved."));
        options.add(text("system.icp_number", "ICP备案号", "网站ICP备案号", "系统", ""));
        options.add(bool("system.maintenance_mode", "维护模式", "是否开启系统维护模式", "系统", "false"));
        options.add(bool("system.enable_registration", "开放注册", "是否允许新用户注册", "系统", "true"));
        options.add(number("system.max_upload_size", "最大上传大小(MB)", "文件上传的最大大小", "系统", "10"));

        // ─── 存储配置 ───
        options.add(select("cos.secret_id", "COS SecretId", "腾讯云COS SecretId", "存储", ""));
        options.add(select("cos.secret_key", "COS SecretKey", "腾讯云COS SecretKey", "存储", ""));
        options.add(text("cos.region", "COS区域", "COS存储区域", "存储", "ap-beijing"));
        options.add(text("cos.bucket", "COS存储桶", "COS存储桶名称", "存储", ""));
        options.add(text("cos.custom_domain", "COS自定义域名", "COS加速访问域名", "存储", ""));

        // ─── 食物识别 ───
        options.add(select("baidu.app_id", "百度AI AppID", "百度AI开放平台应用ID", "食物识别", ""));
        options.add(select("baidu.api_key", "百度AI APIKey", "百度AI开放平台APIKey", "食物识别", ""));
        options.add(select("baidu.secret_key", "百度AI SecretKey", "百度AI开放平台SecretKey", "食物识别", ""));
        options.add(select("tianapi.api_key", "天聚数行APIKey", "天聚数行食物营养识别API密钥", "食物识别", ""));
        options.add(number("recognition.image_max_size_mb", "图片最大上传大小(MB)", "食物识别图片上传限制", "食物识别", "5"));
        options.add(bool("recognition.dish_enabled", "菜品识别", "百度菜品识别功能开关", "食物识别", "true"));
        options.add(bool("recognition.ingredient_enabled", "果蔬识别", "百度果蔬识别功能开关", "食物识别", "true"));
        options.add(number("recognition.confidence_threshold", "置信度阈值", "低于此阈值的识别结果将被过滤(0-1)", "食物识别", "0.6"));

        // ─── 邮件配置 ───
        options.add(text("mail.host", "SMTP服务器", "邮件SMTP服务器地址", "邮件", "smtp.163.com"));
        options.add(number("mail.port", "SMTP端口", "邮件SMTP端口", "邮件", "465"));
        options.add(text("mail.username", "邮件账号", "发件邮箱账号", "邮件", ""));
        options.add(select("mail.password", "邮件授权码", "邮箱SMTP授权码", "邮件", ""));
        options.add(text("mail.nickname", "发件人昵称", "邮件发件人显示名称", "邮件", "NutriAI饮食规划助手"));

        // ─── 社交登录 ───
        options.add(text("qq.web_id", "QQ Web端AppID", "QQ互联网站应用ID(Web/H5)", "社交登录", ""));
        options.add(select("qq.web_key", "QQ Web端AppKey", "QQ互联网站应用密钥(Web/H5)", "社交登录", ""));
        options.add(text("qq.app_id", "QQ APP端AppID", "QQ互联移动应用ID(APP)", "社交登录", ""));
        options.add(select("qq.app_key", "QQ APP端AppKey", "QQ互联移动应用密钥(APP)", "社交登录", ""));
        options.add(text("wx.miniapp_appid", "微信小程序AppID", "微信小程序应用ID", "社交登录", ""));
        options.add(select("wx.miniapp_secret", "微信小程序Secret", "微信小程序应用密钥", "社交登录", ""));

        // ─── 即时通信 ───
        options.add(text("im.sdk_app_id", "IM SDKAppID", "腾讯云即时通信IM应用ID", "即时通信", ""));
        options.add(select("im.secret_key", "IM SecretKey", "腾讯云即时通信IM密钥", "即时通信", ""));

        // ─── 跨域 ───
        options.add(text("cors.allowed_origins", "允许跨域来源", "允许的跨域来源(逗号分隔)", "跨域", "http://localhost:3000"));

        // ─── 安全 ───
        options.add(select("jwt.secret", "JWT密钥", "JWT Token签名密钥（至少64字符）", "安全", ""));
        options.add(number("security.password_min_length", "密码最小长度", "用户密码的最小长度", "安全", "8"));
        options.add(number("security.max_login_attempts", "最大登录尝试", "允许的最大登录失败次数", "安全", "5"));
        options.add(bool("security.captcha_enabled", "验证码", "登录时是否启用验证码", "安全", "false"));

        // ─── 用户 ───
        options.add(number("user.free_ai_quota", "免费AI配额", "免费用户每日AI咨询次数", "用户", "3"));
        options.add(number("user.max_chat_history", "聊天记录上限", "用户可保存的最大对话数量", "用户", "100"));
        options.add(number("user.session_timeout", "会话超时(分钟)", "用户会话超时时间", "用户", "30"));
        options.add(number("user.daily_ai_calls_limit", "每日AI调用限制", "免费用户每日AI调用次数", "用户", "20"));
        options.add(bool("user.require_email_verification", "邮箱验证", "注册时是否强制验证邮箱", "用户", "false"));

        // ─── 通知 ───
        options.add(bool("notification.email_enabled", "邮件通知", "是否启用邮件通知", "通知", "true"));
        options.add(bool("notification.sms_enabled", "短信通知", "是否启用短信通知", "通知", "false"));

        // ─── 营养师 ───
        options.add(bool("nutritionist.auto_approve", "自动审核", "营养师注册后是否自动通过", "营养师", "false"));
        options.add(number("nutritionist.default_fee", "默认咨询费(元)", "营养师咨询默认费用", "营养师", "98"));
        options.add(number("nutritionist.order_timeout", "订单超时(分钟)", "咨询订单未支付超时时间", "营养师", "30"));
        options.add(bool("nutritionist.im_enabled", "即时通信", "是否启用腾讯云IM通信", "营养师", "true"));

        // ─── 饮食计划 ───
        options.add(number("diet.plan_max_days", "计划最大天数", "饮食计划最大可生成天数", "饮食计划", "7"));
        options.add(number("diet.daily_calorie_min", "每日最低热量(kcal)", "饮食计划每日最低热量", "饮食计划", "1200"));
        options.add(number("diet.daily_calorie_max", "每日最高热量(kcal)", "饮食计划每日最高热量", "饮食计划", "3000"));

        // ─── 数据库 ───
        options.add(text("db.host", "数据库主机", "MySQL数据库主机地址", "数据库", ""));
        options.add(number("db.port", "数据库端口", "MySQL数据库端口", "数据库", "3306"));
        options.add(text("db.name", "数据库名称", "MySQL数据库名称", "数据库", "nutriai"));
        options.add(text("db.username", "数据库用户名", "MySQL连接用户名", "数据库", ""));
        options.add(select("db.password", "数据库密码", "MySQL连接密码", "数据库", ""));

        // ─── Redis ───
        options.add(select("redis.password", "Redis密码", "Redis连接密码", "Redis", ""));

        return options;
    }
    
    /**
     * 根据分类获取配置选项
     */
    public List<ConfigOptionDTO> getConfigOptionsByCategory(String category) {
        return getAllConfigOptions().stream()
                .filter(option -> option.getCategory().equals(category))
                .toList();
    }

    // ─── 构建工具方法 ───

    private static ConfigOptionDTO text(String key, String name, String desc, String cat, String def) {
        return ConfigOptionDTO.builder().key(key).name(name).description(desc).category(cat)
                .valueType("string").defaultValue(def).required(false).build();
    }

    private static ConfigOptionDTO number(String key, String name, String desc, String cat, String def) {
        return ConfigOptionDTO.builder().key(key).name(name).description(desc).category(cat)
                .valueType("number").defaultValue(def).required(false).build();
    }

    private static ConfigOptionDTO bool(String key, String name, String desc, String cat, String def) {
        return ConfigOptionDTO.builder().key(key).name(name).description(desc).category(cat)
                .valueType("select").defaultValue(def).required(true)
                .options(Arrays.asList(opt("开启", "true"), opt("关闭", "false"))).build();
    }

    /** 敏感字段用 select type 以便前端可做掩码处理 */
    private static ConfigOptionDTO select(String key, String name, String desc, String cat, String def) {
        return ConfigOptionDTO.builder().key(key).name(name).description(desc).category(cat)
                .valueType("string").defaultValue(def).required(false).build();
    }

    private static ConfigOptionDTO.OptionValue opt(String label, String value) {
        return ConfigOptionDTO.OptionValue.builder().label(label).value(value).build();
    }
}
