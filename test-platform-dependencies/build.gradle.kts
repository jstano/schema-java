plugins {
  id("com.stano.java-library")
}

dependencies {
  api(platform(project(":schema-platform-dependencies")))

  api("org.junit.jupiter:junit-jupiter")
  api("org.junit.platform:junit-platform-launcher")
  api("org.mockito:mockito-junit-jupiter")
  api("ch.qos.logback:logback-classic")
  api("ch.qos.logback:logback-core")
  api("org.slf4j:jcl-over-slf4j")
  api("org.slf4j:jul-to-slf4j")
  api("org.slf4j:log4j-over-slf4j")
  api("uk.org.lidalia:sysout-over-slf4j")
  api("net.logstash.logback:logstash-logback-encoder")
}
