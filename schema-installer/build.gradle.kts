dependencies {
  api(platform(project(":schema-platform-dependencies")))

  api(project(":schema-migrations"))
  api(project(":schema-model"))
  api(project(":schema-parser"))
  api(project(":schema-sql-generator"))

  testImplementation(project(":test-platform-dependencies"))
  testRuntimeOnly("com.h2database:h2")
}
