package cc.grouptwentysix.vitality.controller;

import cc.grouptwentysix.vitality.database.MongoDBConnection;
import cc.grouptwentysix.vitality.mail.EmailService;
import cc.grouptwentysix.vitality.model.MailingListEntry;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import io.javalin.http.Handler;
import org.bson.Document;

public class MailingListController {

    public static Handler subscribe = ctx -> {
        MailingListEntry entry = ctx.bodyAsClass(MailingListEntry.class);
        
        if (entry.getEmail() == null || entry.getEmail().isEmpty()) {
            ctx.status(400).result("Email is required");
            return;
        }

        MongoCollection<Document> mailingList = MongoDBConnection.getMailingListCollection();
        
        // Check if email already exists
        if (mailingList.find(Filters.eq("email", entry.getEmail())).first() != null) {
            ctx.status(400).result("Email already subscribed");
            return;
        }

        // Add new subscription
        Document newSubscription = new Document()
                .append("email", entry.getEmail())
                .append("dateAdded", new java.util.Date());
                
        mailingList.insertOne(newSubscription);
        
        // Send welcome email
        try {
            EmailService emailService = new EmailService();
            emailService.sendMailingListWelcome(entry.getEmail());
        } catch (Exception e) {
            System.err.println("Failed to send welcome email: " + e.getMessage());
            // Continue execution - we don't want to fail the subscription if only the email fails
        }
        
        ctx.status(201).result("Successfully subscribed to mailing list");
    };

    public static Handler sendBulkEmail = ctx -> {
        // Extract email details from request body
        Document emailDetails = Document.parse(ctx.body());
        String subject = emailDetails.getString("subject");
        String content = emailDetails.getString("content");
        
        if (subject == null || subject.isEmpty() || content == null || content.isEmpty()) {
            ctx.status(400).result("Subject and content are required");
            return;
        }

        EmailService emailService = new EmailService();
        try {
            emailService.sendBulkEmail(subject, content);
            ctx.status(200).result("Bulk email sent successfully");
        } catch (Exception e) {
            ctx.status(500).result("Failed to send bulk email: " + e.getMessage());
        }
    };
} 