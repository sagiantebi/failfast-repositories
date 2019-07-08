import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.31"
}

group = "com.sagiantebi"
version = "0.1"

repositories {
    mavenCentral()
}

tasks.register("createClasspathManifest") {
    val outputDir = file("$buildDir/$name")

    inputs.files(sourceSets.main.get().runtimeClasspath)
    outputs.dir(outputDir)

    doLast {
        outputDir.mkdirs()
        file("$outputDir/plugin-classpath.txt").writeText(sourceSets.main.get().runtimeClasspath.joinToString("\n"))
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(gradleApi())
    testImplementation("org.junit.jupiter:junit-jupiter:5.5.0")
    testImplementation(gradleTestKit())
    //the following makes the plugin binary available to the test code
    testRuntimeOnly(files(tasks["createClasspathManifest"]))
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}