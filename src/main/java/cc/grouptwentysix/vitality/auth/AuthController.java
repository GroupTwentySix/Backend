package cc.grouptwentysix.vitality.auth;

import cc.grouptwentysix.vitality.Roles;
import cc.grouptwentysix.vitality.auth.jwt.JavalinJWT;
import cc.grouptwentysix.vitality.auth.jwt.JWTGenerator;
import cc.grouptwentysix.vitality.auth.jwt.JWTProvider;
import cc.grouptwentysix.vitality.database.MongoDBConnection;
import cc.grouptwentysix.vitality.model.User;
import cc.grouptwentysix.vitality.mail.EmailService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import io.javalin.http.Handler;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.http.Context;
import org.bson.Document;
import org.mindrot.jbcrypt.BCrypt;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import static cc.grouptwentysix.vitality.Main.dotenv;

public class AuthController {

    private static final String JWT_SECRET = dotenv.get("JWT_SECRET");
    private static final long EXPIRATION_TIME = 864_000_000; // 10 days
    private static final EmailService emailService = new EmailService();

    public static JWTProvider<User> createJWTProvider() {
        JWTGenerator<User> generator = (user, alg) -> {
            JWTCreator.Builder token = JWT.create()
                    .withClaim("username", user.getUsername())
                    .withClaim("role", user.getRole())
                    .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME));
            return token.sign(alg);
        };

        Algorithm algorithm = Algorithm.HMAC256(JWT_SECRET);
        JWTVerifier verifier = JWT.require(algorithm).build();

        return new JWTProvider<>(algorithm, generator, verifier);
    }

    // Handles user registration, including email verification

    public static Handler register = ctx -> {
        User user = ctx.bodyAsClass(User.class);
        MongoCollection<Document> users = MongoDBConnection.getUsersCollection();

        if (users.find(Filters.eq("username", user.getUsername())).first() != null) {
            ctx.status(400).result("Username already exists");
        } else if (users.find(Filters.eq("email", user.getEmail())).first() != null) {
            ctx.status(400).result("Email already exists");
        } else {
            String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
            String verificationToken = UUID.randomUUID().toString();
            Document newUser = new Document("username", user.getUsername())
                    .append("email", user.getEmail())
                    .append("password", hashedPassword)
                    .append("role", user.getRole())
                    .append("ip", ctx.ip())
                    .append("basket", new ArrayList<>())
                    .append("emailVerified", false)
                    .append("verificationToken", verificationToken);
            users.insertOne(newUser);

            // Handles user login, verifying credentials and issuing JWT

            try {
                emailService.sendVerificationEmail(ctx, user.getEmail(), user.getUsername(), verificationToken);
                ctx.status(201).result("User registered successfully. Please check your email to verify your account.");
            } catch (Exception e) {
                ctx.status(500).result("User registered, but failed to send verification email. Please contact support.");
            }
        }
    };

    public static Handler login = ctx -> {
        User credentials = ctx.bodyAsClass(User.class);
        MongoCollection<Document> users = MongoDBConnection.getUsersCollection();
        Document userDoc = users.find(Filters.eq("username", credentials.getUsername())).first();

        if (userDoc != null && BCrypt.checkpw(credentials.getPassword(), userDoc.getString("password"))) {
            if (!userDoc.getBoolean("emailVerified")) {
                ctx.status(403).result("Please verify your email before logging in.");
                return;
            }

            User user = new User();
            user.setUsername(userDoc.getString("username"));
            user.setRole(userDoc.getString("role"));
            String token = createJWTProvider().generateToken(user);
            ctx.header("Authorization", "Bearer " + token);
            ctx.json(java.util.Map.of("token", token));
        } else {
            throw new UnauthorizedResponse("Invalid credentials");
        }
    };



    public static Handler userInfo = ctx -> {
        String username = JavalinJWT.getDecodedFromContext(ctx).getClaim("username").asString();
        MongoCollection<Document> users = MongoDBConnection.getUsersCollection();
        Document userDoc = users.find(Filters.eq("username", username)).first();

        if (userDoc != null) {
            ctx.json(java.util.Map.of(
                    "username", userDoc.getString("username"),
                    "email", userDoc.getString("email"),
                    "role", userDoc.getString("role"),
                    "ip", userDoc.getString("ip"),
                    "basket", userDoc.getList("basket", String.class)
            ));
        } else {
            ctx.status(404).result("User not found");
        }
    };

    public static Handler adminInfo = ctx -> {
        String username = JavalinJWT.getDecodedFromContext(ctx).getClaim("username").asString();
        ctx.result("Hello, Admin " + username + "! This is an admin-only route.");
    };

    public static Handler logout = ctx -> {
        ctx.header("Authorization", "");
        ctx.result("Logged out successfully");
    };

    public static Handler verifyEmail = ctx -> {
        String token = ctx.queryParam("token");
        if (token == null || token.isEmpty()) {
            ctx.status(400).result("Verification token is required");
            return;
        }

        MongoCollection<Document> users = MongoDBConnection.getUsersCollection();
        Document user = users.find(Filters.eq("verificationToken", token)).first();

        if (user == null) {
            ctx.status(404).result("Invalid or expired verification token");
            return;
        }

        users.updateOne(Filters.eq("verificationToken", token), new Document("$set", new Document("emailVerified", true)));
        ctx.status(200).result("Email verified successfully");
    };

    public static Roles getUserRole(Context ctx) {
        if (!JavalinJWT.containsJWT(ctx)) {
            return Roles.ANYONE;
        }
        DecodedJWT jwt = JavalinJWT.getDecodedFromContext(ctx);
        String role = jwt.getClaim("role").asString();
        try {
            return Roles.valueOf(role);
        } catch (IllegalArgumentException e) {
            return Roles.ANYONE;
        }
    }


    ///************************************************
    /// Jaskaran's code for a guest basket.
    /// Commented out because his contribution is valid but there are some flaws that need to be fixed that prevent compilation.
    ///************************************************


    /*
    public static Handler createGuestSession = ctx -> {
        //creates the unique ID for the guest session
        String guestId = UUID.randomUUID().toString()

        MongoCollection<Document> users = MongoDBConnection.getUsersCollection();
        //creates the guest user in the database with an empty basket
        Document guestUser = new Document("guestId", guestId)
                .append("basket", new ArrayList<>())
                .append("emailVerified", false);
        users.insertOne(guestUser);

        ctx.json(java.util.Map.of("guestId", guestId));
    };

    public static Handler addItemToGuestBasket = ctx -> {
        //the guestId is extracted from the URL
        String guestId = ctx.pathParam("guestId");
        String itemId = ctx.body();
        //updates the guest's basket
        MongoCollection<Document> users = MongoDBConnection.getUsersCollection();
        Document guestUser = users.find(FilteredRowSet.eq("guestId", guestId)),first();

        if (guestUser == null){
            //returns a 404 error to give the user feedback
            ctx.status(404).result("Guest user not found");
            return;
        }

        //adding an item to the guest basket
        guestUser.append("basket", itemId);
        users.updateOne(FilteredRowSet.eq("guestId", guestId), new Document("$set", guestUser));
        ctx.status(200).result("Item added to basket");


    //View basket as guest
    public static viewGuestBasket = ctx -> {
        //guest Id from URL
        String guestId = ctx.pathParam("guestId");
        MongoCollection<Document> users = MongoDBConnection.getUsersCollection();
        Document guestUser = users.find(Filter.eq("guestId", guestId)).first();

        if (guestUser == null){
            ctx.status(404).result("Guest user not found");
            return;
        }

        ctx.json(java.util.Map.of("basket", guestUser.getList("basket", String.class)));
    };

    //Guest checkout
    public static Handler checkoutAsGuest = ctx -> {
        String guestId = ctx.pathParam("guestId")
        MongoCollection<Document> users = MongoDBConnection.getUsersCollection();
        Document guestUser = users.find(Filters.eq("guestId", guestId)).first();
        if (guestUser == null) {
            ctx.status(404).result("Guest user not found");
            return;
        }
        ArrayList<String> basket = (ArrayList<String>) guestUser.get("basket");

        if (basket.isEmpty()){
            ctx.status(404).result("Your basket is empty. Add item(s) before attempting to checkout.");
            return;
        }
        //returns the Id, basket and message to the user
        ctx.json(java.util.Map.of(
                "guestId",guestId,
                "basket",basket,
                "message", "Checking out as guest"
        ));
    };

    public static Handler removeItemFromGuestBasket = ctx -> {
    String guestId = ctx.pathParam("guestId");
    String itemId = ctx.body();

    MongoCollection<Document> users = MongoDBConnection.getUsersCollection();
    Document guestUser = users.find(Filters.eq("guestId", guestId)).first();

    if (guestUser == null) {
        ctx.status(404).result("Guest user not found");
        return;

    ArrayList<String> basket = (ArrayList<String>) guestUser.get("basket");
    if (basket.contains(itemId)){
        basket.remove(itemId);
        users.updateOne(Filters.eq("guestId", guestId), new Document("$set", new Document("basket", basket)));

        ctx.status(200).result("Item deleted from basket");
        else{
            ctx.status(404).result("Item not found in basket");
        }
    }
    };
    */
}


