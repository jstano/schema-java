import com.stano.gradle.mavencentralpublish.MavenCentralPublishExtension

plugins {
  id("java-platform")
  id("maven-publish")
  id("com.stano.maven-central-publish")
}

javaPlatform {
  allowDependencies()
}

dependencies {
  api(platform(project(":schema-platform-dependencies")))
  api("com.stano:schema-xsd")
  api(project(":schema-diagram-generator"))
  api(project(":schema-diff-git"))
  api(project(":schema-diff"))
  api(project(":schema-importer"))
  api(project(":schema-installer"))
  api(project(":schema-installer-flyway"))
  api(project(":schema-installer-liquibase"))
  api(project(":schema-migrations"))
  api(project(":schema-migration-generator"))
  api(project(":schema-model"))
  api(project(":schema-parser"))
  api(project(":schema-sql-generator"))
}

extensions.configure<MavenCentralPublishExtension> {
  componentName = "javaPlatform"
  pomName = "Schema BOM"
  pomDescription = "Maven BOM for the java-schema project."
  pomUrl = "https://github.com/jstano/schema-java"
  licenseName = "MIT License"
  licenseUrl = "https://opensource.org/license/mit"
  developerId = "jstano"
  developerName = "Jeff Stano"
  developerEmail = "jeff@stano.com"
  scmConnection = "scm:git:https://github.com/jstano/schema-java.git"
  scmDeveloperConnection = "scm:git:ssh://git@github.com:jstano/schema-java.git"
  scmUrl = "https://github.com/jstano/schema-java"
}
