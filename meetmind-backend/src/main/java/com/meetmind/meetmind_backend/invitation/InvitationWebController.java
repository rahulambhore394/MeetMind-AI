package com.meetmind.meetmind_backend.invitation;

import com.meetmind.meetmind_backend.meeting.Meeting;
import com.meetmind.meetmind_backend.meeting.MeetingRepository;
import com.meetmind.meetmind_backend.meeting.MeetingStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Controller
public class InvitationWebController {

    private final MeetingRepository meetingRepository;

    public InvitationWebController(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    @GetMapping(value = {"/invite/{code}", "/join/{code}"}, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String renderInvitationLandingPage(@PathVariable String code) {
        String safeCode = HtmlUtils.htmlEscape(code != null ? code.trim() : "");
        Optional<Meeting> meetingOpt = meetingRepository.findByMeetingCode(safeCode);
        if (meetingOpt.isEmpty()) {
            try {
                Long id = Long.parseLong(safeCode);
                meetingOpt = meetingRepository.findById(id);
            } catch (Exception ignored) {}
        }

        String title = "MeetMind AI Meeting";
        String host = "MeetMind Host";
        String schedule = "Live / Instant Room";
        String description = "";
        boolean isLive = false;

        if (meetingOpt.isPresent()) {
            Meeting m = meetingOpt.get();
            title = HtmlUtils.htmlEscape(m.getTitle() != null ? m.getTitle() : "Meeting Session");
            if (m.getHost() != null && m.getHost().getName() != null) {
                host = HtmlUtils.htmlEscape(m.getHost().getName());
            }
            if (m.getDescription() != null && !m.getDescription().isBlank()) {
                description = HtmlUtils.htmlEscape(m.getDescription());
            }
            isLive = m.getStatus() == MeetingStatus.LIVE;
            if (m.getScheduledAt() != null) {
                schedule = m.getScheduledAt().format(DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy 'at' hh:mm a"));
            }
        }

        String rawCode = meetingOpt.map(Meeting::getMeetingCode).orElse(safeCode);
        String deepLink = "meetmind://invite/" + rawCode;
        String webJoinUrl = "https://meetmind-ai.vercel.app/join?code=" + rawCode;
        String downloadUrl = "https://github.com/rahulambhore394/MeetMind-AI/releases";

        return "<!DOCTYPE html>" +
                "<html lang='en'>" +
                "<head>" +
                "  <meta charset='UTF-8'>" +
                "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "  <title>" + title + " - MeetMind AI Invitation</title>" +
                "  <link rel='preconnect' href='https://fonts.googleapis.com'>" +
                "  <link rel='preconnect' href='https://fonts.gstatic.com' crossorigin>" +
                "  <link href='https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800;900&display=swap' rel='stylesheet'>" +
                "  <style>" +
                "    * { box-sizing: border-box; margin: 0; padding: 0; }" +
                "    body {" +
                "      font-family: 'Plus Jakarta Sans', -apple-system, sans-serif;" +
                "      background-color: #0b0f19;" +
                "      color: #f1f5f9;" +
                "      min-height: 100vh;" +
                "      display: flex;" +
                "      align-items: center;" +
                "      justify-content: center;" +
                "      padding: 24px 16px;" +
                "      background-image: radial-gradient(circle at top right, rgba(79, 70, 229, 0.15) 0%, transparent 50%), radial-gradient(circle at bottom left, rgba(6, 182, 212, 0.12) 0%, transparent 50%);" +
                "    }" +
                "    .container {" +
                "      width: 100%;" +
                "      max-width: 480px;" +
                "      background: #111827;" +
                "      border: 1px solid rgba(255, 255, 255, 0.1);" +
                "      border-radius: 28px;" +
                "      padding: 32px 28px;" +
                "      box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.7);" +
                "      text-align: center;" +
                "    }" +
                "    .logo-badge {" +
                "      display: inline-flex;" +
                "      align-items: center;" +
                "      gap: 10px;" +
                "      background: rgba(79, 70, 229, 0.15);" +
                "      border: 1px solid rgba(79, 70, 229, 0.3);" +
                "      padding: 8px 16px;" +
                "      border-radius: 999px;" +
                "      margin-bottom: 24px;" +
                "    }" +
                "    .logo-text { font-size: 14px; font-weight: 800; color: #818cf8; }" +
                "    h1 { font-size: 24px; font-weight: 800; color: #ffffff; margin-bottom: 8px; line-height: 1.3; }" +
                "    .host-sub { font-size: 14px; color: #94a3b8; margin-bottom: 24px; }" +
                "    .meeting-card {" +
                "      background: rgba(15, 23, 42, 0.6);" +
                "      border: 1px solid rgba(255, 255, 255, 0.08);" +
                "      border-radius: 18px;" +
                "      padding: 20px;" +
                "      margin-bottom: 24px;" +
                "      text-align: left;" +
                "    }" +
                "    .meta-row { display: flex; justify-content: space-between; margin-bottom: 12px; }" +
                "    .meta-row:last-child { margin-bottom: 0; }" +
                "    .meta-label { font-size: 12px; font-weight: 600; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; }" +
                "    .meta-val { font-size: 13.5px; font-weight: 700; color: #e2e8f0; }" +
                "    .code-box {" +
                "      background: rgba(6, 182, 212, 0.08);" +
                "      border: 1.5px dashed rgba(6, 182, 212, 0.4);" +
                "      border-radius: 14px;" +
                "      padding: 12px;" +
                "      margin-top: 14px;" +
                "      display: flex;" +
                "      align-items: center;" +
                "      justify-content: space-between;" +
                "    }" +
                "    .code-text { font-family: monospace; font-size: 18px; font-weight: 800; color: #38bdf8; letter-spacing: 2px; }" +
                "    .btn-primary {" +
                "      display: flex;" +
                "      align-items: center;" +
                "      justify-content: center;" +
                "      width: 100%;" +
                "      background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);" +
                "      color: #ffffff;" +
                "      font-size: 16px;" +
                "      font-weight: 700;" +
                "      padding: 16px 20px;" +
                "      border-radius: 16px;" +
                "      text-decoration: none;" +
                "      border: none;" +
                "      cursor: pointer;" +
                "      margin-bottom: 12px;" +
                "      box-shadow: 0 10px 20px -5px rgba(79, 70, 229, 0.4);" +
                "      transition: transform 0.15s ease;" +
                "    }" +
                "    .btn-primary:active { transform: scale(0.98); }" +
                "    .btn-download {" +
                "      display: flex;" +
                "      align-items: center;" +
                "      justify-content: center;" +
                "      width: 100%;" +
                "      background: rgba(16, 185, 129, 0.15);" +
                "      border: 1.5px solid rgba(16, 185, 129, 0.4);" +
                "      color: #34d399;" +
                "      font-size: 14.5px;" +
                "      font-weight: 700;" +
                "      padding: 14px 20px;" +
                "      border-radius: 16px;" +
                "      text-decoration: none;" +
                "      margin-bottom: 12px;" +
                "    }" +
                "    .btn-web {" +
                "      display: inline-block;" +
                "      color: #94a3b8;" +
                "      font-size: 13px;" +
                "      font-weight: 600;" +
                "      text-decoration: none;" +
                "      padding: 8px 12px;" +
                "    }" +
                "    .btn-web:hover { color: #f1f5f9; text-decoration: underline; }" +
                "    .status-badge { display: inline-block; font-size: 11px; font-weight: 800; padding: 3px 8px; border-radius: 6px; }" +
                "    .status-live { background: rgba(16, 185, 129, 0.2); color: #34d399; }" +
                "    .status-sched { background: rgba(99, 102, 241, 0.2); color: #818cf8; }" +
                "  </style>" +
                "</head>" +
                "<body>" +
                "  <div class='container'>" +
                "    <div class='logo-badge'>" +
                "      <span style='font-size:16px;'>🚀</span>" +
                "      <span class='logo-text'>MeetMind AI</span>" +
                "    </div>" +
                "    <h1>" + title + "</h1>" +
                "    <p class='host-sub'>Invited by <strong style='color:#f8fafc;'>" + host + "</strong></p>" +
                "    <div class='meeting-card'>" +
                "      <div class='meta-row'>" +
                "        <span class='meta-label'>Status</span>" +
                (isLive ? "<span class='status-badge status-live'>● LIVE NOW</span>" : "<span class='status-badge status-sched'>📅 SCHEDULED</span>") +
                "      </div>" +
                "      <div class='meta-row'>" +
                "        <span class='meta-label'>When</span>" +
                "        <span class='meta-val'>" + schedule + "</span>" +
                "      </div>" +
                (description.isEmpty() ? "" : "<div class='meta-row' style='margin-top:10px;'><span class='meta-label'>Agenda</span><span class='meta-val' style='max-width:260px; text-align:right;'>" + description + "</span></div>") +
                "      <div class='code-box'>" +
                "        <span class='meta-label' style='color:#0284c7;'>Meeting Code</span>" +
                "        <span class='code-text'>" + rawCode + "</span>" +
                "      </div>" +
                "    </div>" +
                "    <a href='" + deepLink + "' class='btn-primary'>" +
                "      <span>📱 Open & Accept in MeetMind App</span>" +
                "    </a>" +
                "    <a href='" + downloadUrl + "' class='btn-download'>" +
                "      <span>⬇️ Install MeetMind Android App</span>" +
                "    </a>" +
                "    <div>" +
                "      <a href='" + webJoinUrl + "' class='btn-web'>Or continue via Web Browser →</a>" +
                "    </div>" +
                "  </div>" +
                "  <script>" +
                "    // If on a mobile device, automatically attempt to launch the app" +
                "    if (/android/i.test(navigator.userAgent)) {" +
                "      setTimeout(function() {" +
                "        window.location = '" + deepLink + "';" +
                "      }, 400);" +
                "    }" +
                "  </script>" +
                "</body>" +
                "</html>";
    }

    @GetMapping(value = "/download", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String renderDownloadPage() {
        String githubReleaseUrl = "https://github.com/rahulambhore394/MeetMind-AI/releases";

        return "<!DOCTYPE html>" +
                "<html lang='en'>" +
                "<head>" +
                "  <meta charset='UTF-8'>" +
                "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "  <title>Download MeetMind AI Android App</title>" +
                "  <style>" +
                "    body { font-family: -apple-system, BlinkMacSystemFont, sans-serif; background: #0b0f19; color: #f1f5f9; display: flex; align-items: center; justify-content: center; min-height: 100vh; padding: 20px; }" +
                "    .card { background: #111827; border: 1px solid rgba(255,255,255,0.1); border-radius: 24px; padding: 36px 28px; max-width: 440px; text-align: center; }" +
                "    h1 { font-size: 24px; font-weight: 800; color: #fff; margin-bottom: 12px; }" +
                "    p { font-size: 14.5px; color: #94a3b8; line-height: 1.6; margin-bottom: 24px; }" +
                "    .btn { display: block; background: #4f46e5; color: #fff; text-decoration: none; font-weight: 700; padding: 14px 24px; border-radius: 14px; font-size: 15px; margin-bottom: 12px; }" +
                "  </style>" +
                "</head>" +
                "<body>" +
                "  <div class='card'>" +
                "    <div style='font-size: 40px; margin-bottom: 12px;'>📲</div>" +
                "    <h1>Install MeetMind AI</h1>" +
                "    <p>Download the MeetMind AI Android App to participate in AI-assisted video conferences with live speech translation, audio proxies, and automated summaries.</p>" +
                "    <a href='" + githubReleaseUrl + "' class='btn'>Download APK (Latest Release)</a>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }
}

