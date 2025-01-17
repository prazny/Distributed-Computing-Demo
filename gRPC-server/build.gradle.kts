plugins {
    kotlin("jvm")
}

group = "pl.edu.pw"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
val grpcVersion = "1.57.0"
val grpcKotlinVersion = "1.4.1"
val protobufVersion = "3.24.3"

dependencies {
    testImplementation(kotlin("test"))

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

  implementation(project(":protos"))
    implementation("io.grpc:grpc-services:${grpcVersion}")
    implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(19)
}

sourceSets {
    main {
        java {
            srcDirs("src/main/kotlin", "build/generated/source/proto/main/grpc-kotlin")
        }
    }
}
