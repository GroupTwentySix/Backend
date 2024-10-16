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

public class AuthController {

    // todo: Move JWT_SECRET to a secure configuration or environment variable

    private static final String JWT_SECRET = "your_jwt_secret";
    private static final long EXPIRATION_TIME = 864_000_000; // 10 days
    private static final EmailService emailService = new EmailService();

    // Creates and configures the JWT provider for user authentication


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
                emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), verificationToken);
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
            ctx.status(400).result("Invalid verification token");
            return;
        }

        MongoCollection<Document> users = MongoDBConnection.getUsersCollection();
        Document userDoc = users.findOneAndUpdate(
            Filters.eq("verificationToken", token),
            new Document("$set", new Document("emailVerified", true).append("verificationToken", null))
        );

        if (userDoc != null) {
            ctx.result("Email verified successfully. You can now log in.");
        } else {
            ctx.status(400).result("Invalid or expired verification token");
        }
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
}
