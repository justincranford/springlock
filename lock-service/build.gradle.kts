plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

tasks.named("bootJar") { enabled = false }
tasks.named("jar")     { enabled = true  }

sourceSets {
    create("integrationTest") {
        java.srcDir("src/test/java")
        resources.srcDir("src/test/resources")
        compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
        runtimeClasspath += output + compileClasspath
    }
}

configurations {
    named("integrationTestImplementation") {
        extendsFrom(configurations["testImplementation"])
    }
    named("integrationTestRuntimeOnly") {
        extendsFrom(configurations["testRuntimeOnly"])
    }
}

val integrationTest by tasks.registering(Test::class) {
    description = "Runs integration tests (requires Docker for non-H2 backends)."
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    useJUnitPlatform {
        includeTags("integration")
    }
    shouldRunAfter(tasks.named("test"))
    finalizedBy(tasks.named("jacocoTestReport"))
}

val h2Test by tasks.registering(Test::class) {
    description = "Runs H2-backed integration tests without Docker."
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    useJUnitPlatform {
        includeTags("integration")
    }
    filter {
        includeTestsMatching("*.LockServiceH2*")
    }
    testLogging {
        showStandardStreams = true
    }
    shouldRunAfter(tasks.named("test"))
    finalizedBy(tasks.named("jacocoTestReport"))
}

val perfTest by tasks.registering(Test::class) {
    description = "Runs performance benchmarks on H2 backends."
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    useJUnitPlatform {
        includeTags("integration")
    }
    filter {
        includeTestsMatching("*.PerfBenchmark*")
    }
    testLogging {
        showStandardStreams = true
    }
    shouldRunAfter(tasks.named("test"))
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("integration")
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("test"), h2Test)
    executionData(fileTree(layout.buildDirectory.dir("jacoco")).include("*.exec"))
    violationRules {
        rule {
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.95".toBigDecimal()
            }
        }
    }
}

tasks.named<JacocoReport>("jacocoTestReport") {
    executionData(fileTree(layout.buildDirectory.dir("jacoco")).include("*.exec"))
}

// 'check' runs unit tests only; run 'integrationTest' explicitly when Docker is available.
// Note: info.solidsoft.pitest Gradle plugin does not yet support Gradle 9 (reporting.baseDir removed).
// PIT mutation testing is run via a custom JavaExec task below.

val pitestClasspath by configurations.creating
val pitestReport = layout.buildDirectory.dir("reports/pitest")

dependencies {
    pitestClasspath("org.pitest:pitest-command-line:1.17.4")
    pitestClasspath("org.pitest:pitest-junit5-plugin:1.2.3")
}

tasks.register<JavaExec>("pitest") {
    description = "Runs PIT mutation tests against unit test suite."
    group = "verification"
    dependsOn(tasks.named("testClasses"))
    classpath = pitestClasspath + sourceSets["test"].runtimeClasspath
    mainClass.set("org.pitest.mutationtest.commandline.MutationCoverageReport")
    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
    args(
        "--reportDir", pitestReport.get().asFile.absolutePath,
        "--targetClasses", "com.springlock.lock.*",
        "--targetTests", "com.springlock.lock.*",
        "--excludedClasses", "com.springlock.lock.*Test,com.springlock.lock.*IT",
        "--sourceDirs", "src/main/java",
        "--outputFormats", "HTML,XML",
        "--mutationThreshold", "98",
        "--threads", "2"
    )
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis")
    testImplementation(project(":test-fixtures"))
}
