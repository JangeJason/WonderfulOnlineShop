package com.wonderful.onlineshop.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            SaRouter.match("/admin/**").check(r -> StpUtil.checkLogin());
            SaRouter.match("/api/cart/**").check(r -> StpUtil.checkLogin());
            SaRouter.match("/api/orders/**").check(r -> StpUtil.checkLogin());
            SaRouter.match("/api/upload").check(r -> StpUtil.checkLogin());
        }))
                .addPathPatterns("/**")
                .excludePathPatterns("/api/auth/**", "/api/products/**", "/api/quote/**", "/uploads/**",
                        "/api/orders/*/custom-name");
    }
}
