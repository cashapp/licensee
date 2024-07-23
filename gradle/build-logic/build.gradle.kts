plugins {
  `kotlin-dsl`
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)
}

dependencies {
  implementation(libs.kotlinx.serialization)
  implementation(libs.kotlinpoet)

  testImplementation(kotlin("test-junit"))
  testImplementation(libs.assertk)
}
