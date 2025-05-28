package com.cvitae.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.cvitae.controller.AdminController;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration to capture log messages for the admin UI
 */
@Configuration
public class AdminLoggingConfig {

    @PostConstruct
    public void setupAdminLogging() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        
        // Add custom appender to capture logs for admin UI
        AdminLogAppender adminAppender = new AdminLogAppender();
        adminAppender.start();
        rootLogger.addAppender(adminAppender);
    }

    private static class AdminLogAppender extends AppenderBase<ILoggingEvent> {
        
        @Override
        protected void append(ILoggingEvent event) {
            // Only capture interesting logs (not all debug noise)
            String loggerName = event.getLoggerName();
            String level = event.getLevel().toString();
            String message = event.getFormattedMessage();
            
            // Filter for relevant logs
            if (shouldCaptureLog(loggerName, level, message)) {
                String formattedLog = String.format("[%s] %s - %s", 
                    level, 
                    loggerName.substring(loggerName.lastIndexOf('.') + 1), 
                    message);
                
                AdminController.addLog(formattedLog);
            }
        }
        
        private boolean shouldCaptureLog(String loggerName, String level, String message) {
            // Capture CVitae application logs
            if (loggerName.startsWith("com.cvitae")) {
                return true;
            }
            
            // Capture important Spring logs
            if (level.equals("ERROR") || level.equals("WARN")) {
                return true;
            }
            
            // Capture specific interesting patterns
            if (message.contains("LaTeX") || 
                message.contains("export") || 
                message.contains("resume") ||
                message.contains("API") ||
                message.contains("compilation")) {
                return true;
            }
            
            return false;
        }
    }
}
