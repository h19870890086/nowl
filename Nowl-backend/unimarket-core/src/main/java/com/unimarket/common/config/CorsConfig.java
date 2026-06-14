package com.unimarket.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置类
 * 支持通过环境变量 UM_CORS_ALLOWED_ORIGIN_PATTERN_1、UM_CORS_ALLOWED_ORIGIN_PATTERN_2 配置额外允许的域名
 */
@Configuration
public class CorsConfig {

    @Value("${UM_CORS_ALLOWED_ORIGIN_PATTERN_1:http://localhost:*}")
    private String allowedOriginPattern1;

    @Value("${UM_CORS_ALLOWED_ORIGIN_PATTERN_2:http://127.0.0.1:*}")
    private String allowedOriginPattern2;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 允许的域名（使用allowedOriginPatterns配合allowCredentials）
        config.addAllowedOriginPattern(allowedOriginPattern1);
        config.addAllowedOriginPattern(allowedOriginPattern2);
        // Spring的allowedOriginPattern对于不带端口的Origin（如http://localhost）
        // 不会匹配带:*的通配模式，因此额外添加无端口的精确匹配
        config.addAllowedOriginPattern(extractHostOnly(allowedOriginPattern1));
        config.addAllowedOriginPattern(extractHostOnly(allowedOriginPattern2));

        // 允许所有请求方法
        config.addAllowedMethod("*");

        // 允许所有请求头
        config.addAllowedHeader("*");

        // 允许携带凭证（Cookie）
        config.setAllowCredentials(true);

        // 暴露的响应头
        config.addExposedHeader("Authorization");
        config.addExposedHeader("Set-Cookie");

        // 预检请求的有效期（秒）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }

    /**
     * 从带端口通配符的URL模式中提取不带端口的基础URL
     * 例如: "http://120.55.241.163:*" → "http://120.55.241.163"
     */
    private String extractHostOnly(String pattern) {
        if (pattern == null) return null;
        // 去掉 :*  后缀
        return pattern.replaceAll(":\\*$", "");
    }
}
