package com.cvitae.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.util.List;

@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public WebClient webClient() {
        log.info("Configuring WebClient with enhanced settings...");
        
        return WebClient.builder()
                .codecs(configurer -> {
                    // Set max in-memory size to 20MB for file uploads and large responses
                    configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024);
                })
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        log.info("Configuring ObjectMapper with enhanced settings...");
        
        ObjectMapper mapper = new ObjectMapper();
        
        // Register modules
        mapper.registerModule(new JavaTimeModule());
        
        // Configure serialization
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT); // Pretty print in development
        
        // Configure deserialization
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        
        log.info("✅ ObjectMapper configured with enhanced JSON handling");
        return mapper;
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Add custom JSON converter with our ObjectMapper
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setObjectMapper(objectMapper());
        converters.add(0, jsonConverter); // Add at the beginning to have priority
        
        log.info("✅ HTTP message converters configured");
    }
}
