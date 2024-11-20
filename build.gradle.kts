plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "cc.grouptwentysix"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val shade: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {

    shade("org.mongodb:mongodb-driver-sync:5.2.0")
    // A transitive dependency (jetty is vulnerable) to CVE-2024-6763 and CVE-2024-8184
    // Javalin relies on Jetty, but the impact of this vulnerability is limited to-
    // developers that use the Jetty HttpURI directly, which Javalin does not.
    shade("io.javalin:javalin:6.3.0")
    shade("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    shade("org.slf4j:slf4j-simple:2.0.16")
    shade("com.auth0:java-jwt:4.4.0")
    shade("org.mindrot:jbcrypt:0.4")
    shade("com.resend:resend-java:3.1.0")
    shade("io.github.cdimascio:dotenv-java:3.0.0")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}


tasks.shadowJar {
    configurations = listOf(shade)
    doLast {
        configurations.forEach {
            println("Copying dependencies into project: ${it.files}")
        }
    }
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


