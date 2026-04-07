package com.nutriai.config;

import com.nutriai.entity.SystemConfig;
import com.nutriai.repository.SystemConfigRepository;
import com.nutriai.service.DynamicConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统配置初始化器
 * 应用启动时：清空旧配置 → 从环境变量 / application.yml 中读取默认值写入 system_config 表
 * 后续管理员可在后台修改，DynamicConfigService 优先读取数据库值
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemConfigInitializer implements ApplicationRunner {

    private final SystemConfigRepository configRepository;
    private final DynamicConfigService dynamicConfigService;
    private final Environment env;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("========== 系统配置初始化开始 ==========");

        List<SystemConfig> defaults = buildAllDefaults();
        int inserted = 0;

        // 仅插入数据库中不存在的配置项（保留管理员已修改的值）
        for (SystemConfig cfg : defaults) {
            if (!configRepository.existsByConfigKey(cfg.getConfigKey())) {
                configRepository.save(cfg);
                inserted++;
            }
        }

        if (inserted > 0) {
            log.info("新增 {} 条默认配置", inserted);
        }
        log.info("系统配置共 {} 条（含已有 {} 条）", defaults.size(), defaults.size() - inserted);

        // 刷新缓存
        dynamicConfigService.invalidateAll();

        log.info("========== 系统配置初始化完成 ==========");
    }

    // ──────────────────────────────────────────────────────
    //  配置定义
    // ──────────────────────────────────────────────────────

    private List<SystemConfig> buildAllDefaults() {
        List<SystemConfig> list = new ArrayList<>();

        // ─── AI 配置 ───
        add(list, "ai.api_key",          env("AI_API_KEY", ""),                           "string", "AI API密钥",               "AI",     false);
        add(list, "ai.base_url",         env("AI_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1"), "string", "AI API基础URL",           "AI",     false);
        add(list, "ai.model",            env("AI_MODEL_NAME", "kimi-k2.5"),               "string", "默认AI模型（火山方舟Coding套餐）",  "AI",     true);
        add(list, "ai.diet_plan_model",  env("AI_DIET_PLAN_MODEL", "doubao-seed-2.0-lite"), "string", "饮食计划专用AI模型",       "AI",     true);
        add(list, "ai.max_tokens",       env("ai.max-tokens", "2000"),                    "number", "最大Token数",              "AI",     true);
        add(list, "ai.temperature",      env("ai.temperature", "0.7"),                    "number", "温度参数(0-1)",            "AI",     true);
        add(list, "ai.top_p",            env("ai.top-p", "0.9"),                          "number", "Top-P参数(0-1)",           "AI",     true);
        add(list, "ai.timeout",          env("ai.timeout", "180"),                        "number", "AI请求超时(秒)",           "AI",     true);

        // ─── 系统配置 ───
        add(list, "system.site_name",          "NutriAI饮食规划助手",                      "string", "网站名称",                 "系统",   true);
        add(list, "system.site_description",   "智能营养分析 · 个性化饮食方案 · 科学膳食管理", "string", "网站简短描述",             "系统",   true);
        add(list, "system.contact_email",      env("MAIL_USERNAME", "support@nutriai.com"), "string", "客服联系邮箱",             "系统",   true);
        add(list, "system.contact_phone",      "",                                          "string", "客服联系电话",             "系统",   true);
        add(list, "system.copyright",          "© 2025 NutriAI饮食规划助手. All rights reserved.", "string", "页脚版权信息",       "系统",   true);
        add(list, "system.icp_number",         "",                                          "string", "网站ICP备案号",            "系统",   true);
        add(list, "system.maintenance_mode",   "false",                                     "boolean","维护模式开关",             "系统",   true);
        add(list, "system.enable_registration","true",                                      "boolean","是否允许新用户注册",       "系统",   true);
        add(list, "system.max_upload_size",    "10",                                        "number", "最大文件上传大小(MB)",     "系统",   true);

        // ─── 存储配置（腾讯云COS） ───
        add(list, "cos.secret_id",       env("COS_SECRET_ID", ""),                        "string", "腾讯云COS SecretId",       "存储",   false);
        add(list, "cos.secret_key",      env("COS_SECRET_KEY", ""),                       "string", "腾讯云COS SecretKey",      "存储",   false);
        add(list, "cos.region",          env("COS_REGION", "ap-beijing"),                 "string", "COS存储区域",              "存储",   false);
        add(list, "cos.bucket",          env("COS_BUCKET", ""),                           "string", "COS存储桶名称",            "存储",   false);
        add(list, "cos.custom_domain",   env("COS_CUSTOM_DOMAIN", ""),                    "string", "COS自定义加速域名",        "存储",   false);

        // ─── 百度AI识别 ───
        add(list, "baidu.app_id",        env("BAIDU_AI_APP_ID", ""),                      "string", "百度AI应用ID",             "食物识别", false);
        add(list, "baidu.api_key",       env("BAIDU_AI_API_KEY", ""),                     "string", "百度AI API Key",           "食物识别", false);
        add(list, "baidu.secret_key",    env("BAIDU_AI_SECRET_KEY", ""),                  "string", "百度AI Secret Key",        "食物识别", false);
        add(list, "tianapi.api_key",     env("TIANAPI_API_KEY", ""),                       "string", "天聚数行API Key",          "食物识别", false);
        add(list, "recognition.image_max_size_mb", "5",                                    "number", "图片最大上传大小(MB)",     "食物识别", true);
        add(list, "recognition.dish_enabled",      "true",                                 "boolean","菜品识别开关",             "食物识别", true);
        add(list, "recognition.ingredient_enabled", "true",                                "boolean","果蔬识别开关",             "食物识别", true);
        add(list, "recognition.confidence_threshold", "0.6",                               "number", "识别置信度阈值(0-1)",     "食物识别", true);

        // ─── 邮件配置 ───
        add(list, "mail.host",           env("MAIL_HOST", "smtp.163.com"),                "string", "SMTP邮件服务器",           "邮件",   false);
        add(list, "mail.port",           env("MAIL_PORT", "465"),                         "number", "SMTP端口",                 "邮件",   false);
        add(list, "mail.username",       env("MAIL_USERNAME", ""),                        "string", "邮件账号",                 "邮件",   false);
        add(list, "mail.password",       env("MAIL_PASSWORD", ""),                        "string", "邮件授权码",               "邮件",   false);
        add(list, "mail.nickname",       env("MAIL_NICKNAME", "NutriAI饮食规划助手"),      "string", "发件人昵称",               "邮件",   false);

        // ─── 社交登录 ───
        add(list, "qq.web_id",           env("QQ_WEB_ID", ""),                            "string", "QQ登录 Web端AppID",        "社交登录", false);
        add(list, "qq.web_key",          env("QQ_WEB_KEY", ""),                           "string", "QQ登录 Web端AppKey",       "社交登录", false);
        add(list, "qq.app_id",           env("QQ_APP_ID", ""),                            "string", "QQ登录 APP端AppID",        "社交登录", false);
        add(list, "qq.app_key",          env("QQ_APP_KEY", ""),                           "string", "QQ登录 APP端AppKey",       "社交登录", false);
        add(list, "wx.miniapp_appid",    env("WX_MINIAPP_APPID", ""),                     "string", "微信小程序 AppID",         "社交登录", false);
        add(list, "wx.miniapp_secret",   env("WX_MINIAPP_SECRET", ""),                    "string", "微信小程序 Secret",        "社交登录", false);

        // ─── 即时通信 ───
        add(list, "im.sdk_app_id",       env("SDKAppID", "0"),                            "string", "腾讯云IM SDKAppID",        "即时通信", false);
        add(list, "im.secret_key",       env("SecretKey", ""),                            "string", "腾讯云IM SecretKey",       "即时通信", false);

        // ─── 跨域 ───
        add(list, "cors.allowed_origins", env("CORS_ALLOWED_ORIGINS", "http://localhost:3000"), "string", "允许的跨域来源(逗号分隔)", "跨域", false);

        // ─── 安全 ───
        add(list, "jwt.secret",                   env("JWT_SECRET", ""),                  "string", "JWT签名密钥",              "安全",   false);
        add(list, "security.password_min_length",  "8",                                   "number", "密码最小长度",             "安全",   true);
        add(list, "security.max_login_attempts",   "5",                                   "number", "最大登录失败次数",         "安全",   true);
        add(list, "security.captcha_enabled",      "false",                               "boolean","登录验证码开关",           "安全",   true);

        // ─── 用户 ───
        add(list, "user.free_ai_quota",            "3",                                   "number", "免费用户每日AI配额",       "用户",   true);
        add(list, "user.max_chat_history",         "100",                                 "number", "聊天记录上限",             "用户",   true);
        add(list, "user.session_timeout",          "30",                                  "number", "会话超时(分钟)",           "用户",   true);
        add(list, "user.daily_ai_calls_limit",     "20",                                  "number", "每日AI调用次数限制",       "用户",   true);
        add(list, "user.require_email_verification","false",                              "boolean","注册时强制邮箱验证",       "用户",   true);

        // ─── 通知 ───
        add(list, "notification.email_enabled",    "true",                                "boolean","邮件通知开关",             "通知",   true);
        add(list, "notification.sms_enabled",      "false",                               "boolean","短信通知开关",             "通知",   true);

        // ─── 营养师 ───
        add(list, "nutritionist.auto_approve",     "false",                               "boolean","营养师自动审核通过",       "营养师", true);
        add(list, "nutritionist.default_fee",      "98",                                  "number", "默认咨询费用(元)",         "营养师", true);
        add(list, "nutritionist.order_timeout",    "30",                                  "number", "订单支付超时(分钟)",       "营养师", true);
        add(list, "nutritionist.im_enabled",       "true",                                "boolean","即时通信开关",             "营养师", true);

        // ─── 饮食计划 ───
        add(list, "diet.plan_max_days",            "7",                                   "number", "计划最大天数",             "饮食计划", true);
        add(list, "diet.daily_calorie_min",        "1200",                                "number", "每日最低热量(kcal)",       "饮食计划", true);
        add(list, "diet.daily_calorie_max",        "3000",                                "number", "每日最高热量(kcal)",       "饮食计划", true);

        // ─── 数据库 ───
        add(list, "db.host",             env("DB_HOST", ""),                              "string", "数据库主机",               "数据库", false);
        add(list, "db.port",             env("DB_PORT", "3306"),                          "number", "数据库端口",               "数据库", false);
        add(list, "db.name",             env("DB_NAME", "nutriai"),                       "string", "数据库名称",               "数据库", false);
        add(list, "db.username",         env("DB_USERNAME", ""),                          "string", "数据库用户名",             "数据库", false);
        add(list, "db.password",         env("DB_PASSWORD", ""),                          "string", "数据库密码",               "数据库", false);

        // ─── Redis ───
        add(list, "redis.password",      env("REDIS_PASSWORD", ""),                       "string", "Redis密码",                "Redis",  false);

        return list;
    }

    // ──────────────────────────────────────────────────────

    private String env(String key, String def) {
        String val = env.getProperty(key);
        return (val != null && !val.isEmpty()) ? val : def;
    }

    private void add(List<SystemConfig> list,
                     String key, String value, String type,
                     String description, String category, boolean isPublic) {
        list.add(SystemConfig.builder()
                .configKey(key)
                .configValue(value != null ? value : "")
                .configType(type)
                .description(description)
                .category(category)
                .isPublic(isPublic)
                .build());
    }
}
