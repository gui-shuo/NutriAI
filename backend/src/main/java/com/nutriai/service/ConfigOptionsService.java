package com.nutriai.service;

import com.nutriai.dto.admin.ConfigOptionDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 配置选项服务 - 提供所有可用的配置项
 */
@Service
public class ConfigOptionsService {
    
    /**
     * 获取所有可用的配置选项
     */
    public List<ConfigOptionDTO> getAllConfigOptions() {
        List<ConfigOptionDTO> options = new ArrayList<>();
        
        // AI配置
        options.add(ConfigOptionDTO.builder()
                .key("ai.model")
                .name("AI模型")
                .description("使用的AI模型名称（火山引擎Coding套餐）")
                .category("AI")
                .valueType("select")
                .options(Arrays.asList(
                        ConfigOptionDTO.OptionValue.builder().label("Kimi K2.5（推荐）").value("kimi-k2.5").build(),
                        ConfigOptionDTO.OptionValue.builder().label("DeepSeek V3.2").value("deepseek-v3.2").build(),
                        ConfigOptionDTO.OptionValue.builder().label("GLM 4.7").value("glm-4.7").build(),
                        ConfigOptionDTO.OptionValue.builder().label("MiniMax M2.5").value("minimax-m2.5").build(),
                        ConfigOptionDTO.OptionValue.builder().label("豆包 Seed 2.0 Pro").value("doubao-seed-2.0-pro").build(),
                        ConfigOptionDTO.OptionValue.builder().label("豆包 Seed 2.0 Lite").value("doubao-seed-2.0-lite").build(),
                        ConfigOptionDTO.OptionValue.builder().label("豆包 Seed Code").value("doubao-seed-code").build(),
                        ConfigOptionDTO.OptionValue.builder().label("豆包 Seed 2.0 Code").value("doubao-seed-2.0-code").build()
                ))
                .defaultValue("kimi-k2.5")
                .required(true)
                .build());
        
        options.add(ConfigOptionDTO.builder()
                .key("ai.max_tokens")
                .name("最大Token数")
                .description("AI响应的最大Token数量")
                .category("AI")
                .valueType("number")
                .defaultValue("2000")
                .required(true)
                .build());
        
        options.add(ConfigOptionDTO.builder()
                .key("ai.temperature")
                .name("温度参数")
                .description("控制AI响应的随机性，范围0-1")
                .category("AI")
                .valueType("number")
                .defaultValue("0.7")
                .required(true)
                .build());
        
        // 系统配置
        options.add(ConfigOptionDTO.builder()
                .key("system.site_name")
                .name("网站名称")
                .description("系统显示的网站名称")
                .category("系统")
                .valueType("string")
                .defaultValue("AI健康饮食规划助手")
                .required(true)
                .build());
        
        options.add(ConfigOptionDTO.builder()
                .key("system.site_description")
                .name("网站描述")
                .description("网站的简短描述")
                .category("系统")
                .valueType("string")
                .defaultValue("智能营养分析 · 个性化饮食方案 · 健康管理")
                .required(false)
                .build());
        
        options.add(ConfigOptionDTO.builder()
                .key("system.contact_email")
                .name("联系邮箱")
                .description("客服联系邮箱")
                .category("系统")
                .valueType("string")
                .defaultValue("support@nutriai.com")
                .required(false)
                .build());
        
        options.add(ConfigOptionDTO.builder()
                .key("system.support_phone")
                .name("客服电话")
                .description("客服联系电话")
                .category("系统")
                .valueType("string")
                .defaultValue("400-123-4567")
                .required(false)
                .build());
        
        options.add(ConfigOptionDTO.builder()
                .key("system.copyright_text")
                .name("版权信息")
                .description("页脚显示的版权信息")
                .category("系统")
                .valueType("string")
                .defaultValue("© 2025 AI健康饮食规划助手. All rights reserved.")
                .required(false)
                .build());
        
        options.add(ConfigOptionDTO.builder()
                .key("system.icp_number")
                .name("ICP备案号")
                .description("网站ICP备案号")
                .category("系统")
                .valueType("string")
                .defaultValue("")
                .required(false)
                .build());
        
        options.add(ConfigOptionDTO.builder()
                .key("system.maintenance_mode")
                .name("维护模式")
                .description("是否开启系统维护模式")
                .category("系统")
                .valueType("select")
                .options(Arrays.asList(
                        ConfigOptionDTO.OptionValue.builder().label("开启").value("true").build(),
                        ConfigOptionDTO.OptionValue.builder().label("关闭").value("false").build()
                ))
                .defaultValue("false")
                .required(true)
                .build());
        
        options.add(ConfigOptionDTO.builder()
                .key("system.max_upload_size")
                .name("最大上传大小")
                .description("文件上传的最大大小（MB）")
                .category("系统")
                .valueType("number")
                .defaultValue("10")
                .required(true)
                .build());
        
        options.add(ConfigOptionDTO.builder()
                .key("system.enable_registration")
                .name("开放注册")
                .description("是否允许新用户注册")
                .category("系统")
                .valueType("select")
                .options(Arrays.asList(
                        ConfigOptionDTO.OptionValue.builder().label("开启").value("true").build(),
                        ConfigOptionDTO.OptionValue.builder().label("关闭").value("false").build()
                ))
                .defaultValue("true")
                .required(true)
                .build());
        
        // 用户配置
        options.add(ConfigOptionDTO.builder()
                .key("user.free_ai_quota")
                .name("免费用户AI配额")
                .description("免费用户每日AI咨询次数（VIP用户配额由套餐定义）")
                .category("用户")
                .valueType("number")
                .defaultValue("3")
                .required(true)
                .build());
        
        options.add(ConfigOptionDTO.builder()
                .key("user.max_chat_history")
                .name("最大对话历史")
                .description("用户可保存的最大对话历史数量")
                .category("用户")
                .valueType("number")
                .defaultValue("100")
                .required(true)
                .build());
        
        options.add(ConfigOptionDTO.builder()
                .key("user.session_timeout")
                .name("会话超时时间")
                .description("用户会话超时时间（分钟）")
                .category("用户")
                .valueType("number")
                .defaultValue("30")
                .required(true)
                .build());
        
        options.add(ConfigOptionDTO.builder()
                .key("user.daily_ai_calls_limit")
                .name("每日AI调用限制")
                .description("免费用户每日AI调用次数限制")
                .category("用户")
                .valueType("number")
                .defaultValue("20")
                .required(true)
                .build());
        
        options.add(ConfigOptionDTO.builder()
                .key("user.enable_email_verification")
                .name("邮箱验证")
                .description("是否要求用户验证邮箱")
                .category("用户")
                .valueType("select")
                .options(Arrays.asList(
                        ConfigOptionDTO.OptionValue.builder().label("开启").value("true").build(),
                        ConfigOptionDTO.OptionValue.builder().label("关闭").value("false").build()
                ))
                .defaultValue("false")
                .required(true)
                .build());
        
        // 安全配置
        options.add(ConfigOptionDTO.builder()
                .key("security.password_min_length")
                .name("密码最小长度")
                .description("用户密码的最小长度")
                .category("安全")
                .valueType("number")
                .defaultValue("8")
                .required(true)
                .build());
        
        options.add(ConfigOptionDTO.builder()
                .key("security.max_login_attempts")
                .name("最大登录尝试次数")
                .description("允许的最大登录失败次数")
                .category("安全")
                .valueType("number")
                .defaultValue("5")
                .required(true)
                .build());
        
        options.add(ConfigOptionDTO.builder()
                .key("security.enable_captcha")
                .name("启用验证码")
                .description("是否在登录时启用验证码")
                .category("安全")
                .valueType("select")
                .options(Arrays.asList(
                        ConfigOptionDTO.OptionValue.builder().label("启用").value("true").build(),
                        ConfigOptionDTO.OptionValue.builder().label("禁用").value("false").build()
                ))
                .defaultValue("false")
                .required(true)
                .build());
        
        // 通知配置
        options.add(ConfigOptionDTO.builder()
                .key("notification.email_enabled")
                .name("邮件通知")
                .description("是否启用邮件通知功能")
                .category("通知")
                .valueType("select")
                .options(Arrays.asList(
                        ConfigOptionDTO.OptionValue.builder().label("启用").value("true").build(),
                        ConfigOptionDTO.OptionValue.builder().label("禁用").value("false").build()
                ))
                .defaultValue("false")
                .required(true)
                .build());
        
        options.add(ConfigOptionDTO.builder()
                .key("notification.sms_enabled")
                .name("短信通知")
                .description("是否启用短信通知功能")
                .category("通知")
                .valueType("select")
                .options(Arrays.asList(
                        ConfigOptionDTO.OptionValue.builder().label("启用").value("true").build(),
                        ConfigOptionDTO.OptionValue.builder().label("禁用").value("false").build()
                ))
                .defaultValue("false")
                .required(true)
                .build());

        // 食物识别配置
        options.add(ConfigOptionDTO.builder()
                .key("recognition.image_max_size_mb")
                .name("图片最大上传大小")
                .description("食物识别图片最大上传大小（MB）")
                .category("食物识别")
                .valueType("number")
                .defaultValue("5")
                .required(true)
                .build());

        options.add(ConfigOptionDTO.builder()
                .key("recognition.dish_enabled")
                .name("菜品识别")
                .description("是否启用百度菜品识别功能")
                .category("食物识别")
                .valueType("select")
                .options(Arrays.asList(
                        ConfigOptionDTO.OptionValue.builder().label("启用").value("true").build(),
                        ConfigOptionDTO.OptionValue.builder().label("禁用").value("false").build()
                ))
                .defaultValue("true")
                .required(true)
                .build());

        options.add(ConfigOptionDTO.builder()
                .key("recognition.ingredient_enabled")
                .name("果蔬识别")
                .description("是否启用百度果蔬识别功能")
                .category("食物识别")
                .valueType("select")
                .options(Arrays.asList(
                        ConfigOptionDTO.OptionValue.builder().label("启用").value("true").build(),
                        ConfigOptionDTO.OptionValue.builder().label("禁用").value("false").build()
                ))
                .defaultValue("true")
                .required(true)
                .build());

        options.add(ConfigOptionDTO.builder()
                .key("recognition.confidence_threshold")
                .name("识别置信度阈值")
                .description("低于此阈值的识别结果将被过滤（0-1）")
                .category("食物识别")
                .valueType("number")
                .defaultValue("0.6")
                .required(true)
                .build());

        // 营养师咨询配置
        options.add(ConfigOptionDTO.builder()
                .key("consultation.auto_approve_nutritionist")
                .name("营养师自动审核")
                .description("营养师注册后是否自动通过审核")
                .category("营养师")
                .valueType("select")
                .options(Arrays.asList(
                        ConfigOptionDTO.OptionValue.builder().label("自动通过").value("true").build(),
                        ConfigOptionDTO.OptionValue.builder().label("需人工审核").value("false").build()
                ))
                .defaultValue("false")
                .required(true)
                .build());

        options.add(ConfigOptionDTO.builder()
                .key("consultation.default_fee")
                .name("默认咨询费用")
                .description("营养师咨询默认费用（元）")
                .category("营养师")
                .valueType("number")
                .defaultValue("98")
                .required(true)
                .build());

        options.add(ConfigOptionDTO.builder()
                .key("consultation.order_expire_minutes")
                .name("订单超时时间")
                .description("咨询订单未支付超时时间（分钟）")
                .category("营养师")
                .valueType("number")
                .defaultValue("30")
                .required(true)
                .build());

        options.add(ConfigOptionDTO.builder()
                .key("consultation.im_enabled")
                .name("即时通信")
                .description("是否启用腾讯云IM即时通信")
                .category("营养师")
                .valueType("select")
                .options(Arrays.asList(
                        ConfigOptionDTO.OptionValue.builder().label("启用").value("true").build(),
                        ConfigOptionDTO.OptionValue.builder().label("禁用（使用轮询）").value("false").build()
                ))
                .defaultValue("true")
                .required(true)
                .build());

        // 饮食计划配置
        options.add(ConfigOptionDTO.builder()
                .key("diet.plan_max_days")
                .name("计划最大天数")
                .description("饮食计划最大可生成天数")
                .category("饮食计划")
                .valueType("number")
                .defaultValue("7")
                .required(true)
                .build());

        options.add(ConfigOptionDTO.builder()
                .key("diet.daily_calorie_min")
                .name("每日最低热量")
                .description("饮食计划每日最低热量建议（kcal）")
                .category("饮食计划")
                .valueType("number")
                .defaultValue("1200")
                .required(true)
                .build());

        options.add(ConfigOptionDTO.builder()
                .key("diet.daily_calorie_max")
                .name("每日最高热量")
                .description("饮食计划每日最高热量建议（kcal）")
                .category("饮食计划")
                .valueType("number")
                .defaultValue("3000")
                .required(true)
                .build());
        
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
}
