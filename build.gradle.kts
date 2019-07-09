import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.31"
    id("com.jfrog.bintray") version "1.7"
    maven
    `maven-publish`
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            from(components["java"])
        }
    }
}

bintray {
    user = if (project.hasProperty("bintrayUser")) { project.property("bintrayUser")  as String } else { System.getenv("BINTRAY_USER") ?: "" }
    key = if (project.hasProperty("bintrayApiKey")) { project.property("bintrayApiKey") as String } else { System.getenv("BINTRAY_API_KEY") ?: "" }
    setConfigurations("archives")
    with(pkg) {
        dryRun = true
        repo = "mvn"
        name = project.name
        userOrg = user
        setLicenses("MIT")
        vcsUrl = "https://github.com/sagiantebi/failfast-repositories"
        with(version) {
            name = project.name
            desc = ""
            vcsTag = project.version.toString() //assume version is git-tagged.
        }
    }
}