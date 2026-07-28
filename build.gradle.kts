import com.stano.gradle.mavencentralpublish.MavenCentralPublishExtension

plugins {
  id("com.stano.base")
  id("com.stano.sonar")
  id("com.stano.maven-central-publish") apply false
  id("com.stano.java-library") apply false
  id("java-library")
  id("maven-publish")
  id("jacoco")
}

val moduleDescriptions = mapOf(
  "schema-model" to "A virtual model for relational database schemas.",
  "schema-parser" to "A parser that can read schema xml files and generate a virtual model for relational database schemas.",
  "schema-sql-generator" to "Generates SQL scripts for relational database schemas from the schema model.",
  "schema-installer" to "Installs a schema into a database",
  "schema-installer-flyway" to "Flyway schema installer",
  "schema-installer-liquibase" to "Liquibase schema installer",
  "schema-migrations" to "Migration helper classes",
  "schema-reverse-engineer" to "Reverse-engineers a live database schema into XML.",
  "schema-diagram-generator" to "Generates ER diagrams (Mermaid, PlantUML) from the schema model.",
  "schema-diff" to "Computes structural differences between two Schema models and produces a ChangeSet.",
  "schema-migration-generator" to "Generates SQL migration scripts from a ChangeSet for multiple database dialects.",
  "schema-diff-git" to "Reads schema XML files from git HEAD and the working tree."
)

configure(javaProjects()) {
  apply(plugin = "com.stano.java-library")
  apply(plugin = "com.stano.maven-central-publish")
  apply(plugin = "groovy")

  configurations {
    all {
      exclude(group = "commons-logging", module = "commons-logging")
    }
  }

  extensions.configure<MavenCentralPublishExtension> {
    componentName = "java"
    pomName = name
    pomDescription = moduleDescriptions[name] ?: name
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
}

fun javaProjects(): Set<Project> = subprojects - nonJavaProjects()
fun nonJavaProjects(): Set<Project> = subprojects.filter { project ->
  project.name == "schema-bom" || project.name == "schema-platform-dependencies" || project.name == "test-platform-dependencies"
}.toSet()
