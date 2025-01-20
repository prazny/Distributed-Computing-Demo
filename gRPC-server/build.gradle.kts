plugins {
  kotlin("jvm")
  application
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

  implementation(kotlin("stdlib"))

  implementation(project(":protos"))
  implementation(project(":matrix-service"))
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
  implementation("io.grpc:grpc-services:${grpcVersion}")
  implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
  implementation("org.slf4j:slf4j-api:1.7.25")

}

tasks.test {
  useJUnitPlatform()
}
kotlin {
  jvmToolchain(19)
}

application {
  mainClass.set("pl.edu.pw.MainKt")
}

tasks.withType<JavaExec> {
  args = if (project.hasProperty("serverPort")) {
    listOf(project.property("serverPort") as String)
  } else {
    emptyList()
  }
}

tasks.jar {
  manifest {
    attributes["Main-Class"] = "pl.edu.pw.MainKt"
  }
  from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
  duplicatesStrategy = DuplicatesStrategy.INCLUDE

}

sourceSets {
  main {
    java {
      srcDirs("src/main/kotlin", "build/generated/source/proto/main/grpc-kotlin")
    }
  }
}