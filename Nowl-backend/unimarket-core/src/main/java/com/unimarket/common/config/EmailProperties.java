package com.unimarket.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邮件服务开关配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "mail")
public class EmailProperties {

    /**
     * 是否启用邮件发送（false时验证码打印到日志）
     */
    private boolean enabled = false;
}
