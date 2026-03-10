plugins {
    id("java-library")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.3")
    }
}

dependencies {
    api("org.testcontainers:testcontainers-postgresql")
    api("org.testcontainers:testcontainers-junit-jupiter")
    compileOnly("org.springframework:spring-test")
    compileOnly("org.springframework.boot:spring-boot-test")
}
