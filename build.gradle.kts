plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1" // Shadow plugin
}

group = "nub.wi1helm"
version = "0.1"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://jitpack.io") // Include Jitpack repository
    maven {
        name = "buf"
        url = uri("https://buf.build/gen/maven")
    }
}

dependencies {
    // Minestom
    implementation("net.minestom:minestom-snapshots:1_21_5-69b9a5d844")
    // Minimessages
    implementation("net.kyori:adventure-text-minimessage:4.17.0") // MiniMessage


    // Logging
    implementation("org.slf4j:slf4j-api:2.0.15")
    implementation("ch.qos.logback:logback-classic:1.5.7")
    // Redis
    implementation("redis.clients:jedis:5.0.0")

    // Utilities
    implementation("com.google.guava:guava:32.1.2-jre")

    // gRPC and protocol buffers for gate-proxy
    implementation("build.buf.gen:minekube_gate_protocolbuffers_java:29.2.0.1.20241120101512.f1a10b5029ce")
    implementation("build.buf.gen:minekube_gate_grpc_java:1.69.0.1.20241120101512.f1a10b5029ce")
    implementation("io.grpc:grpc-netty:1.69.0") // gRPC transport


    // Testing
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

java {
    withSourcesJar()
    withJavadocJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }

    jar {
        manifest {
            attributes(
                "Main-Class" to "nub.wi1helm.Main" // Your main class
            )
        }
    }

    shadowJar {
        archiveClassifier.set("") // Removes the '-all' suffix
        mergeServiceFiles() // Merges service descriptor files for dependencies
    }

    build {
        dependsOn(shadowJar)
    }
}

tasks.test {
    useJUnitPlatform()
}
