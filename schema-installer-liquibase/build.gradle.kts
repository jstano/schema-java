dependencies {
  api(platform(project(":schema-platform-dependencies")))
  api(project(":schema-installer"))
  api(project(":schema-migrations"))
  api(project(":schema-model"))
  api(project(":schema-parser"))
  api(project(":schema-sql-generator"))

  implementation("org.liquibase:liquibase-core")

  testImplementation(project(":test-platform-dependencies"))
  testRuntimeOnly("com.h2database:h2")
}
