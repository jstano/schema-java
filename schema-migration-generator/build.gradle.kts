import com.stano.buildlogic.configurePublishing

plugins { id("com.stano.java-library-convention") }

configurePublishing(
  name = "schema-migration-generator",
  description = "Generates SQL migration scripts from a ChangeSet for multiple database dialects.",
  url = "https://github.com/jstano/schema-java"
)

dependencies {
  api(platform(project(":schema-platform-dependencies")))
  api(project(":schema-diff"))
  implementation(project(":schema-model"))
  implementation("org.slf4j:slf4j-api")

  testImplementation(project(":test-platform-dependencies"))
  testImplementation(project(":schema-parser"))
}
