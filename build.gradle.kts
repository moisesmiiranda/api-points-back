plugins {
	java
	id("org.springframework.boot") version "3.5.4"
	id("io.spring.dependency-management") version "1.1.7"
	jacoco
}

group = "com.mmiranda"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.flywaydb:flyway-core")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("com.h2database:h2")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
	finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		xml.required.set(true)
		html.required.set(true)
		csv.required.set(false)
	}
}

tasks.register("verifyCoverageThreshold") {
	dependsOn(tasks.jacocoTestReport)
	doLast {
		val jacocoReportFile = layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml").get().asFile
		
		if (!jacocoReportFile.exists()) {
			throw GradleException("JaCoCo report not found at ${jacocoReportFile.absolutePath}")
		}

		val reportContent = jacocoReportFile.readText()
		val regex = """<sourcefile name="(\w+Service)\.java".*?<counter type="LINE".*?covered="(\d+)".*?missed="(\d+)"""".toRegex(RegexOption.DOT_MATCHES_ALL)
		
		var hasLowCoverage = false
		regex.findAll(reportContent).forEach { match ->
			val className = match.groupValues[1]
			val covered = match.groupValues[2].toInt()
			val missed = match.groupValues[3].toInt()
			val total = covered + missed
			
			if (total > 0) {
				val coverage = (covered * 100) / total
				println("$className: $coverage% ($covered/$total lines)")
				
				if (coverage < 90) {
					hasLowCoverage = true
					println("  ❌ Coverage below 90% threshold!")
				} else {
					println("  ✓ Coverage meets 90% threshold")
				}
			}
		}

		if (hasLowCoverage) {
			throw GradleException("Code coverage is below 90% for one or more service classes!")
		}
	}
}

tasks.named("build") {
	dependsOn("verifyCoverageThreshold")
}
