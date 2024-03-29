plugins {
  `kotlin-dsl`
  kotlin("plugin.serialization") version embeddedKotlinVersion
}

dependencies {
  implementation(libs.kotlinx.serialization)
  implementation(libs.kotlinpoet)

  testImplementation(kotlin("test-junit"))
  testImplementation(libs.assertk)
}
