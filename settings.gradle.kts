pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
  }
}

plugins {
  id("com.stano.settings") version "0.1.8"
}

rootProject.name = "schema"

include("schema-bom")
include("schema-diagram-generator")
include("schema-diff")
include("schema-importer")
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
