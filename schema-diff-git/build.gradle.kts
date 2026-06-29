import com.stano.buildlogic.configurePublishing

plugins {
  id("com.stano.java-library-convention")
  application
}

application {
  mainClass.set("com.stano.schema.git.GitSchemaDiffCli")
}

configurePublishing(
  name = "schema-diff-git",
  description = "Reads schema XML files from git HEAD and the working tree.",
  url = "https://github.com/jstano/schema-java"
)

dependencies {
  api(platform(project(":schema-platform-dependencies")))
  api(project(":schema-model"))
  implementation(project(":schema-diff"))
  implementation(project(":schema-migration-generator"))
  implementation(project(":schema-parser"))
  implementation("commons-cli:commons-cli")
  implementation("org.eclipse.jgit:org.eclipse.jgit")
  implementation("org.slf4j:slf4j-api")

  testImplementation(project(":test-platform-dependencies"))
}
