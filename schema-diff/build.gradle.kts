import com.stano.buildlogic.configurePublishing

plugins { id("com.stano.java-library-convention") }

configurePublishing(
  name = "schema-diff",
  description = "Computes structural differences between two Schema models and produces a ChangeSet.",
  url = "https://github.com/jstano/schema-java"
)

dependencies {
  api(platform(project(":schema-platform-dependencies")))
  api(project(":schema-model"))
  implementation("org.slf4j:slf4j-api")

  testImplementation(project(":test-platform-dependencies"))
  testImplementation(project(":schema-parser"))
}
