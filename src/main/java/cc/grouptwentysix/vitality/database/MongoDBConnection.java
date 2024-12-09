package cc.grouptwentysix.vitality.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import static cc.grouptwentysix.vitality.Main.dotenv;

public class MongoDBConnection {
    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static final int MAX_RETRIES = 10;
    private static final int RETRY_DELAY_MS = 1500;

    public static void connect() {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                String connectionString = "mongodb://" + dotenv.get("DB_HOST") + ":" + dotenv.get("DB_PORT");

                mongoClient = MongoClients.create(connectionString);
                database = mongoClient.getDatabase("vitality");
                System.out.println("Connected to the database successfully");
                return;
            } catch (Exception ignored) {
                System.out.println("Failed to connect to the database, retrying in " + RETRY_DELAY_MS / 1000 + " seconds...");
                attempt++;
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread was interrupted", ie);
                }
            }
        }
        throw new RuntimeException("Exceeded maximum number of retries to connect to the database");
    }

    public static MongoCollection<Document> getUsersCollection() {
        return database.getCollection("users");
    }

    public static MongoCollection<Document> getProductsCollection() {
        return database.getCollection("products");
    }

    public static MongoCollection<Document> getCategoriesCollection() {
        return database.getCollection("categories");
    }

    public static MongoCollection<Document> getContactsCollection() {
        return database.getCollection("contacts");
    }

    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}