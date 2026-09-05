pluginManagement {
	repositories {
		maven {
			name = "Fabric"
			url = uri("https://maven.fabricmc.net/")
		}
		mavenCentral()
		gradlePluginPortal()
	}

	plugins {
		id("net.fabricmc.fabric-loom-remap") version providers.gradleProperty("loom_version")
	}
}

rootProject.name = "Sword-Art-Online-UI-Reborn"

val saomclibDirectory = providers.gradleProperty("saomclib_path")
	.orElse("../saomclib-reborn-1.21.1")
	.get()

run {
	val saomclib = file(saomclibDirectory)
	require(saomclib.resolve("settings.gradle.kts").isFile) {
		"SAOMCLib Fabric 1.21.1 checkout was not found at '${saomclib.absolutePath}'. " +
			"Set -Psaomclib_path=<path> to the saomclib-reborn checkout."
	}
	includeBuild(saomclib) {
		dependencySubstitution {
			substitute(module("com.tencao.saomclib:saomclib")).using(project(":"))
		}
	}
}
