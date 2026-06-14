package com.unimarket.utils;

import com.unimarket.common.config.EmailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * 邮件发送工具类（QQ邮箱SMTP）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailUtils {

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    /**
     * 发送验证码邮件
     */
    public boolean sendVerifyCode(String toEmail, String code) {
        if (!emailProperties.isEnabled()) {
            log.info("【邮件发送】邮件服务已禁用，跳过发送。验证码: {}", code);
            return true;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(toEmail);
            helper.setSubject("Nowl 校园二手交易 - 验证码");

            String htmlContent = buildHtmlContent(code);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("【邮件发送】验证码已发送至: {}", maskEmail(toEmail));
            return true;
        } catch (MessagingException e) {
            log.error("【邮件发送】发送失败: {}", e.getMessage());
            return false;
        }
    }

    private String buildHtmlContent(String code) {
        return """
            <div style="max-width:480px;margin:0 auto;padding:24px;font-family:Arial,sans-serif;
                        background:#fff;border-radius:12px;box-shadow:0 2px 12px rgba(0,0,0,0.08);">
                <div style="text-align:center;padding:16px 0;">
                    <h2 style="color:#f97316;margin:0;">Nowl 校园二手交易</h2>
                </div>
                <div style="background:#fff7ed;border-radius:8px;padding:24px;margin:16px 0;
                            text-align:center;">
                    <p style="color:#666;font-size:14px;margin:0 0 12px;">你的验证码（5分钟内有效）</p>
                    <div style="font-size:32px;font-weight:bold;color:#f97316;letter-spacing:6px;">
                        %s
                    </div>
                </div>
                <p style="color:#999;font-size:12px;text-align:center;margin:16px 0 0;">
                    如果这不是你本人操作，请忽略此邮件。
                </p>
            </div>
            """.formatted(code);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        String name = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (name.length() <= 2) {
            return name.charAt(0) + "***" + domain;
        }
        return name.substring(0, 2) + "***" + name.charAt(name.length() - 1) + domain;
    }
}
