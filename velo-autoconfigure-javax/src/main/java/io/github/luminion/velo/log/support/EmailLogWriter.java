package io.github.luminion.velo.log.support;

import io.github.luminion.velo.util.InvocationUtils;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.event.Level;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 邮件异常日志写入器
 * 当发生异常时发送邮件通知
 *
 * @author luminion
 */
public class EmailLogWriter extends RequestLogWriter {

    private final String[] to;
    private final JavaMailSender mailSender;
    private final String from;
    private final Environment environment;

    public EmailLogWriter(Level level, JavaMailSender mailSender, String from,
            Environment environment, String... to) {
        super(level);
        this.to = to;
        this.mailSender = mailSender;
        this.from = from;
        this.environment = environment;
    }

    @Override
    public void writeError(MethodSignature signature, Object[] args, Throwable e) {
        super.writeError(signature, args, e);
        sendErrorEmail(signature, args, e);
    }

    private void sendErrorEmail(MethodSignature signature, Object[] args, Throwable throwable) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);

            String appName = environment != null ? environment.getProperty("spring.application.name", "N/A") : "N/A";
            helper.setSubject(String.format("Exception in %s: %s", appName, InvocationUtils.getMethodName(signature)));

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            String stackTrace = sw.toString();

            String port = environment != null ? environment.getProperty("server.port", "N/A") : "N/A";

            String body = buildHtmlEmailBody(
                    appName,
                    port,
                    getRequestInfo(),
                    InvocationUtils.getMethodName(signature),
                    InvocationUtils.formatArguments(signature, args, 2000),
                    stackTrace);

            helper.setText(body, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException ex) {
            System.err.println("Error sending email notification: " + ex.getMessage());
        }
    }

    private String buildHtmlEmailBody(String appName, String port, String requestInfo,
            String methodName, String methodArgs, String stackTrace) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body {font-family: Arial, sans-serif; margin: 20px; color: #333;}" +
                "h2 {color: #D32F2F; border-bottom: 2px solid #f2f2f2; padding-bottom: 10px;}" +
                "table {border-collapse: collapse; width: 100%; margin-top: 20px; border: 1px solid #ddd;}" +
                "th, td {text-align: left; padding: 12px; border-bottom: 1px solid #ddd;}" +
                "th {background-color: #f8f8f8; font-weight: bold; width: 120px;}" +
                "pre {white-space: pre-wrap; word-wrap: break-word; background-color:#f5f5f5;padding:15px;border-radius:4px;border:1px solid #ccc; font-family:'Courier New',Courier,monospace;}" +
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
                "</table>" +
                "<h3>Stack Trace</h3>" +
                "<pre>" + stackTrace + "</pre>" +
                "</body>" +
                "</html>";
    }
}
