package com.accounting.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web MVC 配置
 * 主要处理前端路由的 History 模式支持和文件上传
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.base-url:/uploads}")
    private String baseUrl;

    /**
     * 配置静态资源处理
     * 1. 上传文件目录映射
     * 2. 前端路由的 History 模式支持
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. 配置上传文件目录映射 (优先级高，先匹配)
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String uploadPathStr = uploadPath.toString().replace("\\", "/");
        
        registry.addResourceHandler(baseUrl + "/**")
                .addResourceLocations("file:" + uploadPathStr + "/")
                .setCachePeriod(86400); // 缓存 24 小时
        
        System.out.println("📂 File upload mapping: " + baseUrl + "/** -> file:" + uploadPathStr + "/");
        
        // 2. 配置前端静态资源和 SPA fallback
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        // 如果是上传文件路径，跳过（让上面的 handler 处理）
                        if (resourcePath.startsWith(baseUrl.substring(1))) {
                            return null;
                        }
                        
                        Resource requestedResource = location.createRelative(resourcePath);
                        
                        // 如果请求的资源存在，直接返回
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        
                        // 如果是 API 请求，不要 fallback
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }
                        
                        // 对于前端路由路径，返回 index.html 让前端处理
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
    
    /**
     * 为前端路由添加 Fallback
     * 处理 SPA（单页应用）刷新问题
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/dashboard").setViewName("forward:/index.html");
        registry.addViewController("/transactions").setViewName("forward:/index.html");
        registry.addViewController("/accounts").setViewName("forward:/index.html");
        registry.addViewController("/categories").setViewName("forward:/index.html");
        registry.addViewController("/payment-methods").setViewName("forward:/index.html");
        registry.addViewController("/statistics").setViewName("forward:/index.html");
        registry.addViewController("/settings").setViewName("forward:/index.html");
        registry.addViewController("/system-settings").setViewName("forward:/index.html");
    }
}
