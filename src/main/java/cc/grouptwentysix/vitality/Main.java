package cc.grouptwentysix.vitality;

import io.javalin.Javalin;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        System.out.println("[DEBUG] App Start, Hello World");
        var app = Javalin.create(/*config*/)
                .get("/", ctx -> ctx.result("Hello World"))
                .start(7071);
        System.out.println("[DEBUG] Route / started at http://localhost:7070/");
    }
}