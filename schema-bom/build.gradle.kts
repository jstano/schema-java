plugins {
  id("java-platform")
  id("maven-publish")
  id("signing")
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

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["javaPlatform"])
      pom {
        name.set("Schema BOM")
        description.set("Maven BOM for the java-schema project.")
        url.set("https://github.com/jstano/java-schema")

        licenses {
          license {
            name.set("MIT License")
            url.set("https://opensource.org/license/mit")
          }
        }

        developers {
          developer {
            id.set("jstano")
            name.set("Jeff Stano")
            email.set("jeff@stano.com")
          }
        }

        scm {
          connection.set("scm:git:https://github.com/jstano/java-schema.git")
          developerConnection.set("scm:git:ssh://git@github.com:jstano/java-schema.git")
          url.set("https://github.com/jstano/java-schema")
        }
      }
    }
  }
  repositories {
    maven {
      url = uri(layout.buildDirectory.dir("staging-deploy").get().toString())
    }
  }
}

signing {
  isRequired = gradle.taskGraph.hasTask("publish")
  sign(publishing.publications["mavenJava"])
}

tasks.register<Zip>("zipStagingDeploy") {
  archiveFileName.set("staging-deploy.zip")
  destinationDirectory.set(layout.buildDirectory.dir("tmp"))
  from("build/staging-deploy") {
    include("**/*")
  }
}
