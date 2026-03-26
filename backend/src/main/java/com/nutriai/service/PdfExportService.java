package com.nutriai.service;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.nutriai.dto.DietPlanResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * PDF导出服务
 */
@Slf4j
@Service
public class PdfExportService {
    
    /**
     * 导出饮食计划为PDF
     */
    public byte[] exportDietPlanToPdf(DietPlanResponse plan) throws IOException {
        log.info("开始生成PDF: {}", plan.getTitle());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        
        try {
            // 设置中文字体
            PdfFont font;
            PdfFont boldFont;
            
            try {
                // 尝试多种中文字体
                font = loadChineseFont();
                boldFont = font; // 使用同一字体，通过setBold()来加粗
                log.info("成功加载中文字体");
            } catch (Exception e) {
                log.error("无法加载任何中文字体: {}", e.getMessage());
                throw new IOException("无法加载中文字体，PDF导出失败。请确保系统安装了中文字体。");
            }
            
            document.setFont(font);
            
            // 设置页边距
            document.setMargins(50, 50, 50, 70);
            
            // 添加封面
            addCoverPage(document, plan, font, boldFont);
            
            // 添加新页
            document.add(new com.itextpdf.layout.element.AreaBreak());
            
            // 添加内容
            addContent(document, plan, font, boldFont);
            
            // 添加页脚
            addFooter(pdf, font);
            
            log.info("PDF生成成功，页数: {}", pdf.getNumberOfPages());
            
        } catch (Exception e) {
            log.error("PDF生成失败: {}", e.getMessage(), e);
            log.error("错误类型: {}", e.getClass().getName());
            if (e.getCause() != null) {
                log.error("原因: {}", e.getCause().getMessage());
            }
            throw new IOException("PDF生成失败: " + e.getMessage(), e);
        } finally {
            document.close();
        }
        
        return baos.toByteArray();
    }
    
    /**
     * 添加封面
     */
    private void addCoverPage(Document document, DietPlanResponse plan, PdfFont font, PdfFont boldFont) {
        String title = plan.getTitle();
        String subtitle = plan.getGoalDescription();
        String daysText = "计划天数: " + plan.getDays() + " 天";
        String timeText = "生成时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String footerText = "AI健康饮食规划助手";
        
        // 标题
        Paragraph titlePara = new Paragraph(title)
            .setFont(boldFont)
            .setFontSize(32)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(150);
        document.add(titlePara);
        
        // 副标题
        Paragraph subtitlePara = new Paragraph(subtitle)
            .setFont(font)
            .setFontSize(18)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(20)
            .setFontColor(ColorConstants.DARK_GRAY);
        document.add(subtitlePara);
        
        // 计划信息
        Paragraph info = new Paragraph()
            .setFont(font)
            .setFontSize(14)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(40);
        
        info.add(new Text(daysText + "\n"));
        info.add(new Text(timeText));
        
        document.add(info);
        
        // 底部标识
        Paragraph footer = new Paragraph(footerText)
            .setFont(font)
            .setFontSize(12)
            .setTextAlignment(TextAlignment.CENTER)
            .setFixedPosition(50, 50, 500)
            .setFontColor(ColorConstants.GRAY);
        document.add(footer);
    }
    
    /**
     * 添加内容
     */
    private void addContent(Document document, DietPlanResponse plan, PdfFont font, PdfFont boldFont) {
        String content = plan.getMarkdownContent();
        
        String[] lines = content.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            
            try {
                if (line.startsWith("# ")) {
                    // 一级标题
                    Paragraph p = new Paragraph(line.substring(2))
                        .setFont(boldFont)
                        .setFontSize(24)
                        .setBold()
                        .setMarginTop(20)
                        .setMarginBottom(10);
                    document.add(p);
                    
                } else if (line.startsWith("## ")) {
                    // 二级标题
                    Paragraph p = new Paragraph(line.substring(3))
                        .setFont(boldFont)
                        .setFontSize(18)
                        .setBold()
                        .setMarginTop(15)
                        .setMarginBottom(8);
                    document.add(p);
                    
                } else if (line.startsWith("### ")) {
                    // 三级标题
                    Paragraph p = new Paragraph(line.substring(4))
                        .setFont(boldFont)
                        .setFontSize(14)
                        .setBold()
                        .setMarginTop(10)
                        .setMarginBottom(5);
                    document.add(p);
                    
                } else if (line.startsWith("- ")) {
                    // 列表项
                    Paragraph p = new Paragraph("  • " + line.substring(2))
                        .setFont(font)
                        .setFontSize(11)
                        .setMarginLeft(20);
                    document.add(p);
                    
                } else if (line.equals("---")) {
                    // 分隔线
                    document.add(new Paragraph("\n"));
                    
                } else if (line.startsWith("**") && line.endsWith("**")) {
                    // 加粗文本
                    Paragraph p = new Paragraph(line.replace("**", ""))
                        .setFont(boldFont)
                        .setFontSize(12)
                        .setBold();
                    document.add(p);
                    
                } else {
                    // 普通文本
                    Paragraph p = new Paragraph(line)
                        .setFont(font)
                        .setFontSize(11);
                    document.add(p);
                }
            } catch (Exception e) {
                // 如果某行添加失败（比如包含不支持的字符），记录并跳过
                log.warn("添加行失败，跳过: {}", line.substring(0, Math.min(50, line.length())));
            }
        }
    }
    
    /**
     * 加载中文字体
     */
    private PdfFont loadChineseFont() throws IOException {
        // 尝试多种系统中文字体路径（Windows + Linux + macOS）
        String[] fontPaths = {
            // Linux 常见路径
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc,0",        // 文泉驿正黑
            "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc,0",      // 文泉驿微米黑
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0",// Noto Sans CJK
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc,0",
            "/usr/share/fonts/noto-cjk/NotoSansCJK-Regular.ttc,0",
            "/usr/share/fonts/google-noto-cjk/NotoSansCJK-Regular.ttc,0",
            "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf",
            // Windows
            "C:/Windows/Fonts/msyh.ttc,0",
            "C:/Windows/Fonts/simhei.ttf",
            "C:/Windows/Fonts/simsun.ttc,0",
            // macOS
            "/System/Library/Fonts/PingFang.ttc,0",
            "/System/Library/Fonts/STHeiti Light.ttc,0",
        };
        
        for (String fontPath : fontPaths) {
            try {
                PdfFont font = PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H);
                log.info("成功加载字体: {}", fontPath);
                return font;
            } catch (Exception e) {
                log.debug("无法加载字体 {}: {}", fontPath, e.getMessage());
            }
        }
        
        // 尝试从 classpath 加载内嵌字体
        try {
            java.io.InputStream is = getClass().getResourceAsStream("/fonts/NotoSansSC-Regular.ttf");
            if (is != null) {
                byte[] fontBytes = is.readAllBytes();
                PdfFont font = PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H);
                log.info("成功加载内嵌字体: NotoSansSC-Regular.ttf");
                return font;
            }
        } catch (Exception e) {
            log.debug("无法加载内嵌字体: {}", e.getMessage());
        }
        
        // 最终回退：iText内置字体
        try {
            PdfFont font = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H");
            log.info("成功加载iText内置中文字体: STSong-Light");
            return font;
        } catch (Exception e) {
            log.debug("无法加载iText内置字体: {}", e.getMessage());
        }
        
        throw new IOException("无法加载任何中文字体，请安装中文字体包(如: apt-get install fonts-wqy-zenhei)");
    }
    
    /**
     * 移除中文字符（保留ASCII字符）
     */
    private String removeChinese(String text) {
        if (text == null) return "";
        // 只保留ASCII字符（包括英文、数字、标点符号）
        return text.replaceAll("[^\\x00-\\x7F]", " ");
    }
    
    /**
     * 添加页脚
     */
    private void addFooter(PdfDocument pdf, PdfFont font) {
        try {
            int numberOfPages = pdf.getNumberOfPages();
            
            for (int i = 1; i <= numberOfPages; i++) {
                PdfPage page = pdf.getPage(i);
                Rectangle pageSize = page.getPageSize();
                
                // 使用 try-with-resources 或手动管理 canvas
                PdfCanvas canvas = new PdfCanvas(page);
                try {
                    // 页码
                    canvas.beginText()
                        .setFontAndSize(font, 9)
                        .moveText(pageSize.getWidth() / 2 - 50, 30)
                        .showText(String.format("第 %d 页 / 共 %d 页", i, numberOfPages))
                        .endText();
                    
                    // 底部文字
                    canvas.beginText()
                        .setFontAndSize(font, 9)
                        .moveText(pageSize.getWidth() / 2 - 100, 15)
                        .showText("AI健康饮食规划助手 - 专业营养计划定制")
                        .endText();
                } finally {
                    canvas.release();
                }
            }
        } catch (Exception e) {
            log.warn("添加页脚失败，跳过: {}", e.getMessage());
            // 页脚不是必须的，失败了也不影响主要内容
        }
    }
}
