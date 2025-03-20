package com.example.stockmanagement;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import java.util.ArrayList;
import java.util.List;
import static spark.Spark.*;

class Product {
    private String id;
    private String name;
    private int quantity;

    public Product(String id, String name, int quantity) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
}
class Database {
    private static final MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
    private static final MongoDatabase database = mongoClient.getDatabase("stockdb");
    private static final MongoCollection<Document> collection = database.getCollection("products");

    public static List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        for (Document doc : collection.find()) {
            products.add(new Product(doc.getObjectId("_id").toString(), doc.getString("name"), doc.getInteger("quantity")));
        }
        return products;
    }

    public static void addProduct(String name, int quantity) {
        Document doc = new Document("name", name).append("quantity", quantity);
        collection.insertOne(doc);
    }

    public static void updateProduct(String id, String name, int quantity) {
        collection.updateOne(Filters.eq("_id", id), new Document("$set", new Document("name", name).append("quantity", quantity)));
    }

    public static void deleteProduct(String id) {
        collection.deleteOne(Filters.eq("_id", id));
    }
}
