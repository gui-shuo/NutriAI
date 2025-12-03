# Sprint 8 WebSocket连接失败 - 故障排查

## 问题描述
WebSocket连接始终失败，错误代码1006（异常关闭）

## 已完成的修复
1. ✅ 添加`spring-boot-starter-websocket`依赖
2. ✅ 创建`WebSocketConfig`配置类
3. ✅ 创建`AIWebSocketHandler`和`SimpleTestWebSocketHandler`
4. ✅ 创建`WebSocketSessionManager`会话管理器
5. ✅ 创建`AIStreamingService`流式服务
6. ✅ 在`SecurityConfig`中添加`/ws/**`到permitAll列表
7. ✅ 修复JwtUtil验证逻辑
8. ✅ 重新编译和启动后端

## 仍然失败的症状
- 简单的`/ws/test`端点也无法连接
- 错误代码：1006
- 没有握手响应

## 可能的原因排查

### 原因1: WebSocket未启用
**检查方法**: 查看后端启动日志
**预期输出**:
```
✅ WebSocket处理器注册完成:
   - /ws/test (测试端点)
   - /ws/ai/chat (原生)
   - /ws/ai/chat-sockjs (SockJS)
```

### 原因2: Spring Security仍在拦截
**检查方法**: 查看HTTP请求日志
**可能的问题**:
- Security Filter链顺序问题
- WebSocket升级请求被当作HTTP请求处理

**解决方案**: 需要在SecurityConfig中显式处理WebSocket升级

### 原因3: 端口或协议问题
**检查**: 
- ✅ 后端运行在8080端口
- ✅ Health端点正常访问
- ❓ WebSocket协议升级是否成功

### 原因4: CORS配置问题
虽然已启用CORS，但WebSocket握手可能需要特殊处理

### 原因5: 依赖冲突
pom.xml中WebSocket依赖被添加了两次（警告信息）

## 诊断步骤

### 步骤1: 检查WebSocket是否被Spring识别

创建一个测试控制器检查WebSocket映射：

```java
@RestController
@RequestMapping("/api/debug")
public class DebugController {
    
    @Autowired
    private RequestMappingHandlerMapping handlerMapping;
    
    @GetMapping("/mappings")
    public Map<String, Object> getMappings() {
        // 返回所有映射
    }
}
```

### 步骤2: 添加WebSocket日志

在`application.yml`中添加：
```yaml
logging:
  level:
    org.springframework.web.socket: DEBUG
    org.springframework.messaging: DEBUG
    com.nutriai.websocket: DEBUG
```

### 步骤3: 使用独立HTML测试

已创建`test-ws-direct.html`，直接在浏览器中打开测试，排除前端应用的影响。

### 步骤4: 检查Security配置

可能需要显式配置WebSocket的Security：

```java
@Configuration
public class WebSocketSecurityConfig {
    @Bean
    public WebSocketMessageBrokerConfigurer webSocketMessageBrokerConfigurer() {
        return new WebSocketMessageBrokerConfigurer() {
            @Override
            public void registerStompEndpoints(StompEndpointRegistry registry) {
                registry.addEndpoint("/ws/**")
                    .setAllowedOrigins("*");
            }
        };
    }
}
```

## 待尝试的解决方案

### 方案1: 简化Security配置

临时禁用Security，测试WebSocket是否可以连接：

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .anyRequest().permitAll()  // 临时开放所有请求
        )
        .csrf(AbstractHttpConfigurer::disable);
    return http.build();
}
```

### 方案2: 使用STOMP协议

Spring Boot对STOMP over WebSocket有更好的支持：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketMessageConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins("*");
    }
}
```

### 方案3: 修复pom.xml重复依赖

删除其中一个重复的WebSocket依赖声明。

### 方案4: 添加WebSocket升级过滤器

创建一个Filter，显式处理WebSocket升级请求。

## 下一步行动

1. **立即**: 打开`test-ws-direct.html`测试
2. **如果失败**: 添加DEBUG日志并重启
3. **如果仍失败**: 尝试方案1（临时禁用Security）
4. **最后手段**: 切换到STOMP协议

## 测试命令

### 浏览器Console测试
```javascript
// 打开 test-ws-direct.html
// 点击 "Test Simple WS" 按钮
```

### cURL测试（如果有wscat）
```bash
wscat -c ws://localhost:8080/ws/test
```

### PowerShell测试
```powershell
# 使用.NET WebSocket客户端
# （需要编写脚本）
```

---

**状态**: 🔴 未解决
**优先级**: P0 - 阻塞性问题
**估计时间**: 1-2小时

---

## 更新记录
- 2025-12-03 18:45 - 创建故障排查文档
