package cc.grouptwentysix.vitality;
import cc.grouptwentysix.vitality.util.RateLimiter;
import cc.grouptwentysix.vitality.auth.AuthController;
import cc.grouptwentysix.vitality.auth.jwt.JWTProvider;
import cc.grouptwentysix.vitality.auth.jwt.JavalinJWT;
import cc.grouptwentysix.vitality.controller.ProductController;
import cc.grouptwentysix.vitality.database.MongoDBConnection;
import cc.grouptwentysix.vitality.model.User;
import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.Javalin;
import io.javalin.http.Handler;
import io.javalin.http.staticfiles.Location;


public class Main {

    public static Dotenv dotenv;


    public static void main(String[] args) {

        dotenv = Dotenv.configure().ignoreIfMissing().load();
        MongoDBConnection.connect();

        // Create JWT provider for user authentication
        JWTProvider<User> jwtProvider = AuthController.createJWTProvider();

        // Create JWT decode handler for protected routes
        Handler decodeHandler = JavalinJWT.createHeaderDecodeHandler(jwtProvider);

        // Create Javalin app and configure static files
        Javalin app = Javalin.create(config -> {
            // Serve static files from 'src/main/resources/public'
            config.staticFiles.add("/public", Location.CLASSPATH);
        }).start(Integer.parseInt(dotenv.get("PORT")));

        //guest rate limiting
        app.before("/api/guest/*", ctx -> {
            String guestId = ctx.queryParam("guestId");
            if (guestId == null || !guestRateLimiter.isAllowed(guestId)) {
                ctx.status(429).result("Too many requests. Please try again later.");
            }
        });

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

        app.post("/wishlist/{username}/{productId}", ProductController.addToWishList, Roles.USER, Roles.ADMIN);
        app.delete("/wishlist/{username}/{productId}", ProductController.removeFromWishList, Roles.USER, Roles.ADMIN);
        app.get("/wishlist/{username}", ProductController.getWishList, Roles.USER, Roles.ADMIN);

        app.delete("/products/{productId}", ProductController.removeProduct, Roles.ADMIN);
        app.get("/products/search", ProductController.searchProducts);


        //set up guest routes
        app.post("/api/guest", AuthController.createGuestSession);
        app.post("/api/guest/:guestId/basket/add", AuthController.addItemToGuestBasket);
        app/get("/api/guest/:guestId/basket", AuthController.viewGuestBasket);
        app.delete("/api/guest/:guestId/basket/remove", AuthController.removeItemFromGuestBasket);
        app.post("/api/guest/:guestId/checkout", AuthController.checkoutAsGuest);


        // Add shutdown hook to close MongoDB connection
        Runtime.getRuntime().addShutdownHook(new Thread(MongoDBConnection::close));


    }


}
