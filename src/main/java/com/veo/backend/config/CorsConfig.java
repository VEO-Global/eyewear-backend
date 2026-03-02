package com.veo.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Áp dụng cho TOÀN BỘ các API
                        .allowedOrigins("http://localhost:3000", "http://localhost:5173") // Cấp phép cho FE (React/Vite)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Cho phép các hành động này
                        .allowedHeaders("*") // Cho phép FE gửi lên các Header (Đặc biệt là Header chứa Token JWT)
                        .allowCredentials(true); // Bắt buộc phải có để truyền Cookie/Token
            }
        };
    }
}