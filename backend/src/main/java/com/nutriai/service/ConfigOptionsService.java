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
                .description("使用的AI模型名称")
                .category("AI")
                .valueType("select")
                .options(Arrays.asList(
                        ConfigOptionDTO.OptionValue.builder().label("通义千问Max").value("qwen-max").build(),
                        ConfigOptionDTO.OptionValue.builder().label("通义千问Plus").value("qwen-plus").build(),
                        ConfigOptionDTO.OptionValue.builder().label("通义千问Turbo").value("qwen-turbo").build()
                ))
                .defaultValue("qwen-max")
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
                .key("user.default_member_level")
                .name("默认会员等级")
                .description("新用户注册时的默认会员等级")
                .category("用户")
                .valueType("select")
                .options(Arrays.asList(
                        ConfigOptionDTO.OptionValue.builder().label("免费会员").value("FREE").build(),
                        ConfigOptionDTO.OptionValue.builder().label("青铜会员").value("BRONZE").build(),
                        ConfigOptionDTO.OptionValue.builder().label("白银会员").value("SILVER").build(),
                        ConfigOptionDTO.OptionValue.builder().label("黄金会员").value("GOLD").build()
                ))
                .defaultValue("FREE")
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
