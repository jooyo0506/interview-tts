package com.interview.tts.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${storage.local-dir:./data/audio}")
    private String localDir;

    @Value("${storage.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 本地存储模式下的静态资源访问
        String absolutePath = new java.io.File(localDir).getAbsolutePath();
        registry.addResourceHandler("/media/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}
