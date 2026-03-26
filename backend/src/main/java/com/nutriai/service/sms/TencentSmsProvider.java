package com.nutriai.service.sms;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 腾讯云短信提供者
 * 使用腾讯云SMS API v3签名方式直接发送HTTP请求
 */
@Slf4j
public class TencentSmsProvider implements SmsProvider {

    private final String secretId;
    private final String secretKey;
    private final String sdkAppId;
    private final String signName;
    private final String templateId;

    private static final String SERVICE = "sms";
    private static final String HOST = "sms.tencentcloudapi.com";
    private static final String ENDPOINT = "https://" + HOST;
    private static final String ALGORITHM = "TC3-HMAC-SHA256";

    public TencentSmsProvider(String secretId, String secretKey, String sdkAppId,
                               String signName, String templateId) {
        this.secretId = secretId;
        this.secretKey = secretKey;
        this.sdkAppId = sdkAppId;
        this.signName = signName;
        this.templateId = templateId;
    }

    @Override
    public boolean sendVerificationCode(String phone, String code) {
        try {
            // 构建请求体
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("SmsSdkAppId", sdkAppId);
            body.put("SignName", signName);
            body.put("TemplateId", templateId);
            body.put("PhoneNumberSet", new String[]{"+86" + phone});
            body.put("TemplateParamSet", new String[]{code, "5"});

            String payload = JSONUtil.toJsonStr(body);
            String action = "SendSms";
            String version = "2021-01-11";

            // TC3签名
            long timestamp = System.currentTimeMillis() / 1000;
            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date(timestamp * 1000));

            // 1. 拼接规范请求串
            String httpRequestMethod = "POST";
            String canonicalUri = "/";
            String canonicalQueryString = "";
            String canonicalHeaders = "content-type:application/json; charset=utf-8\n"
                    + "host:" + HOST + "\n"
                    + "x-tc-action:" + action.toLowerCase() + "\n";
            String signedHeaders = "content-type;host;x-tc-action";
            String hashedPayload = sha256Hex(payload);
            String canonicalRequest = httpRequestMethod + "\n" + canonicalUri + "\n"
                    + canonicalQueryString + "\n" + canonicalHeaders + "\n"
                    + signedHeaders + "\n" + hashedPayload;

            // 2. 拼接待签名字符串
            String credentialScope = date + "/" + SERVICE + "/tc3_request";
            String stringToSign = ALGORITHM + "\n" + timestamp + "\n"
                    + credentialScope + "\n" + sha256Hex(canonicalRequest);

            // 3. 计算签名
            byte[] secretDate = hmac256(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
            byte[] secretService = hmac256(secretDate, SERVICE);
            byte[] secretSigning = hmac256(secretService, "tc3_request");
            String signature = bytesToHex(hmac256(secretSigning, stringToSign));

            // 4. 拼接签名头
            String authorization = ALGORITHM + " "
                    + "Credential=" + secretId + "/" + credentialScope + ", "
                    + "SignedHeaders=" + signedHeaders + ", "
                    + "Signature=" + signature;

            // 发送请求
            String response = HttpRequest.post(ENDPOINT)
                    .header("Authorization", authorization)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Host", HOST)
                    .header("X-TC-Action", action)
                    .header("X-TC-Timestamp", String.valueOf(timestamp))
                    .header("X-TC-Version", version)
                    .header("X-TC-Region", "ap-beijing")
                    .body(payload)
                    .timeout(10000)
                    .execute()
                    .body();

            log.info("腾讯云短信发送响应: phone={}, response={}", phone, response);

            // 检查响应
            if (response != null && response.contains("\"Ok\"")) {
                log.info("短信发送成功: phone={}", phone);
                return true;
            } else {
                log.error("短信发送失败: phone={}, response={}", phone, response);
                return false;
            }
        } catch (Exception e) {
            log.error("短信发送异常: phone={}", phone, e);
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "TencentCloud";
    }

    private static byte[] hmac256(byte[] key, String msg) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, mac.getAlgorithm());
        mac.init(secretKeySpec);
        return mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(d);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
