rootProject.name = "schema"

pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
  }
}

plugins {
  id("com.stano.settings") version "0.1.12"
}

include("schema-bom")
include("schema-diagram-generator")
include("schema-diff")
include("schema-reverse-engineer")
include("schema-installer")
include("schema-installer-flyway")
include("schema-installer-liquibase")
include("schema-diff-git")
include("schema-migrations")
include("schema-migration-generator")
include("schema-model")
include("schema-parser")
include("schema-platform-dependencies")
include("schema-sql-generator")
include("test-platform-dependencies")
