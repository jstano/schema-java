dependencies {
  api(platform(project(":schema-platform-dependencies")))
  api("com.stano:schema-xsd")

  implementation("org.apache.commons:commons-lang3")
  implementation("org.apache.commons:commons-collections4")
  implementation("org.slf4j:slf4j-api")

  testImplementation(project(":test-platform-dependencies"))
}
