dependencies {
  api(platform(project(":schema-platform-dependencies")))
  api(project(":schema-installer"))
  api(project(":schema-migrations"))
  api(project(":schema-model"))

  implementation("org.flywaydb:flyway-core")
  implementation("org.flywaydb:flyway-database-postgresql")
  implementation("org.flywaydb:flyway-sqlserver")

  testImplementation(project(":test-platform-dependencies"))
  testRuntimeOnly("com.h2database:h2")
}
