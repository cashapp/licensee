dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    mavenCentral()
    google()
	  maven {
		  setUrl("https://repo.gradle.org/gradle/libs-releases")
		  metadataSources {
			  mavenPom()
			  ignoreGradleMetadataRedirection()
		  }
		  content {
			  includeGroup("org.gradle.experimental")
		  }
	  }
  }
}
