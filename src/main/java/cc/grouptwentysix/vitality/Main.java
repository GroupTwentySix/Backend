package cc.grouptwentysix.vitality;

import cc.grouptwentysix.vitality.auth.AuthController;
import cc.grouptwentysix.vitality.auth.jwt.JWTProvider;
import cc.grouptwentysix.vitality.auth.jwt.JavalinJWT;
import cc.grouptwentysix.vitality.controller.ProductController;
import cc.grouptwentysix.vitality.database.MongoDBConnection;
import cc.grouptwentysix.vitality.model.User;
import io.javalin.Javalin;
import io.javalin.http.Handler;

public class Main {
    public static void main(String[] args) {
        // Initialize MongoDB connection
        MongoDBConnection.connect();

        // Create JWT provider for user authentication
        JWTProvider<User> jwtProvider = AuthController.createJWTProvider();

        // Create JWT decode handler for protected routes
        Handler decodeHandler = JavalinJWT.createHeaderDecodeHandler(jwtProvider);

        // Create Javalin app and configure static files
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public");
        }).start(7000);

        // Set up authentication routes
        app.post("/register", AuthController.register);
        app.post("/login", AuthController.login);

        // Apply JWT decode handler to protected routes
        app.before("/user", decodeHandler);
        app.before("/admin", decodeHandler);
        app.before("/logout", decodeHandler);

        // Set up user and admin routes


        app.get("/user", AuthController.userInfo, Roles.USER, Roles.ADMIN);
        app.get("/admin", AuthController.adminInfo, Roles.ADMIN);
        app.post("/logout", AuthController.logout);
        app.get("/verify", AuthController.verifyEmail);

        // Set up product and basket routes
        app.post("/products", ProductController.addProduct, Roles.ADMIN);
        app.get("/products", ProductController.getAllProducts);
        app.post("/basket/{username}/{productId}", ProductController.addToBasket, Roles.USER, Roles.ADMIN);
        app.delete("/basket/{username}/{productId}", ProductController.removeFromBasket, Roles.USER, Roles.ADMIN);
        app.get("/basket/{username}", ProductController.getBasket, Roles.USER, Roles.ADMIN);
        app.delete("/products/{productId}", ProductController.removeProduct, Roles.ADMIN);

        // Add shutdown hook to close MongoDB connection
        Runtime.getRuntime().addShutdownHook(new Thread(MongoDBConnection::close));
    }
}