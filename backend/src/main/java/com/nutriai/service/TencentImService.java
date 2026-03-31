package com.nutriai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriai.config.TencentImConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.Deflater;

/**
 * 腾讯云即时通信IM服务
 * 负责UserSig生成、账号导入、消息推送等
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TencentImService {

    private final TencentImConfig imConfig;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    // 缓存管理员UserSig（避免频繁生成）
    private String cachedAdminSig;
    private long adminSigExpireTime;

    /**
     * 将系统用户ID转为IM用户标识
     */
    public String toImUserId(Long userId) {
        return "user_" + userId;
    }

    /**
     * 生成UserSig
     */
    public String generateUserSig(String userId) {
        return generateUserSig(userId, imConfig.getExpire());
    }

    /**
     * 生成UserSig（指定有效期）
     */
    public String generateUserSig(String userId, long expire) {
        if (!imConfig.isEnabled()) {
            throw new RuntimeException("腾讯云IM未配置");
        }

        long currTime = System.currentTimeMillis() / 1000;

        // 1. HMAC-SHA256签名
        String sigContent = "TLS.identifier:" + userId + "\n"
                + "TLS.sdkappid:" + imConfig.getSdkAppId() + "\n"
                + "TLS.time:" + currTime + "\n"
                + "TLS.expire:" + expire + "\n";

        String sig;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    imConfig.getSecretKey().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(sigContent.getBytes(StandardCharsets.UTF_8));
            sig = Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("UserSig签名失败", e);
        }

        // 2. 组装JSON文档
        Map<String, Object> sigDoc = new LinkedHashMap<>();
        sigDoc.put("TLS.ver", "2.0");
        sigDoc.put("TLS.identifier", userId);
        sigDoc.put("TLS.sdkappid", imConfig.getSdkAppId());
        sigDoc.put("TLS.expire", expire);
        sigDoc.put("TLS.time", currTime);
        sigDoc.put("TLS.sig", sig);

        String jsonStr;
        try {
            jsonStr = objectMapper.writeValueAsString(sigDoc);
        } catch (Exception e) {
            throw new RuntimeException("UserSig JSON序列化失败", e);
        }

        // 3. Zlib压缩 + Base64编码（腾讯自定义编码）
        Deflater compressor = new Deflater();
        compressor.setInput(jsonStr.getBytes(StandardCharsets.UTF_8));
        compressor.finish();
        byte[] compressed = new byte[2048];
        int len = compressor.deflate(compressed);
        compressor.end();

        return base64EncodeUrl(Arrays.copyOf(compressed, len));
    }

    /**
     * 获取管理员UserSig（带缓存）
     */
    public String getAdminUserSig() {
        long now = System.currentTimeMillis() / 1000;
        if (cachedAdminSig != null && now < adminSigExpireTime - 300) {
            return cachedAdminSig;
        }
        cachedAdminSig = generateUserSig(imConfig.getAdminUserId(), imConfig.getExpire());
        adminSigExpireTime = now + imConfig.getExpire();
        return cachedAdminSig;
    }

    /**
     * 导入用户账号到IM（幂等操作）
     */
    public boolean importAccount(String imUserId, String nickname, String faceUrl) {
        if (!imConfig.isEnabled()) {
            log.warn("IM未启用，跳过账号导入");
            return false;
        }

        try {
            String url = buildApiUrl("/v4/im_open_login_svc/account_import");

            Map<String, Object> body = new HashMap<>();
            body.put("UserID", imUserId);
            if (nickname != null && !nickname.isBlank()) {
                body.put("Nick", nickname);
            }
            if (faceUrl != null && !faceUrl.isBlank()) {
                body.put("FaceUrl", faceUrl);
            }

            String response = postToIm(url, body);
            JsonNode result = objectMapper.readTree(response);
            int errorCode = result.path("ErrorCode").asInt(-1);

            if (errorCode == 0) {
                log.info("IM账号导入成功: {}", imUserId);
                return true;
            } else {
                log.warn("IM账号导入失败: {} - {}", errorCode, result.path("ErrorInfo").asText());
                return false;
            }
        } catch (Exception e) {
            log.error("IM账号导入异常: {}", imUserId, e);
            return false;
        }
    }

    /**
     * 发送C2C单聊消息（用于服务端推送通知）
     */
    public boolean sendC2CMessage(String fromAccount, String toAccount, String text, String orderNo) {
        if (!imConfig.isEnabled()) {
            return false;
        }

        try {
            String url = buildApiUrl("/v4/openim/sendmsg");

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("SyncOtherMachine", 1);
            body.put("From_Account", fromAccount);
            body.put("To_Account", toAccount);
            body.put("MsgLifeTime", 604800); // 7天
            body.put("MsgRandom", new Random().nextInt(Integer.MAX_VALUE));
            body.put("MsgTimeStamp", System.currentTimeMillis() / 1000);

            // 消息体
            Map<String, Object> textElem = new HashMap<>();
            textElem.put("MsgType", "TIMTextElem");
            Map<String, String> msgContent = new HashMap<>();
            msgContent.put("Text", text);
            textElem.put("MsgContent", msgContent);
            body.put("MsgBody", List.of(textElem));

            // 自定义数据（关联订单号）
            if (orderNo != null) {
                body.put("CloudCustomData", "{\"orderNo\":\"" + orderNo + "\"}");
            }

            String response = postToIm(url, body);
            JsonNode result = objectMapper.readTree(response);
            int errorCode = result.path("ErrorCode").asInt(-1);

            if (errorCode == 0) {
                log.debug("IM消息发送成功: {} -> {}", fromAccount, toAccount);
                return true;
            } else {
                log.warn("IM消息发送失败: {} - {}", errorCode, result.path("ErrorInfo").asText());
                return false;
            }
        } catch (Exception e) {
            log.error("IM消息发送异常", e);
            return false;
        }
    }

    // === 私有方法 ===

    private String buildApiUrl(String path) {
        long random = new Random().nextInt(Integer.MAX_VALUE);
        return imConfig.getApiBaseUrl() + path
                + "?sdkappid=" + imConfig.getSdkAppId()
                + "&identifier=" + imConfig.getAdminUserId()
                + "&usersig=" + getAdminUserSig()
                + "&random=" + random
                + "&contenttype=json";
    }

    private String postToIm(String url, Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String json = objectMapper.writeValueAsString(body);
            HttpEntity<String> request = new HttpEntity<>(json, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("IM API调用失败", e);
        }
    }

    /**
     * 腾讯自定义Base64URL编码：+ → *, / → -, = → _
     */
    private static String base64EncodeUrl(byte[] input) {
        byte[] base64 = Base64.getEncoder().encode(input);
        for (int i = 0; i < base64.length; i++) {
            switch (base64[i]) {
                case '+': base64[i] = '*'; break;
                case '/': base64[i] = '-'; break;
                case '=': base64[i] = '_'; break;
            }
        }
        return new String(base64, StandardCharsets.UTF_8);
    }
}
