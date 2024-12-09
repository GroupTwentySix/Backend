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
import java.util.Objects;

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
                    doc.getDouble("price"),
                    1
            );
            productList.add(product);
        }
        ctx.json(productList);
    };

    public static Handler addToBasket = ctx -> {
        String username = ctx.pathParam("username");
        String productId = ctx.pathParam("productId");
        // Default to 1 if quantity is not provided
        int quantity = ctx.queryParam("quantity") != null ?
                Integer.parseInt(ctx.queryParam("quantity")) : 1;

        MongoCollection<Document> users = MongoDBConnection.getUsersCollection();
        Document user = users.find(Filters.eq("username", username)).first();

        if (user != null) {
            List<String> basket = user.getList("basket", String.class);
            if (basket == null) {
                basket = new ArrayList<>();
            }

            boolean itemExists = false;
            for (int i = 0; i < basket.size(); i++) {
                String[] parts = basket.get(i).split(":");
                if (parts[0].equals(productId)) {
                    int currentQuantity = Integer.parseInt(parts[1]);
                    basket.set(i, productId + ":" + (currentQuantity + quantity));
                    itemExists = true;
                    break;
                }
            }

            if (!itemExists) {
                basket.add(productId + ":" + quantity);
            }

            users.updateOne(
                    Filters.eq("username", username),
                    new Document("$set", new Document("basket", basket))
            );
            ctx.status(200).result("Product added to basket with quantity " + quantity);
        } else {
            ctx.status(404).result("User not found");
        }
    };

    public static Handler removeFromBasket = ctx -> {
        String username = ctx.pathParam("username");
        String productId = ctx.pathParam("productId");
        MongoCollection<Document> users = MongoDBConnection.getUsersCollection();

        // First get the current basket
        Document user = users.find(Filters.eq("username", username)).first();
        if (user != null) {
            List<String> basket = user.getList("basket", String.class);
            if (basket != null) {
                // Remove any item that starts with the productId (ignoring quantity  - backwards compatability moment)
                basket.removeIf(item -> item.split(":")[0].equals(productId));

                // Update the basket
                users.updateOne(
                        Filters.eq("username", username),
                        new Document("$set", new Document("basket", basket))
                );
            }
        }
        ctx.status(200).result("Product removed from basket");
    };

    public static Handler getBasket = ctx -> {
        String username = ctx.pathParam("username");
        MongoCollection<Document> users = MongoDBConnection.getUsersCollection();
        Document user = users.find(Filters.eq("username", username)).first();

        if (user != null) {
            List<String> basket = user.getList("basket", String.class);
            if (basket == null) {
                basket = new ArrayList<>();
            }

            MongoCollection<Document> products = MongoDBConnection.getProductsCollection();
            List<Document> basketProducts = new ArrayList<>();

            for (String item : basket) {
                String[] parts = item.split(":");
                String productId = parts[0];
                int quantity = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;

                Document doc = products.find(Filters.eq("_id", new ObjectId(productId))).first();
                if (doc != null) {
                    Document productWithQuantity = new Document()
                            .append("id", doc.getObjectId("_id").toString())
                            .append("name", doc.getString("name"))
                            .append("description", doc.getString("description"))
                            .append("imageUrl", doc.getString("imageUrl"))
                            .append("price", doc.getDouble("price"))
                            .append("quantity", quantity);
                    basketProducts.add(productWithQuantity);
                }
            }

            ctx.json(basketProducts);
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

    public static Handler addToWishList = ctx -> {
        String username = ctx.pathParam("username");
        String productId = ctx.pathParam("productId");
        // Default to 1 if quantity is not provided because it can be null
        int quantity = ctx.queryParam("quantity") != null ?
                Integer.parseInt(ctx.queryParam("quantity")) : 1;

        MongoCollection<Document> users = MongoDBConnection.getUsersCollection();
        Document user = users.find(Filters.eq("username", username)).first();

        if (user != null) {
            List<String> wishlist = user.getList("wishlist", String.class);
            if (wishlist == null) {
                wishlist = new ArrayList<>();
            }

            boolean itemExists = false;
            for (int i = 0; i < wishlist.size(); i++) {
                String[] parts = wishlist.get(i).split(":");
                if (parts[0].equals(productId)) {
                    int currentQuantity = Integer.parseInt(parts[1]);
                    wishlist.set(i, productId + ":" + (currentQuantity + quantity));
                    itemExists = true;
                    break;
                }
            }

            if (!itemExists) {
                wishlist.add(productId + ":" + quantity);
            }

            users.updateOne(
                    Filters.eq("username", username),
                    new Document("$set", new Document("wishlist", wishlist))
            );
            ctx.status(200).result("Product added to wishlist with quantity " + quantity);
        } else {
            ctx.status(404).result("User not found");
        }
    };

    public static Handler removeFromWishList = ctx -> {
        String username = ctx.pathParam("username");
        String productId = ctx.pathParam("productId");
        MongoCollection<Document> users = MongoDBConnection.getUsersCollection();

        // First get the current wishlist
        Document user = users.find(Filters.eq("username", username)).first();
        if (user != null) {
            List<String> wishlist = user.getList("wishlist", String.class);
            if (wishlist != null) {
                // Remove any item that starts with the productId (ignoring the quantity)
                wishlist.removeIf(item -> item.split(":")[0].equals(productId));

                // Update the wishlist
                users.updateOne(
                        Filters.eq("username", username),
                        new Document("$set", new Document("wishlist", wishlist))
                );
            }
        }
        ctx.status(200).result("Product removed from wishlist");
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
                    doc.getDouble("price"),
                    0 // fix nullability.
            );
            productList.add(product);
        }
        ctx.json(productList);
    };

    public static Handler getWishList = ctx -> {
        String username = ctx.pathParam("username");
        MongoCollection<Document> users = MongoDBConnection.getUsersCollection();
        Document user = users.find(Filters.eq("username", username)).first();
        if (user != null) {
            List<String> wishlist = user.getList("wishlist", String.class);
            if (wishlist == null) {
                wishlist = new ArrayList<>();
            }
            MongoCollection<Document> products = MongoDBConnection.getProductsCollection();
            List<Product> wishlistProducts = new ArrayList<>();
            for (String item : wishlist) {
                String[] parts = item.split(":");
                String productId = parts[0];
                int quantity = parts.length > 1 ? Integer.parseInt(parts[1]) : 1; // Default quantity is 1 if not provided :D
                Document doc = products.find(Filters.eq("_id", new ObjectId(productId))).first();
                if (doc != null) {
                    Product product = new Product(
                            doc.getObjectId("_id").toString(),
                            doc.getString("name"),
                            doc.getString("description"),
                            doc.getString("imageUrl"),
                            doc.getDouble("price"),
                            quantity
                    );
                    wishlistProducts.add(product);
                }
            }
            ctx.json(wishlistProducts);
        } else {
            ctx.status(404).result("User not found");
        }
    };
}

