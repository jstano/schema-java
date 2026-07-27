dependencies {
  api(platform(project(":schema-platform-dependencies")))

  implementation(project(":schema-model"))
  implementation(project(":schema-parser"))

  implementation("org.slf4j:slf4j-api")

  testImplementation(project(":test-platform-dependencies"))
}
