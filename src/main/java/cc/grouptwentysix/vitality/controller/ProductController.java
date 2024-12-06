package cc.grouptwentysix.vitality.controller;

import cc.grouptwentysix.vitality.database.MongoDBConnection;
import cc.grouptwentysix.vitality.model.Product;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import io.javalin.http.Handler;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class ProductController {

    public static Handler addProduct = ctx -> {
        Product product = ctx.bodyAsClass(Product.class);
        MongoCollection<Document> products = MongoDBConnection.getProductsCollection();
        Document newProduct = new Document("name", product.getName())
                .append("description", product.getDescription())
                .append("imageUrl", product.getImageUrl())
                .append("price", product.getPrice());
        products.insertOne(newProduct);
        ctx.status(201).json(newProduct);
    };

    public static Handler getAllProducts = ctx -> {
        MongoCollection<Document> products = MongoDBConnection.getProductsCollection();
        List<Product> productList = new ArrayList<>();
        for (Document doc : products.find()) {
            Product product = new Product(
                doc.getObjectId("_id").toString(),
                doc.getString("name"),
                doc.getString("description"),
                doc.getString("imageUrl"),
                doc.getDouble("price")
            );
            productList.add(product);
        }
        ctx.json(productList);
    };

    public static Handler addToBasket = ctx -> {
        String username = ctx.pathParam("username");
        String productId = ctx.pathParam("productId");
        MongoCollection<Document> users = MongoDBConnection.getUsersCollection();
        users.updateOne(
            Filters.eq("username", username),
            new Document("$addToSet", new Document("basket", productId))
        );
        ctx.status(200).result("Product added to basket");
    };

    public static Handler removeFromBasket = ctx -> {
        String username = ctx.pathParam("username");
        String productId = ctx.pathParam("productId");
        MongoCollection<Document> users = MongoDBConnection.getUsersCollection();
        users.updateOne(
            Filters.eq("username", username),
            new Document("$pull", new Document("basket", productId))
        );
        ctx.status(200).result("Product removed from basket");
    };

    public static Handler getBasket = ctx -> {
        String username = ctx.pathParam("username");
        MongoCollection<Document> users = MongoDBConnection.getUsersCollection();
        Document user = users.find(Filters.eq("username", username)).first();
        if (user != null) {
            List<String> basketIds = user.getList("basket", String.class);
            MongoCollection<Document> products = MongoDBConnection.getProductsCollection();
            List<Product> basket = new ArrayList<>();
            for (String id : basketIds) {
                Document doc = products.find(Filters.eq("_id", new ObjectId(id))).first();
                if (doc != null) {
                    Product product = new Product(
                        doc.getObjectId("_id").toString(),
                        doc.getString("name"),
                        doc.getString("description"),
                        doc.getString("imageUrl"),
                        doc.getDouble("price")
                    );
                    basket.add(product);
                }
            }
            ctx.json(basket);
        } else {
            ctx.status(404).result("User not found");
        }
    };

    public static Handler removeProduct = ctx -> {
        String productId = ctx.pathParam("productId");
        MongoCollection<Document> products = MongoDBConnection.getProductsCollection();
        long deletedCount = products.deleteOne(Filters.eq("_id", new ObjectId(productId))).getDeletedCount();
        if (deletedCount > 0) {
            ctx.status(200).result("Product removed successfully");
        } else {
            ctx.status(404).result("Product not found");
        }
    };


    public static Handler searchProducts = ctx -> {
        String query = ctx.queryParam("q");
        if (query == null || query.isEmpty()) {
            ctx.status(400).result("Query parameter 'q' is required");
            return;
        }

        MongoCollection<Document> products = MongoDBConnection.getProductsCollection();
        List<Product> productList = new ArrayList<>();
        for (Document doc : products.find(Filters.regex("name", query, "i"))) {
            Product product = new Product(
                    doc.getObjectId("_id").toString(),
                    doc.getString("name"),
                    doc.getString("description"),
                    doc.getString("imageUrl"),
                    doc.getDouble("price")
            );
            productList.add(product);
        }
        ctx.json(productList);
    };

    //WishList
    public static Handler addToWishList = ctx -> {
        String username = ctx.pathParam("username");
        String productId = ctx.pathParam("productId");
        MongoCollection<Document> users = MongoDBConnection.getUsersCollection();
        users.updateOne(
                Filters.eq("username", username),
                new Document("$addToSet", new Document("wishlist", productId))
        );
        ctx.status(200).result("Product added to basket");
    };

    public static Handler removeFromWishList = ctx -> {
        String username = ctx.pathParam("username");
        String productId = ctx.pathParam("productId");
        MongoCollection<Document> users = MongoDBConnection.getUsersCollection();
        users.updateOne(
                Filters.eq("username", username),
                new Document("$pull", new Document("wishlist", productId))
        );
        ctx.status(200).result("Product removed from wishlist");
    };

    public static Handler getWishList = ctx -> {
        String username = ctx.pathParam("username");
        MongoCollection<Document> users = MongoDBConnection.getUsersCollection();
        Document user = users.find(Filters.eq("username", username)).first();
        if (user != null) {
            List<String> wishlistIds = user.getList("wishlist", String.class);
            MongoCollection<Document> products = MongoDBConnection.getProductsCollection();
            List<Product> wishlist = new ArrayList<>();
            for (String id : wishlistIds) {
                Document doc = products.find(Filters.eq("_id", new ObjectId(id))).first();
                if (doc != null) {
                    Product product = new Product(
                            doc.getObjectId("_id").toString(),
                            doc.getString("name"),
                            doc.getString("description"),
                            doc.getString("imageUrl"),
                            doc.getDouble("price")
                    );
                    wishlist.add(product);
                }
            }
            ctx.json(wishlist);
        } else {
            ctx.status(404).result("User not found");
        }
    };
}

