package com.meetmind.meetmind_backend.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public void sendMeetingInvitationEmail(
            String recipientEmail,
            String hostName,
            String meetingTitle,
            String meetingDescription,
            String meetingCode,
            LocalDateTime scheduledAt,
            boolean isRegisteredUser
    ) {
        String formattedDate = scheduledAt != null 
                ? scheduledAt.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' hh:mm a"))
                : "Now / Live Meeting";

        String joinUrl = "https://meetmind-ai.vercel.app/join?code=" + meetingCode;
        String appDownloadUrl = "https://meetmind-ai.vercel.app/download";

        String subject = "You're Invited to Join Meeting: " + meetingTitle + " by " + hostName;

        StringBuilder htmlBody = new StringBuilder();
        htmlBody.append("<!DOCTYPE html><html><head><style>")
                .append("body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #0b0f19; color: #ffffff; margin: 0; padding: 20px; }")
                .append(".container { max-width: 600px; background-color: #131b2e; border-radius: 16px; padding: 30px; border: 1px solid #1f2d4d; margin: 0 auto; }")
                .append(".header { text-align: center; border-bottom: 1px solid #1f2d4d; padding-bottom: 20px; }")
                .append(".logo { font-size: 24px; font-weight: bold; color: #6366f1; letter-spacing: 1px; }")
                .append(".content { padding: 20px 0; }")
                .append(".card { background-color: #1b2640; border-radius: 12px; padding: 20px; border-left: 4px solid #6366f1; margin: 15px 0; }")
                .append(".code-badge { font-size: 22px; font-weight: bold; color: #00f2fe; background: #0c162d; padding: 10px 18px; border-radius: 8px; display: inline-block; letter-spacing: 2px; }")
                .append(".btn { display: inline-block; background-color: #6366f1; color: #ffffff !important; padding: 14px 28px; text-decoration: none; border-radius: 10px; font-weight: bold; margin-top: 15px; }")
                .append(".app-box { background: #0d172a; border-radius: 12px; padding: 20px; border: 1px dashed #6366f1; margin-top: 25px; }")
                .append(".footer { font-size: 12px; color: #8a99ad; text-align: center; margin-top: 25px; border-top: 1px solid #1f2d4d; padding-top: 15px; }")
                .append("</style></head><body>")
                .append("<div class='container'>")
                .append("<div class='header'><div class='logo'>🎥 MeetMind AI</div></div>")
                .append("<div class='content'>")
                .append("<h2>Hello!</h2>")
                .append("<p><strong>").append(hostName).append("</strong> has invited you to join a live meeting on <strong>MeetMind AI</strong>.</p>")
                .append("<div class='card'>")
                .append("<h3 style='margin-top:0; color:#00f2fe;'>").append(meetingTitle).append("</h3>")
                .append("<p><strong>Host:</strong> ").append(hostName).append("</p>")
                .append("<p><strong>Schedule:</strong> ").append(formattedDate).append("</p>");

        if (meetingDescription != null && !meetingDescription.trim().isEmpty()) {
            htmlBody.append("<p><strong>Description:</strong> ").append(meetingDescription).append("</p>");
        }

        htmlBody.append("<p><strong>Meeting Code:</strong></p>")
                .append("<div class='code-badge'>").append(meetingCode).append("</div><br/>")
                .append("<a href='").append(joinUrl).append("' class='btn'>Join Meeting Direct Link 🚀</a>")
                .append("</div>");

        if (!isRegisteredUser) {
            htmlBody.append("<div class='app-box'>")
                    .append("<h4 style='margin-top:0; color:#00f2fe;'>📱 New to MeetMind AI? Download the App</h4>")
                    .append("<p>You don't have a registered MeetMind AI account yet. Follow these quick steps to join:</p>")
                    .append("<ol>")
                    .append("<li><a href='").append(appDownloadUrl).append("' style='color:#00f2fe;'>Download MeetMind AI App</a></li>")
                    .append("<li>Sign up with this email: <strong>").append(recipientEmail).append("</strong></li>")
                    .append("<li>Enter Meeting Code <strong>").append(meetingCode).append("</strong> to join the room!</li>")
                    .append("</ol>")
                    .append("</div>");
        }

        htmlBody.append("</div>")
                .append("<div class='footer'>Sent with ❤️ by MeetMind AI • AI-Powered Meeting Platform</div>")
                .append("</div></body></html>");

        log.info("=================== MEETING INVITATION EMAIL ===================");
        log.info("TO: {}", recipientEmail);
        log.info("SUBJECT: {}", subject);
        log.info("REGISTERED USER: {}", isRegisteredUser);
        log.info("MEETING CODE: {}", meetingCode);
        log.info("JOIN LINK: {}", joinUrl);
        log.info("===============================================================");

        if (mailSender != null) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setTo(recipientEmail);
                helper.setSubject(subject);
                helper.setText(htmlBody.toString(), true);
                mailSender.send(message);
                log.info("Email successfully dispatched via SMTP to {}", recipientEmail);
            } catch (Exception e) {
                log.warn("SMTP mail sending failed or unconfigured, logged email to console: {}", e.getMessage());
            }
        }
    }
}
