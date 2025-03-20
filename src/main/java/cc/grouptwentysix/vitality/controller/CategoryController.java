package cc.grouptwentysix.vitality.controller;

import cc.grouptwentysix.vitality.model.Category;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import io.javalin.http.Handler;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

import static cc.grouptwentysix.vitality.database.MongoDBConnection.getCategoriesCollection;

public class CategoryController {

    public static Handler getAllCategories = ctx -> {
        List<Category> categories = new ArrayList<>();
        MongoCollection<Document> collection = getCategoriesCollection();

        try {
            collection.find().forEach(doc -> {
                categories.add(mapDocumentToCategory(doc));
            });
            ctx.json(categories);
        } catch (Exception e) {
            ctx.status(500).result("Error fetching categories: " + e.getMessage());
        }
    };

    public static Handler createCategory = ctx -> {
        Category category = ctx.bodyAsClass(Category.class);

        if (category.getName() == null || category.getName().isEmpty()) {
            ctx.status(400).result("Category name is required");
            return;
        }

        Document doc = new Document()
                .append("name", category.getName())
                .append("description", category.getDescription());

        MongoCollection<Document> collection = getCategoriesCollection();

        try {
            collection.insertOne(doc);
            category.setId(doc.getObjectId("_id").toString());
            ctx.status(201).json(category);
        } catch (Exception e) {
            ctx.status(500).result("Error creating category: " + e.getMessage());
        }
    };

    public static Handler deleteCategory = ctx -> {
        String id = ctx.pathParam("id");

        if (!ObjectId.isValid(id)) {
            ctx.status(400).result("Invalid category ID format");
            return;
        }

        MongoCollection<Document> collection = getCategoriesCollection();

        try {
            Document result = collection.findOneAndDelete(
                    Filters.eq("_id", new ObjectId(id))
            );

            if (result != null) {
                ctx.status(204); // No Content, successfully deleted
            } else {
                ctx.status(404).result("Category not found");
            }
        } catch (Exception e) {
            ctx.status(500).result("Error deleting category: " + e.getMessage());
        }
    };

    private static Category mapDocumentToCategory(Document doc) {
        Category category = new Category();
        category.setId(doc.getObjectId("_id").toString());
        category.setName(doc.getString("name"));
        category.setDescription(doc.getString("description"));
        return category;
    }
}