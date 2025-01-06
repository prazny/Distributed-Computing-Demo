plugins {
  kotlin("jvm")
}

group = "pl.edu.pw"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

val grpcVersion = "1.57.0"

dependencies {
  testImplementation("org.jetbrains.kotlin:kotlin-test")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

  implementation(project(":protos"))
  implementation("io.grpc:grpc-netty:${grpcVersion}")
  implementation("io.grpc:grpc-protobuf:${grpcVersion}")
  implementation("io.grpc:grpc-stub:${grpcVersion}")
}

tasks.test {
  useJUnitPlatform()
}
kotlin {
  jvmToolchain(19)
}