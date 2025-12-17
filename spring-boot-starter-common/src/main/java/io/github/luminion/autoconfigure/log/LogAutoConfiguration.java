package io.github.luminion.autoconfigure.log;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.filter.ThresholdFilter;
import io.github.luminion.autoconfigure.ConditionalOnListProperty;
import io.github.luminion.autoconfigure.log.appender.LogbackEmailAppender;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * @author luminion
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({logProperties.class})
public class LogAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty("spring.mail.username")
    @ConditionalOnListProperty("turbo.log.mail.to")
    @ConditionalOnBean({JavaMailSender.class, LoggerContext.class})
    static class LogbackGlobalErrorEmailAppenderAutoConfiguration implements ApplicationListener<ApplicationFailedEvent>, ApplicationContextAware {
        private ApplicationContext  applicationContext;
        private volatile boolean initialized = false;

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            this.applicationContext = applicationContext;
            init();
        }
        
        
        @Override
        public void onApplicationEvent(ApplicationFailedEvent event) {
          
        }

        private void init() {
            if (initialized){
                return;
            }
            logProperties logProperties = applicationContext.getBean(logProperties.class);
            Environment environment = applicationContext.getEnvironment();
            JavaMailSender mailSender = applicationContext.getBean(JavaMailSender.class);
            String[] toAddresses = logProperties.getEmail().getTo();
            String fromAddress = environment.getProperty("spring.mail.username");
            String applicationName = environment.getProperty("spring.application.name", "N/A");
            String serverPort = environment.getProperty("server.port", "N/A");
            LogbackEmailAppender emailAppender = new LogbackEmailAppender("global_error_email_sender",
                    mailSender,
                    applicationName,
                    serverPort,
                    fromAddress,
                    toAddresses
            );
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            emailAppender.setContext(context);

            // Add a filter to only process ERROR level logs
            ThresholdFilter filter = new ThresholdFilter();
            filter.setLevel(Level.ERROR.toString());
            filter.start();
            emailAppender.addFilter(filter);

            // The appender must be started before use
            emailAppender.start();

            // Wrap the email appender in an async appender
            AsyncAppender asyncAppender = new AsyncAppender();
            asyncAppender.setContext(context);
            asyncAppender.addAppender(emailAppender);
            asyncAppender.setQueueSize(256); // Set queue size
            asyncAppender.setDiscardingThreshold(0); // Never drop logs unless queue is full
            asyncAppender.start();


            // Attach the async appender to the root logger
            Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
            rootLogger.addAppender(asyncAppender);
            initialized = true;
            log.debug("LogbackEmailAppender has been added to the root logger via AsyncAppender.");
        }
        
    }

}
