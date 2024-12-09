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

        collection.find().forEach(doc -> {
            Category category = new Category();
            category.setId(doc.getObjectId("_id").toString());
            category.setName(doc.getString("name"));
            category.setDescription(doc.getString("description"));
            categories.add(category);
        });

        ctx.json(categories);
    };

    public static Handler createCategory = ctx -> {
        Category category = ctx.bodyAsClass(Category.class);
        Document doc = new Document()
                .append("name", category.getName())
                .append("description", category.getDescription());

        MongoCollection<Document> collection = getCategoriesCollection();
        collection.insertOne(doc);

        category.setId(doc.getObjectId("_id").toString());
        ctx.json(category);
    };

    public static Handler deleteCategory = ctx -> {
        String id = ctx.pathParam("id");
        MongoCollection<Document> collection = getCategoriesCollection();

        Document result = collection.findOneAndDelete(
                Filters.eq("_id", new ObjectId(id))
        );

        if (result != null) {
            ctx.status(204);
        } else {
            ctx.status(404).result("Category not found");
        }
    };
}