import com.stano.buildlogic.configurePublishing

plugins {
  id("com.stano.java-library-convention")
}

configurePublishing(
  name = "schema-model",
  description = "A virtual model for relational database schemas.",
  url = "https://github.com/jstano/java-schema"
)

dependencies {
  api(platform(project(":schema-platform-dependencies")))
  api("com.stano:schema-xsd")

  implementation("org.apache.commons:commons-lang3")
  implementation("org.apache.commons:commons-collections4")
  implementation("org.slf4j:slf4j-api")

  testImplementation(project(":test-platform-dependencies"))
}
