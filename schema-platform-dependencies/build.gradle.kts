plugins {
  `java-platform`
  `maven-publish`
  `signing`
}

javaPlatform {
  allowDependencies()
}

dependencies {
  constraints {
    api("com.stano:schema-xsd:1.0.0-SNAPSHOT")
    api("ch.qos.logback:logback-classic:1.5.34")
    api("ch.qos.logback:logback-core:1.5.34")
    api("com.h2database:h2:2.1.214")
    api("commons-cli:commons-cli:1.11.0")
    api("commons-io:commons-io:2.22.0")
    api("net.bytebuddy:byte-buddy:1.18.10")
    api("net.logstash.logback:logstash-logback-encoder:9.0")
    api("org.apache.commons:commons-collections4:4.5.0")
    api("org.apache.commons:commons-lang3:3.20.0")
    api("org.eclipse.jgit:org.eclipse.jgit:7.3.0.202506031305-r")
    api("org.apache.commons:commons-text:1.15.0")
    api("org.apache.groovy:groovy-all:4.0.32")
    api("org.flywaydb:flyway-core:12.8.1")
    api("org.flywaydb:flyway-database-postgresql:12.8.1")
    api("org.flywaydb:flyway-sqlserver:12.8.1")
    api("org.junit.jupiter:junit-jupiter:6.1.0")
    api("org.junit.platform:junit-platform-launcher:6.1.0")
    api("org.liquibase:liquibase-core:5.0.3")
    api("org.mockito:mockito-junit-jupiter:5.23.0")
    api("org.postgresql:postgresql:42.7.11")
    api("org.slf4j:jcl-over-slf4j:2.0.18")
    api("org.slf4j:jul-to-slf4j:2.0.18")
    api("org.slf4j:log4j-over-slf4j:2.0.18")
    api("org.slf4j:slf4j-api:2.0.18")
    api("org.spockframework:spock-core:2.4-groovy-4.0")
    api("uk.org.lidalia:sysout-over-slf4j:1.0.2")
  }
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["javaPlatform"])
      pom {
        name.set("Schema Platform Dependencies")
        description.set("Platform BOM for third-party dependency versions used by java-schema.")
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
