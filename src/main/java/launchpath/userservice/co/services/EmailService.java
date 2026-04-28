package launchpath.userservice.co.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // ── Send OTP Email ────────────────────────────────────

    @Async
    public void sendOtpEmail(String toEmail,
                             String otp,
                             String purpose) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("noreply@launchpath.co");
            helper.setTo(toEmail);
            helper.setSubject(getSubject(purpose));
            helper.setText(buildHtmlEmail(otp, purpose), true);

            mailSender.send(message);
            log.info("OTP email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}",
                    toEmail, e.getMessage());
            // Don't throw — email failure shouldn't break the flow
            // Log and continue
        }
    }

    // ── Send Welcome Email ────────────────────────────────

    @Async
    public void sendWelcomeEmail(String toEmail, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("noreply@launchpath.co");
            helper.setTo(toEmail);
            helper.setSubject("Welcome to LaunchPath — AI Career Guardian! 🚀");
            helper.setText(buildWelcomeEmail(fullName), true);

            mailSender.send(message);
            log.info("Welcome email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send welcome email: {}", e.getMessage());
        }
    }

    // ── Private: Subject ──────────────────────────────────

    private String getSubject(String purpose) {
        return switch (purpose) {
            case "EMAIL_VERIFICATION" ->
                    "Verify your LaunchPath email — OTP inside";
            case "FORGOT_PASSWORD" ->
                    "Reset your LaunchPath password — OTP inside";
            case "LOGIN_OTP" ->
                    "Your LaunchPath login OTP";
            default -> "Your LaunchPath OTP";
        };
    }

    // ── Private: HTML Email Builder ───────────────────────

    private String buildHtmlEmail(String otp, String purpose) {
        String heading = switch (purpose) {
            case "EMAIL_VERIFICATION" -> "Verify Your Email";
            case "FORGOT_PASSWORD"    -> "Reset Your Password";
            case "LOGIN_OTP"          -> "Your Login OTP";
            default -> "Your OTP";
        };

        String subtext = switch (purpose) {
            case "EMAIL_VERIFICATION" ->
                    "Use this OTP to verify your email address.";
            case "FORGOT_PASSWORD" ->
                    "Use this OTP to reset your password.";
            case "LOGIN_OTP" ->
                    "Use this OTP to log in to your account.";
            default -> "Use this OTP to proceed.";
        };

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0;padding:0;font-family:Arial,sans-serif;
                         background-color:#F8FAFC;">
              <div style="max-width:520px;margin:40px auto;
                          background:#FFFFFF;border-radius:12px;
                          box-shadow:0 2px 8px rgba(0,0,0,0.08);
                          overflow:hidden;">
                
                <!-- Header -->
                <div style="background:linear-gradient(135deg,#0F172A,#1E3A5F);
                            padding:32px 40px;text-align:center;">
                  <h1 style="margin:0;color:#F59E0B;font-size:28px;
                              font-weight:800;letter-spacing:-0.5px;">
                    LaunchPath
                  </h1>
                  <p style="margin:4px 0 0;color:#94A3B8;font-size:13px;">
                    AI Career Guardian
                  </p>
                </div>
                
                <!-- Body -->
                <div style="padding:40px;">
                  <h2 style="margin:0 0 8px;color:#0F172A;font-size:22px;">
                    %s
                  </h2>
                  <p style="margin:0 0 28px;color:#475569;font-size:15px;">
                    %s
                  </p>
                  
                  <!-- OTP Box -->
                  <div style="background:#F1F5F9;border:2px dashed #CBD5E1;
                              border-radius:12px;padding:24px;
                              text-align:center;margin-bottom:24px;">
                    <p style="margin:0 0 4px;color:#64748B;font-size:13px;
                               text-transform:uppercase;letter-spacing:1px;">
                      Your OTP
                    </p>
                    <p style="margin:0;color:#2563EB;font-size:42px;
                               font-weight:800;letter-spacing:12px;">
                      %s
                    </p>
                  </div>
                  
                  <!-- Warning -->
                  <div style="background:#FFF7ED;border-left:4px solid #F59E0B;
                              padding:12px 16px;border-radius:4px;
                              margin-bottom:24px;">
                    <p style="margin:0;color:#92400E;font-size:13px;">
                      ⏱ This OTP expires in <strong>10 minutes</strong>.
                      Do not share it with anyone.
                    </p>
                  </div>
                  
                  <p style="margin:0;color:#94A3B8;font-size:13px;">
                    If you didn't request this, please ignore this email.
                    Your account is safe.
                  </p>
                </div>
                
                <!-- Footer -->
                <div style="background:#F8FAFC;padding:20px 40px;
                            border-top:1px solid #E2E8F0;text-align:center;">
                  <p style="margin:0;color:#94A3B8;font-size:12px;">
                    LaunchPath — AI Career Guardian<br>
                    Built with ❤ in Pune, India
                  </p>
                </div>
                
              </div>
            </body>
            </html>
            """.formatted(heading, subtext, otp);
    }

    private String buildWelcomeEmail(String fullName) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Arial,sans-serif;background:#F8FAFC;
                         margin:0;padding:0;">
              <div style="max-width:520px;margin:40px auto;background:#FFFFFF;
                          border-radius:12px;overflow:hidden;
                          box-shadow:0 2px 8px rgba(0,0,0,0.08);">
                
                <div style="background:linear-gradient(135deg,#0F172A,#1E3A5F);
                            padding:32px 40px;text-align:center;">
                  <h1 style="margin:0;color:#F59E0B;font-size:28px;font-weight:800;">
                    LaunchPath
                  </h1>
                  <p style="margin:4px 0 0;color:#94A3B8;font-size:13px;">
                    AI Career Guardian
                  </p>
                </div>
                
                <div style="padding:40px;">
                  <h2 style="color:#0F172A;">Welcome, %s! 🎉</h2>
                  <p style="color:#475569;line-height:1.6;">
                    Your LaunchPath account is ready.
                    Start building your AI-powered resume today.
                  </p>
                  <ul style="color:#475569;line-height:2;">
                    <li>✅ Create unlimited resumes (FREE plan: 2)</li>
                    <li>🎯 Check your ATS score</li>
                    <li>✨ Optimize with AI</li>
                    <li>💬 Chat with your career coach</li>
                  </ul>
                  <a href="https://launchpath.vercel.app/dashboard"
                     style="display:inline-block;margin-top:20px;
                            padding:14px 28px;background:#2563EB;
                            color:#FFFFFF;text-decoration:none;
                            border-radius:8px;font-weight:600;font-size:15px;">
                    Go to Dashboard →
                  </a>
                </div>
                
                <div style="background:#F8FAFC;padding:20px 40px;
                            border-top:1px solid #E2E8F0;text-align:center;">
                  <p style="margin:0;color:#94A3B8;font-size:12px;">
                    LaunchPath — Built with ❤ in Pune, India
                  </p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(fullName);
    }
}
