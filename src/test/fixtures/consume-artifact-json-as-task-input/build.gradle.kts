plugins {
  id("java-library")
  alias(libs.plugins.licensee)
}

dependencies {
  implementation("com.example:example:1.0.0")
}

licensee {
  allowDependency("com.example", "example", "1.0.0")
}

tasks.register("myCopy", Copy::class) {
  from(tasks.licensee.flatMap { it.jsonOutput })
  into(layout.buildDirectory.dir("myCopy"))
}
