package com.nutriai.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 验证码工具类
 */
@Slf4j
@Component
public class CaptchaUtil {
    
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LENGTH = 4;
    private static final String CODE_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    
    private final SecureRandom random = new SecureRandom();
    
    /**
     * 生成验证码文本
     */
    public String generateCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return code.toString();
    }
    
    /**
     * 生成验证码图片并返回Base64编码
     */
    public String generateCaptchaImage(String code) {
        try {
            // 创建图片缓冲区
            BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            
            // 设置抗锯齿
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // 填充背景色
            g.setColor(new Color(240, 240, 240));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            
            // 绘制干扰线
            drawInterferenceLines(g);
            
            // 绘制验证码
            drawCode(g, code);
            
            // 绘制边框
            g.setColor(Color.GRAY);
            g.drawRect(0, 0, WIDTH - 1, HEIGHT - 1);
            
            g.dispose();
            
            // 转换为Base64
            return imageToBase64(image);
        } catch (Exception e) {
            log.error("生成验证码图片失败", e);
            return null;
        }
    }
    
    /**
     * 绘制干扰线
     */
    private void drawInterferenceLines(Graphics2D g) {
        for (int i = 0; i < 8; i++) {
            int x1 = random.nextInt(WIDTH);
            int y1 = random.nextInt(HEIGHT);
            int x2 = random.nextInt(WIDTH);
            int y2 = random.nextInt(HEIGHT);
            
            g.setColor(getRandomColor());
            g.drawLine(x1, y1, x2, y2);
        }
    }
    
    /**
     * 绘制验证码文本
     */
    private void drawCode(Graphics2D g, String code) {
        int x = 10;
        for (int i = 0; i < code.length(); i++) {
            // 随机颜色
            g.setColor(getRandomColor());
            
            // 随机字体
            Font font = new Font("Arial", Font.BOLD, 28 + random.nextInt(4));
            g.setFont(font);
            
            // 随机旋转角度
            double angle = (random.nextDouble() - 0.5) * 0.5;
            g.rotate(angle, x + 12, HEIGHT / 2);
            
            // 绘制字符
            g.drawString(String.valueOf(code.charAt(i)), x, HEIGHT / 2 + 8);
            
            // 恢复旋转
            g.rotate(-angle, x + 12, HEIGHT / 2);
            
            x += 25;
        }
    }
    
    /**
     * 获取随机颜色
     */
    private Color getRandomColor() {
        return new Color(
                random.nextInt(150),
                random.nextInt(150),
                random.nextInt(150)
        );
    }
    
    /**
     * 图片转Base64
     */
    private String imageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
    }
    
    /**
     * 验证码比对（忽略大小写）
     */
    public boolean verify(String userInput, String correctCode) {
        if (userInput == null || correctCode == null) {
            return false;
        }
        return userInput.trim().equalsIgnoreCase(correctCode.trim());
    }
}
