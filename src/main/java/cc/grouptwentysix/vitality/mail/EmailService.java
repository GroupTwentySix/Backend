package cc.grouptwentysix.vitality.mail;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;

public class EmailService {
    private static final String RESEND_API_KEY = "RESEND_API_KEY";
    private static final String FROM_EMAIL = "noreply@vitality.dev";

    private final Resend resend;

    public EmailService() {
        this.resend = new Resend(RESEND_API_KEY);
    }

    public void sendVerificationEmail(String to, String username, String verificationToken) throws ResendException {
        String subject = "Verify your email for Vitality";
        String verificationLink = "http://localhost:7000/verify?token=" + verificationToken;
        String content = String.format(
            "<p>Hello %s,</p><p>Please click the following link to verify your email: <a href=\"%s\">Verify Email</a></p>",
            username, verificationLink
        );

        CreateEmailOptions params = CreateEmailOptions.builder()
            .from(FROM_EMAIL)
            .to(to)
            .subject(subject)
            .html(content)
            .build();

        try {
            CreateEmailResponse response = resend.emails().send(params);
            System.out.println("Email sent with ID: " + response.getId());
        } catch (ResendException e) {
            System.err.println("Failed to send email: " + e.getMessage());
            throw e;
        }
    }
}