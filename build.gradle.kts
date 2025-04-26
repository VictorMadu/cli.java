plugins {
    id("java-library")
    id("maven-publish")
}

group = "io.github.victormadu"
version = "1.0.0-SNAPSHOT"

subprojects {
    plugins.apply("java-library")
    plugins.apply("maven-publish")
    group = rootProject.group

    repositories {
        mavenCentral()
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.2")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(8)
        }
    }

    tasks.named<Test>("test") {
        useJUnitPlatform()
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
            }
        }
        repositories {
            maven {
                url = uri("https://maven.pkg.github.com/VictorMadu/cli.java")
                credentials {
                    username = findProperty("gpr.username") as String? ?: System.getenv("GITHUB_USERNAME")
                    password = findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }

    tasks.register("printProperties") {
        val projectGroup = project.group.toString()
        val projectName = project.name
        val projectVersion = project.version.toString()
        
        doLast {
            println("GPR User: $projectGroup")
            println("Project Name: $projectName")
            println("Project Version: $projectVersion")
        }
    }
}
