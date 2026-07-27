dependencies {
  api(platform(project(":schema-platform-dependencies")))
  api(project(":schema-model"))
  implementation("org.slf4j:slf4j-api")

  testImplementation(project(":test-platform-dependencies"))
  testImplementation(project(":schema-parser"))
}
