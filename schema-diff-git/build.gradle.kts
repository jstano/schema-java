plugins {
  application
}

application {
  mainClass.set("com.stano.schema.git.GitSchemaDiffCli")
}

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
