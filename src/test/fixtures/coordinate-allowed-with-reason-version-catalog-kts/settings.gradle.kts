pluginManagement {
  includeBuild("../../test-build-logic")
}

plugins {
  id("licenseeTests")
}

dependencyResolutionManagement {
  versionCatalogs.named("libs") {
    library("exam", "com.example", "example").version("1.0.0")
  }
}
