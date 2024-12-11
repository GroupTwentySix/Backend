package cc.grouptwentysix.vitality.mail;

import cc.grouptwentysix.vitality.model.Contact;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import io.javalin.http.Context;
import org.bson.Document;
import com.mongodb.client.MongoCollection;
import cc.grouptwentysix.vitality.database.MongoDBConnection;

import static cc.grouptwentysix.vitality.Main.dotenv;

public class EmailService {
    private static final String RESEND_API_KEY = dotenv.get("RESEND_API_KEY");
    private static final String FROM_EMAIL = "noreply@vitality-skincare.co.uk";

    private final Resend resend;

    public EmailService() {
        this.resend = new Resend(RESEND_API_KEY);
    }

    public void sendVerificationEmail(Context ctx, String to, String username, String verificationToken) throws ResendException {
        String subject = "Verify your email for Vitality";
        String verificationLink = "https://vitality-skincare.co.uk/email-verified?token=" + verificationToken;
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

    public void sendContactConfirmation(Contact contact) throws ResendException {
        String subject = "We've received your message - Vitality Skincare";
        String content = String.format("""
        <h2>Thank you for contacting us, %s!</h2>
        <p>We've received your message regarding %s.</p>
        <p>Our team will review your inquiry and get back to you within 5-7 business days.</p>
        <p>For reference, here's a copy of your message:</p>
        <hr>
        <p><strong>Message:</strong> %s</p>
        %s
        <hr>
        <p>Best regards,<br>Vitality Skincare Team</p>
        """,
                contact.getFirstName(),
                contact.getQuestionType().toLowerCase(),
                contact.getMessage(),
                contact.getOrderNumber() != null && !contact.getOrderNumber().isEmpty()
                        ? "<p><strong>Order Number:</strong> " + contact.getOrderNumber() + "</p>"
                        : ""
        );

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(FROM_EMAIL)
                .to(contact.getEmail())
                .subject(subject)
                .html(content)
                .build();

        resend.emails().send(params);
    }

    public void sendContactNotification(Contact contact) throws ResendException {
        String subject = "New Contact Form Submission";
        String content = String.format("""
        <h2>New Contact Form Submission</h2>
        <p><strong>From:</strong> %s %s</p>
        <p><strong>Email:</strong> %s</p>
        <p><strong>Phone:</strong> %s</p>
        <p><strong>Order Number:</strong> %s</p>
        <p><strong>Question Type:</strong> %s</p>
        <p><strong>Message:</strong></p>
        <p>%s</p>
        """,
                contact.getFirstName(),
                contact.getLastName(),
                contact.getEmail(),
                contact.getPhone() != null ? contact.getPhone() : "Not provided",
                contact.getOrderNumber() != null ? contact.getOrderNumber() : "Not provided",
                contact.getQuestionType(),
                contact.getMessage()
        );

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(FROM_EMAIL)
                .to("admin@vitality-skincare.co.uk")
                .subject(subject)
                .html(content)
                .build();

        resend.emails().send(params);
    }

    public void sendBulkEmail(String subject, String content) throws ResendException {
        MongoCollection<Document> mailingList = MongoDBConnection.getMailingListCollection();
        
        // Find all subscribers
        mailingList.find().forEach(subscriber -> {
            String email = subscriber.getString("email");
            try {
                CreateEmailOptions options = CreateEmailOptions.builder()
                        .from(FROM_EMAIL)
                        .to(email)
                        .subject(subject)
                        .html(content)
                        .build();
                
                resend.emails().send(options);
            } catch (ResendException e) {
                System.err.println("Failed to send email to: " + email);
                e.printStackTrace();
            }
        });
    }

    public void sendMailingListWelcome(String email) throws ResendException {
        String subject = "Welcome to Vitality Skincare's Mailing List";
        String content = """
                <h2>Welcome to Vitality Skincare!</h2>
                <p>Thank you for subscribing to our mailing list. You'll be the first to know about:</p>
                <ul>
                    <li>New product launches</li>
                    <li>Exclusive offers</li>
                    <li>Skincare tips and advice</li>
                    <li>Special promotions</li>
                </ul>
                <p>We're excited to have you join our community!</p>
                <p>Best regards,<br>The Vitality Skincare Team</p>
                """;

        CreateEmailOptions options = CreateEmailOptions.builder()
                .from(FROM_EMAIL)
                .to(email)
                .subject(subject)
                .html(content)
                .build();
                
        resend.emails().send(options);
    }
}