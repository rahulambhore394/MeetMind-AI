package com.meetmind.meetmind_backend.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${mail.from.address:}")
    private String configuredFromAddress;

    @Value("${mail.from.name:MeetMind AI}")
    private String configuredFromName;

    @Value("${mail.replyto.address:noreply@meetmind.ai}")
    private String configuredReplyToAddress;

    private final java.util.concurrent.ExecutorService emailExecutor = java.util.concurrent.Executors.newFixedThreadPool(5);

    public void sendBatchMeetingInvitationEmailsAsync(
            java.util.List<String> recipientEmails,
            String hostName,
            String meetingTitle,
            String meetingDescription,
            String meetingCode,
            LocalDateTime scheduledAt,
            java.util.function.Function<String, Boolean> isRegisteredUserChecker
    ) {
        if (recipientEmails == null || recipientEmails.isEmpty()) return;
        
        emailExecutor.submit(() -> {
            for (String email : recipientEmails) {
                if (email == null || email.isBlank()) continue;
                try {
                    boolean isRegistered = isRegisteredUserChecker != null && Boolean.TRUE.equals(isRegisteredUserChecker.apply(email.trim()));
                    sendMeetingInvitationEmail(
                            email.trim(),
                            hostName,
                            meetingTitle,
                            meetingDescription,
                            meetingCode,
                            scheduledAt,
                            isRegistered
                    );
                } catch (Exception e) {
                    log.error("Failed batch email dispatch to {}: {}", email, e.getMessage());
                }
            }
        });
    }

    public void sendMeetingInvitationEmail(
            String recipientEmail,
            String hostName,
            String meetingTitle,
            String meetingDescription,
            String meetingCode,
            LocalDateTime scheduledAt,
            boolean isRegisteredUser
    ) {
        String safeHostName = HtmlUtils.htmlEscape(hostName != null ? hostName : "Host");
        String safeMeetingTitle = HtmlUtils.htmlEscape(meetingTitle != null ? meetingTitle : "Meeting");
        String safeDescription = meetingDescription != null ? HtmlUtils.htmlEscape(meetingDescription.trim()) : "";
        String safeMeetingCode = HtmlUtils.htmlEscape(meetingCode != null ? meetingCode : "");
        String safeRecipientEmail = HtmlUtils.htmlEscape(recipientEmail != null ? recipientEmail.trim() : "");

        String rawHostName = hostName != null ? hostName : "Host";
        String rawMeetingTitle = meetingTitle != null ? meetingTitle : "Meeting";
        String rawMeetingCode = meetingCode != null ? meetingCode : "";

        String formattedDate = scheduledAt != null 
                ? scheduledAt.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' hh:mm a"))
                : "Now / Live Meeting";

        String joinUrl = "https://meetmind-ai.vercel.app/join?code=" + safeMeetingCode;
        String appDownloadUrl = "https://meetmind-ai.vercel.app/download";

        String subject = "Meeting Invitation: " + rawMeetingTitle + " - MeetMind AI";

        // Plain Text Fallback Body for anti-spam filtering
        StringBuilder plainBody = new StringBuilder();
        plainBody.append("MeetMind AI Meeting Invitation\n\n")
                .append(rawHostName).append(" has invited you to join a live meeting on MeetMind AI.\n\n")
                .append("Meeting Title: ").append(rawMeetingTitle).append("\n")
                .append("Host: ").append(rawHostName).append("\n")
                .append("Schedule: ").append(formattedDate).append("\n");

        if (meetingDescription != null && !meetingDescription.isBlank()) {
            plainBody.append("Description: ").append(meetingDescription.trim()).append("\n");
        }

        plainBody.append("\nMeeting Access Code: ").append(rawMeetingCode).append("\n")
                .append("Join Direct Link: ").append(joinUrl).append("\n\n");

        if (!isRegisteredUser) {
            plainBody.append("First Time Joining MeetMind AI?\n")
                    .append("1. Download App: ").append(appDownloadUrl).append("\n")
                    .append("2. Register using: ").append(recipientEmail).append("\n")
                    .append("3. Enter Meeting Code: ").append(rawMeetingCode).append("\n\n");
        }

        plainBody.append("Sent by MeetMind AI Platform\n");

        StringBuilder htmlBody = new StringBuilder();
        htmlBody.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>")
                .append("<title>MeetMind AI Invitation</title>")
                .append("<style>")
                .append("@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap'); ")
                .append("body { margin: 0; padding: 0; background-color: #f8fafc; font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; } ")
                .append("</style></head>")
                .append("<body style='margin: 0; padding: 30px 10px; background-color: #f8fafc; color: #1e293b; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif;'>")
                .append("<table role='presentation' width='100%' border='0' cellspacing='0' cellpadding='0' style='width: 100%; background-color: #f8fafc;'>")
                .append("<tr><td align='center'>")
                .append("<table role='presentation' width='100%' border='0' cellspacing='0' cellpadding='0' style='max-width: 580px; background-color: #ffffff; border-radius: 20px; border: 1px solid #e2e8f0; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.05);'>")
                .append("<tr><td style='height: 5px; background: linear-gradient(90deg, #4f46e5 0%, #7c3aed 50%, #0284c7 100%);'></td></tr>")
                .append("<tr><td style='padding: 32px 36px 24px 36px; text-align: center; border-bottom: 1px solid #f1f5f9;'>")
                .append("<table role='presentation' align='center' border='0' cellspacing='0' cellpadding='0'><tr>")
                .append("<td style='background: linear-gradient(135deg, #4f46e5 0%, #4338ca 100%); border-radius: 12px; padding: 10px 14px; text-align: center;'>")
                .append("<span style='font-size: 18px; font-weight: 800; line-height: 1; color: #ffffff;'>Meet</span></td>")
                .append("<td style='padding-left: 12px; text-align: left;'>")
                .append("<span style='font-size: 22px; font-weight: 800; color: #0f172a; letter-spacing: -0.5px;'>MeetMind <span style='color: #4f46e5;'>AI</span></span>")
                .append("</td></tr></table>")
                .append("<div style='margin-top: 10px; font-size: 11px; font-weight: 700; color: #64748b; letter-spacing: 1.5px; text-transform: uppercase;'>AI-Powered Video Meeting Platform</div>")
                .append("</td></tr>")
                .append("<tr><td style='padding: 32px 36px 20px 36px;'>")
                .append("<div style='display: inline-block; background-color: #eef2ff; border: 1px solid #c7d2fe; border-radius: 20px; padding: 6px 14px; font-size: 12px; font-weight: 700; color: #4338ca; letter-spacing: 0.5px; text-transform: uppercase; margin-bottom: 16px;'>Meeting Invitation</div>")
                .append("<h1 style='margin: 0 0 12px 0; font-size: 24px; font-weight: 800; color: #0f172a; line-height: 1.3;'>Join <span style='color: #4f46e5;'>").append(safeHostName).append("</span>'s Session</h1>")
                .append("<p style='margin: 0; font-size: 15px; color: #475569; line-height: 1.6;'><strong>").append(safeHostName).append("</strong> has invited you to collaborate on <strong>MeetMind AI</strong> featuring real-time AI transcriptions and summaries.</p>")
                .append("</td></tr>")
                .append("<tr><td style='padding: 0 36px 28px 36px;'>")
                .append("<table role='presentation' width='100%' border='0' cellspacing='0' cellpadding='0' style='background-color: #f8fafc; border-radius: 16px; border: 1px solid #e2e8f0; padding: 24px;'>")
                .append("<tr><td>")
                .append("<div style='font-size: 11px; font-weight: 700; color: #4f46e5; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 6px;'>Meeting Title</div>")
                .append("<div style='font-size: 18px; font-weight: 700; color: #0f172a; margin-bottom: 18px; line-height: 1.4;'>").append(safeMeetingTitle).append("</div>")
                .append("<table role='presentation' width='100%' border='0' cellspacing='0' cellpadding='0' style='margin-bottom: 18px;'><tr>")
                .append("<td width='50%' style='padding-right: 10px; vertical-align: top;'>")
                .append("<div style='font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px;'>Host</div>")
                .append("<div style='font-size: 14px; font-weight: 600; color: #1e293b;'>").append(safeHostName).append("</div></td>")
                .append("<td width='50%' style='padding-left: 10px; vertical-align: top;'>")
                .append("<div style='font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px;'>Scheduled For</div>")
                .append("<div style='font-size: 14px; font-weight: 600; color: #1e293b;'>").append(formattedDate).append("</div></td>")
                .append("</tr></table>");

        if (!safeDescription.isEmpty()) {
            htmlBody.append("<div style='margin-bottom: 18px; background-color: #ffffff; padding: 12px 16px; border-radius: 8px; border-left: 3px solid #4f46e5; border: 1px solid #e2e8f0; border-left-width: 3px;'>")
                    .append("<div style='font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase; margin-bottom: 4px;'>Description</div>")
                    .append("<div style='font-size: 13.5px; color: #334155; line-height: 1.5;'>").append(safeDescription).append("</div>")
                    .append("</div>");
        }

        htmlBody.append("<div style='margin-top: 20px; background-color: #f0f9ff; border-radius: 12px; border: 1.5px dashed #93c5fd; padding: 16px; text-align: center;'>")
                .append("<div style='font-size: 11px; font-weight: 700; color: #0369a1; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 8px;'>Meeting Access Code</div>")
                .append("<div style='font-family: \"Courier New\", Courier, monospace; font-size: 24px; font-weight: 800; color: #0284c7; letter-spacing: 3px;'>").append(safeMeetingCode).append("</div>")
                .append("</div>")
                .append("<div style='margin-top: 24px; text-align: center;'>")
                .append("<a href='").append(joinUrl).append("' style='display: inline-block; width: 85%; background: linear-gradient(135deg, #4f46e5 0%, #4338ca 100%); color: #ffffff !important; text-decoration: none; font-size: 15px; font-weight: 700; padding: 14px 28px; border-radius: 12px; box-shadow: 0 4px 14px rgba(79, 70, 229, 0.3); letter-spacing: 0.3px;'>Join Meeting Direct Link</a>")
                .append("</div></td></tr></table></td></tr>");

        if (!isRegisteredUser) {
            htmlBody.append("<tr><td style='padding: 0 36px 28px 36px;'>")
                    .append("<table role='presentation' width='100%' border='0' cellspacing='0' cellpadding='0' style='background: #f8fafc; border-radius: 16px; border: 1px solid #e2e8f0; padding: 20px 24px;'>")
                    .append("<tr><td>")
                    .append("<div style='font-size: 14px; font-weight: 700; color: #0284c7; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 8px;'>First Time Joining MeetMind AI?</div>")
                    .append("<p style='margin: 0 0 12px 0; font-size: 13.5px; color: #475569; line-height: 1.5;'>Follow these quick steps to join from your phone or device:</p>")
                    .append("<table role='presentation' width='100%' border='0' cellspacing='0' cellpadding='0' style='font-size: 13px; color: #475569; line-height: 1.8;'>")
                    .append("<tr><td><strong style='color: #0284c7;'>1.</strong> Download the MeetMind AI App</td></tr>")
                    .append("<tr><td><strong style='color: #0284c7;'>2.</strong> Register using <strong>").append(safeRecipientEmail).append("</strong></td></tr>")
                    .append("<tr><td><strong style='color: #0284c7;'>3.</strong> Enter Meeting Code <strong style='color: #0f172a; background: #e2e8f0; padding: 2px 6px; border-radius: 4px;'>").append(safeMeetingCode).append("</strong></td></tr>")
                    .append("</table>")
                    .append("<div style='margin-top: 14px;'>")
                    .append("<a href='").append(appDownloadUrl).append("' style='display: inline-block; background-color: #0284c7; color: #ffffff !important; text-decoration: none; font-size: 13px; font-weight: 700; padding: 10px 20px; border-radius: 8px;'>Download Mobile App</a>")
                    .append("</div></td></tr></table></td></tr>");
        }

        htmlBody.append("<tr><td style='padding: 24px 36px 32px 36px; border-top: 1px solid #f1f5f9; text-align: center;'>")
                .append("<p style='margin: 0 0 8px 0; font-size: 13px; font-weight: 600; color: #64748b;'>Powered by <span style='color: #4f46e5;'>MeetMind AI</span> Platform</p>")
                .append("<p style='margin: 0; font-size: 12px; color: #94a3b8; line-height: 1.5;'>This invitation was sent to <span style='color: #64748b;'>").append(safeRecipientEmail).append("</span>.<br/>If you were not expecting this invitation, you can safely ignore this email.</p>")
                .append("</td></tr></table></td></tr></table></body></html>");

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

                String fromAddress = (configuredFromAddress != null && !configuredFromAddress.isBlank())
                        ? configuredFromAddress
                        : ((mailUsername != null && !mailUsername.isBlank()) ? mailUsername : "noreply@meetmind.ai");

                String fromName = (configuredFromName != null && !configuredFromName.isBlank())
                        ? configuredFromName
                        : "MeetMind AI";

                helper.setFrom(fromAddress, fromName);

                String replyToAddress = (configuredReplyToAddress != null && !configuredReplyToAddress.isBlank())
                        ? configuredReplyToAddress
                        : fromAddress;

                helper.setReplyTo(replyToAddress, fromName + " Support");

                helper.setTo(recipientEmail);
                helper.setSubject(subject);

                // Send multipart: text/plain + text/html for spam filter compliance
                helper.setText(plainBody.toString(), htmlBody.toString());

                // Anti-spam Headers
                message.setHeader("X-Mailer", "MeetMind AI Notification Engine");
                message.setHeader("Auto-Submitted", "auto-generated");
                message.setHeader("X-Auto-Response-Suppress", "OOF, AutoReply");

                mailSender.send(message);
                log.info("Email successfully dispatched via SMTP to {}", recipientEmail);
            } catch (Exception e) {
                log.error("SMTP mail sending failed for recipient {}: {}", recipientEmail, e.getMessage());
            }
        } else {
            log.warn("JavaMailSender is NOT configured. Skipping SMTP dispatch for {}.", recipientEmail);
        }
    }
}
