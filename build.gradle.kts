plugins {
    id("java")
}

group = "nub.wi1helm"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Minestom
    implementation("net.minestom:minestom-snapshots:1_21_5-69b9a5d844")
    // Minimessages
    implementation("net.kyori:adventure-text-minimessage:4.17.0") // MiniMessage


    // Logging
    implementation("org.slf4j:slf4j-api:2.0.15")
    implementation("ch.qos.logback:logback-classic:1.5.7")

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}