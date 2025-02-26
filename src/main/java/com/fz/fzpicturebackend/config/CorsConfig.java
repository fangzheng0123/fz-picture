package com.fz.fzpicturebackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Author fang
 * @Date 2025/2/26 14:06
 * @注释  全局跨域配置
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry){
        registry.addMapping("/**")
                .allowedHeaders("*")
                .allowCredentials(true)
                .allowedOriginPatterns("*")
                .allowedMethods("GET","POST","OPTIONS","DELETE","PUT","PATCH")
                .exposedHeaders("*");
    }
}
