plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

// lock-service is a library, not a runnable application.
// Disable bootJar (requires a main class) and enable the plain jar task instead.
tasks.named("bootJar") { enabled = false }
tasks.named("jar")     { enabled = true  }

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
    testImplementation(project(":test-fixtures"))
}
