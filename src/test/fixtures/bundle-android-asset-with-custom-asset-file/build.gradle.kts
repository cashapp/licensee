plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.licensee)
}

licensee {
  bundleAndroidAsset = true
  androidAssetReportPath = "example/licenses.json"
  allow("Apache-2.0")
}

android {
  namespace = "app.cash.licensee.app"
  compileSdk = 35

  defaultConfig {
    applicationId = "app.cash.licensee.app"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"
  }
}

dependencies {
  implementation("com.example:example:1.0.0")
}
