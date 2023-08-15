import app.cash.licensee.ViolationAction.IGNORE

plugins {
  id("java-library")
  alias(libs.plugins.licensee)
}

dependencies {
  implementation("com.example:example:1.0.0")
}

licensee {
  violationAction(IGNORE)
}
