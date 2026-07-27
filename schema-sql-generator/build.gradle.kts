dependencies {
  api(platform(project(":schema-platform-dependencies")))
  implementation(project(":schema-model"))
  implementation(project(":schema-parser"))

  implementation("commons-cli:commons-cli")
  implementation("commons-io:commons-io")
  implementation("org.apache.commons:commons-lang3")
  implementation("org.apache.commons:commons-text")
  implementation("org.slf4j:slf4j-api")

  testImplementation(project(":test-platform-dependencies"))
}
