dependencies {
  api(platform(project(":schema-platform-dependencies")))

  implementation("org.apache.commons:commons-lang3")
  implementation("org.apache.commons:commons-collections4")
  implementation("org.slf4j:slf4j-api")
  implementation("org.postgresql:postgresql")

  testImplementation(project(":test-platform-dependencies"))
}
