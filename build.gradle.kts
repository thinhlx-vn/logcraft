plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion"))
        pluginVerifier()
        zipSigner()
    }

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    // Gradle runs on JDK 25 in Docker for IntelliJ Platform 2026.2, while
    // plugin bytecode stays compatible with the older JBR used by build 252.
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release.set(17)
    }

    test { useJUnitPlatform() }

    patchPluginXml {
        sinceBuild.set("252")
        untilBuild.set("262.*")
        changeNotes.set("""
            <h3>1.0.2</h3>
            <ul>
              <li>Clarified the local-first log pipeline workflow</li>
              <li>Aligned the source build with the Apache License 2.0</li>
            </ul>
        """.trimIndent())
    }
}
