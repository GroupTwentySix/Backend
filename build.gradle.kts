plugins {
    id("java")
}

group = "cc.grouptwentysix"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
implementation("org.mongodb:mongodb-driver-sync:5.2.0")
implementation("io.javalin:javalin:6.3.0")

testImplementation(platform("org.junit:junit-bom:5.9.1"))
testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "cc.grouptwentysix.vitality.Main",
            "Implementation-Title" to "Vitality Backend",
            "Implementation-Version" to version
        )
    }
}

