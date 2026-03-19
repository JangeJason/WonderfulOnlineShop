package com.wonderful.onlineshop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AdminOperationLogInterceptor adminOperationLogInterceptor;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public WebConfig(AdminOperationLogInterceptor adminOperationLogInterceptor) {
        this.adminOperationLogInterceptor = adminOperationLogInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminOperationLogInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/operation-logs/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absolutePath);
    }
}
