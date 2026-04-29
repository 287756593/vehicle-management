package com.company.vehicle.config;

import com.company.vehicle.service.UploadStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.time.Duration;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = System.getProperty("user.dir");
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/uploads/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        // 防止路径遍历攻击：检查路径中是否包含 ../ 或 ..\
                        if (resourcePath.contains("..") || resourcePath.contains("./") || resourcePath.contains(".\\")) {
                            return null;
                        }

                        // 规范化路径
                        String normalizedPath = resourcePath.replaceAll("//+", "/");

                        Resource resource = location.createRelative(normalizedPath);

                        // 验证资源路径是否在允许的目录内
                        if (resource.exists() && resource.isReadable()) {
                            String canonicalLocation = location.getFile().getCanonicalPath();
                            String canonicalResource = resource.getFile().getCanonicalPath();
                            if (!canonicalResource.startsWith(canonicalLocation)) {
                                return null;
                            }
                            return resource;
                        }

                        String originalPath = UploadStorageService.resolveOriginalPathFromThumbnail("/uploads/" + normalizedPath);
                        if (originalPath.equals("/uploads/" + normalizedPath)) {
                            return null;
                        }

                        String fallbackPath = originalPath.substring("/uploads/".length());
                        if (fallbackPath.contains("..") || fallbackPath.contains("./") || fallbackPath.contains(".\\")) {
                            return null;
                        }

                        Resource fallback = location.createRelative(fallbackPath);
                        if (fallback.exists() && fallback.isReadable()) {
                            String canonicalLocation = location.getFile().getCanonicalPath();
                            String canonicalFallback = fallback.getFile().getCanonicalPath();
                            if (!canonicalFallback.startsWith(canonicalLocation)) {
                                return null;
                            }
                            return fallback;
                        }
                        return null;
                    }
                });
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
}
