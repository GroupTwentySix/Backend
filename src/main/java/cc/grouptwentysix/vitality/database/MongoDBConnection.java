package cc.grouptwentysix.vitality.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoDBConnection {
    private static MongoClient mongoClient;
    private static MongoDatabase database;

    public static void connect() {
        String connectionString = "mongodb://localhost:27017";
        mongoClient = MongoClients.create(connectionString);
        database = mongoClient.getDatabase("vitality");
    }

    public static MongoCollection<Document> getUsersCollection() {
        return database.getCollection("users");
    }

    public static MongoCollection<Document> getProductsCollection() {
        return database.getCollection("products");
    }

    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}