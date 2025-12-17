package io.github.luminion.autoconfigure.aop.spi.writer;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.event.Level;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * An extension of RequestLogWriter that sends an email when an error occurs.
 *
 * @author luminion
 */
public class EmailLogWriter extends RequestLogWriter {

    private final String[] to;
    private final JavaMailSender mailSender;
    private final String from;
    private final Environment environment;


    public EmailLogWriter(Level level, JavaMailSender mailSender, String from, Environment environment, String... to) {
        super(level);
        this.to = to;
        this.mailSender = mailSender;
        this.from = from;
        this.environment = environment;
    }

    @Override
    public void error(Object target, MethodSignature signature, Object[] args, Throwable throwable, long duration) {
        // First, log the error using the parent's implementation
        super.error(target, signature, args, throwable, duration);

        // Then, send an email with the error details
        sendErrorEmail(signature, args, throwable, duration);
    }

    /**
     * Sends an email with details about the error.
     *
     * @param signature the method signature
     * @param args      the method arguments
     * @param throwable the exception that was thrown
     * @param duration  the duration of the method execution
     */
    private void sendErrorEmail(MethodSignature signature, Object[] args, Throwable throwable, long duration) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);

            String appName = environment != null ? environment.getProperty("spring.application.name", "N/A") : "N/A";
            helper.setSubject(String.format("⚠️ Exception in %s: %s", appName, getMethodName(signature)));

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            String stackTrace = sw.toString();

            String port = environment != null ? environment.getProperty("server.port", "N/A") : "N/A";

            String body = buildHtmlEmailBody(
                appName,
                port,
                getRequestInfo(),
                getMethodName(signature),
                formatArgs(signature.getParameterNames(), args),
                duration,
                stackTrace
            );

            helper.setText(body, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            // Log the exception that occurred while sending email
            System.err.println("Error sending email notification: " + e.getMessage());
        }
    }

    private String buildHtmlEmailBody(String appName, String port, String requestInfo, String methodName, String methodArgs, long duration, String stackTrace) {
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
               "<tr><th>Application</th><td>" + appName + "</td></tr>" +
               "<tr><th>Port</th><td>" + port + "</td></tr>" +
               "<tr><th>Request</th><td>" + requestInfo.replace(" -", "") + "</td></tr>" +
               "<tr><th>Method</th><td>" + methodName + "</td></tr>" +
               "<tr><th>Arguments</th><td>" + methodArgs + "</td></tr>" +
               "<tr><th>Duration</th><td>" + duration + " ms</td></tr>" +
               "</table>" +
               "<h3>Stack Trace</h3>" +
               "<pre>" + stackTrace + "</pre>" +
               "</body>" +
               "</html>";
    }


 
}
