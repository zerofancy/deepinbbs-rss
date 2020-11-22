import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.10"
    application
}
group = "top.ntutn.kfeed.deepinbbs"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}
application {
    mainClassName = "MainKt"
}

dependencies {
    implementation("com.xenomachina:kotlin-argparser:2.0.7")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-core
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-core", version = "2.11.3")
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = "2.11.3")
    // https://mvnrepository.com/artifact/com.rometools/rome-modules
    implementation(group = "com.rometools", name = "rome-modules", version = "1.15.0")
    // https://mvnrepository.com/artifact/org.jsoup/jsoup
    implementation(group = "org.jsoup", name = "jsoup", version = "1.13.1")

}

// https://stackoverflow.com/questions/48553029/how-do-i-overwrite-a-task-in-gradle-kotlin-dsl
// https://github.com/gradle/kotlin-dsl/issues/705
// https://github.com/gradle/kotlin-dsl/issues/716
val fatJar = task("fatJar", type = org.gradle.jvm.tasks.Jar::class) {
    System.out.println("fatJar打包")
    archiveFileName.set("${project.name}-fat.jar")
    manifest {
        attributes["Main-Class"] = "top.ntutn.kfeed.deepinbbs.MainKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks["jar"] as CopySpec).exclude("META-INF/LICENSE.txt").exclude("META-INF/NOTICE.txt")
}

tasks {
    "build" {
        dependsOn("fatJar")
    }
}