package cc.grouptwentysix.vitality.controller;

import cc.grouptwentysix.vitality.database.MongoDBConnection;
import cc.grouptwentysix.vitality.mail.EmailService;
import cc.grouptwentysix.vitality.model.Contact;
import com.mongodb.client.MongoCollection;
import io.javalin.http.Handler;
import org.bson.Document;

public class ContactController {
    private static final EmailService emailService = new EmailService();

    public static Handler submitContact = ctx -> {
        Contact contact = ctx.bodyAsClass(Contact.class);

        // Store in database
        MongoCollection<Document> contacts = MongoDBConnection.getContactsCollection();
        Document contactDoc = new Document()
                .append("firstName", contact.getFirstName())
                .append("lastName", contact.getLastName())
                .append("email", contact.getEmail())
                .append("phone", contact.getPhone())
                .append("orderNumber", contact.getOrderNumber())
                .append("questionType", contact.getQuestionType())
                .append("message", contact.getMessage())
                .append("timestamp", new java.util.Date());

        contacts.insertOne(contactDoc);

        // Send confirmation email to user
        emailService.sendContactConfirmation(contact);

        // Send notification email to admin
        emailService.sendContactNotification(contact);

        ctx.status(201).json(new Document("message", "Contact form submitted successfully"));
    };
}