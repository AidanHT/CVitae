package com.cvitae.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import jakarta.servlet.MultipartConfigElement;

/**
 * Configuration for file upload handling
 */
@Configuration
@Slf4j
public class FileUploadConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        
        // Set maximum file size to 10MB
        factory.setMaxFileSize(DataSize.ofMegabytes(10));
        
        // Set maximum request size to 15MB (to allow for multiple files + metadata)
        factory.setMaxRequestSize(DataSize.ofMegabytes(15));
        
        // Set file size threshold (files larger than this will be written to disk)
        factory.setFileSizeThreshold(DataSize.ofKilobytes(512));
        
        log.info("✅ File upload configured - Max file: 10MB, Max request: 15MB");
        
        return factory.createMultipartConfig();
    }

    @Bean
    public MultipartResolver multipartResolver() {
        StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();
        resolver.setResolveLazily(true); // Resolve multipart requests lazily
        return resolver;
    }
}
