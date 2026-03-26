package com.nutriai.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.nutriai.config.AlipayConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付宝支付服务
 * 封装 PC网站支付（alipay.trade.page.pay）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayService {

    private final AlipayClient alipayClient;
    private final AlipayConfig alipayConfig;

    /**
     * 创建PC端支付宝支付表单/跳转URL
     *
     * @param orderNo   业务订单号
     * @param amount    金额（元）
     * @param subject   商品标题
     * @param timeoutMs 超时分钟数（如 30 表示 30m 后自动关单）
     * @return 支付宝收银台的完整 HTML Form（前端 document.write 后自动提交）
     */
    public String createPagePayForm(String orderNo, BigDecimal amount, String subject, int timeoutMs) {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(alipayConfig.getNotifyUrl());
        request.setReturnUrl(alipayConfig.getReturnUrl() + "?orderNo=" + orderNo);

        request.setBizContent(buildBizContent(orderNo, amount, subject, timeoutMs));

        try {
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            if (response.isSuccess()) {
                log.info("支付宝创建支付成功, orderNo={}", orderNo);
                return response.getBody();
            } else {
                log.error("支付宝创建支付失败, orderNo={}, code={}, msg={}",
                        orderNo, response.getCode(), response.getMsg());
                throw new RuntimeException("创建支付失败：" + response.getMsg());
            }
        } catch (AlipayApiException e) {
            log.error("支付宝API异常, orderNo={}", orderNo, e);
            throw new RuntimeException("支付宝请求异常：" + e.getErrMsg(), e);
        }
    }

    /**
     * 主动查询支付宝交易状态
     *
     * @param orderNo 业务订单号
     * @return AlipayTradeQueryResponse，调用方根据 tradeStatus 判断
     */
    public AlipayTradeQueryResponse queryTrade(String orderNo) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent("{\"out_trade_no\":\"" + orderNo + "\"}");

        try {
            AlipayTradeQueryResponse resp = alipayClient.execute(request);
            log.info("支付宝查单, orderNo={}, tradeStatus={}", orderNo, resp.getTradeStatus());
            return resp;
        } catch (AlipayApiException e) {
            log.error("支付宝查单异常, orderNo={}", orderNo, e);
            throw new RuntimeException("查询支付状态失败：" + e.getErrMsg(), e);
        }
    }

    /**
     * 验证支付宝异步回调签名
     *
     * @param params 支付宝 POST 的所有参数（Map）
     * @return true = 签名合法
     */
    public boolean verifyNotify(Map<String, String> params) {
        try {
            boolean result = AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getAlipayPublicKey(),
                    "UTF-8",
                    "RSA2"
            );
            log.info("支付宝回调签名验证结果: {}", result);
            return result;
        } catch (AlipayApiException e) {
            log.error("支付宝回调签名验证异常", e);
            return false;
        }
    }

    // ---- 私有方法 ----

    private String buildBizContent(String orderNo, BigDecimal amount, String subject, int timeoutMinutes) {
        return String.format(
                "{\"out_trade_no\":\"%s\",\"total_amount\":\"%s\",\"subject\":\"%s\"," +
                "\"product_code\":\"FAST_INSTANT_TRADE_PAY\",\"timeout_express\":\"%dm\"}",
                orderNo,
                amount.toPlainString(),
                subject,
                timeoutMinutes
        );
    }
}
