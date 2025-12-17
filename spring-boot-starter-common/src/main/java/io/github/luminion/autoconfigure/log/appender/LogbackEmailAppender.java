package io.github.luminion.autoconfigure.log.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import io.github.luminion.autoconfigure.aop.spi.writer.EmailLogWriter;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A custom Logback Appender that sends an email for ERROR level events.
 * This class is NOT a Spring Bean. It is instantiated and managed programmatically.
 *
 * @author luminion
 */
@RequiredArgsConstructor
public class LogbackEmailAppender extends AppenderBase<ILoggingEvent> {

    private final JavaMailSender mailSender;
    private final String applicationName;
    private final String servletPort;
    private final String from;
    private final String[] to;

    public LogbackEmailAppender(String appenderName, JavaMailSender mailSender, String applicationName, String servletPort, String from, String[] to) {
        this.mailSender = mailSender;
        this.applicationName = applicationName;
        this.servletPort = servletPort;
        this.from = from;
        this.to = to;
        // The appender name is required by Logback
        setName(appenderName);
    }

    @Override
    protected void append(ILoggingEvent event) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(String.format("⚠️ Error in %s: %s", applicationName, event.getLoggerName()));
            String stackTrace = "No stack trace available.";
            if (event.getThrowableProxy() instanceof ThrowableProxy) {
                ThrowableProxy throwableProxy = (ThrowableProxy) event.getThrowableProxy();
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwableProxy.getThrowable().printStackTrace(pw);
                stackTrace = sw.toString();
            }
            String body = buildHtmlEmailBody( event.getLoggerName(), event.getFormattedMessage(), stackTrace);
            helper.setText(body, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            // Log to stderr to avoid infinite loops if email sending fails
            System.err.println("Error sending email notification from Appender: " + e.getMessage());
            addError("Error sending email notification", e);
        }
    }

    private String buildHtmlEmailBody(String loggerName, String message, String stackTrace) {
        return "<!DOCTYPE html>" +
               "<html>" +
               "<head>" +
               "<style>" +
               "body {font-family: Arial, sans-serif; margin: 20px; color: #333;}" +
               "h2 {color: #D32F2F; border-bottom: 2px solid #f2f2f2; padding-bottom: 10px;}" +
               "table {border-collapse: collapse; width: 100%; margin-top: 20px; border: 1px solid #ddd;}" +
               "th, td {text-align: left; padding: 12px; border-bottom: 1px solid #ddd;}" +
               "th {background-color: #f8f8f8; font-weight: bold; width: 120px;}" +
               "pre {white-space: pre-wrap; word-wrap: break-word; background-color: #f5f5f5; padding: 15px; border-radius: 4px; border: 1px solid #ccc; font-family: 'Courier New', Courier, monospace;}" +
               "</style>" +
               "</head>" +
               "<body>" +
               "<h2>Application Error Report</h2>" +
               "<table>" +
               "<tr><th>Application</th><td>" + applicationName + "</td></tr>" +
               "<tr><th>Port</th><td>" + servletPort + "</td></tr>" +
               "<tr><th>Logger</th><td>" + loggerName + "</td></tr>" +
               "<tr><th>Message</th><td>" + message + "</td></tr>" +
               "</table>" +
               "<h3>Stack Trace</h3>" +
               "<pre>" + stackTrace + "</pre>" +
               "</body>" +
               "</html>";
    }

    public static void main(String[] args) throws Exception {
        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
        javaMailSender.setHost("smtp.163.com");
        javaMailSender.setUsername("bootystar@163.com");
        String pwd = System.getenv("163_MAIL_PWD");
        System.out.println(pwd);
        javaMailSender.setPassword(pwd);

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setFrom("bootystar@163.com");
        helper.setTo("604647004@qq.com"); // 收件人
        
        String msg = "Failed to process user order due to insufficient inventory";
        String stackTrace = "java.lang.IllegalStateException: Insufficient stock for item 7\n" +
                "\tat com.example.service.OrderService.createOrder(OrderService.java:115)\n" +
                "\tat com.example.controller.OrderController.placeOrder(OrderController.java:52)\n" +
                "\t... (full stack trace)";
     
        String appName = "test-app";
        String port = "8080";
        String loggerName = "test logger";
        helper.setSubject(String.format("⚠️ Error in %s: %s", appName, loggerName));
        LogbackEmailAppender aoo = new LogbackEmailAppender("111", javaMailSender,appName,port, "123", null);
        String body = aoo.buildHtmlEmailBody(loggerName, msg, stackTrace);

        helper.setText(body, true);
        javaMailSender.send(mimeMessage);
        System.out.println(javaMailSender.getProtocol());
        System.out.println(javaMailSender.getPort());
        System.out.println("Test email sent successfully!");
    }
}
