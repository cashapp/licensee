import app.cash.licensee.ViolationAction.LOG

plugins {
  id("java-library")
  alias(libs.plugins.licensee)
}

dependencies {
  implementation("com.example:example:1.0.0")
}

licensee {
  violationAction(LOG)
}
