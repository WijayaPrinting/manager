plugins {
    kotlin("jvm")
    dokka()
    idea
    id("com.hendraanggrian.r")
    shadow
    application
    packr
}

group = RELEASE_GROUP
version = RELEASE_VERSION

application.mainClassName = "$group.OpenPSSApplication"

sourceSets {
    getByName("main") {
        // manual import client generated build
        val dirs = mutableListOf("src")
        val clientGeneratedDir = "client/build/generated"
        if (rootDir.resolve(clientGeneratedDir).exists()) {
            dirs += "../$clientGeneratedDir/buildconfig/src/main"
            dirs += "../$clientGeneratedDir/r/src/main"
        }
        java.srcDirs(*dirs.toTypedArray())
        resources.srcDir("res")
    }
    getByName("test") {
        java.srcDir("tests/src")
        resources.srcDir("tests/res")
    }
}

ktlint()

dependencies {
    api(project(":client"))
    api(kotlinx("coroutines-javafx", VERSION_COROUTINES))

    implementation(slf4j("log4j12"))

    implementation(hendraanggrian("ktfx", "ktfx", VERSION_KTFX))
    implementation(hendraanggrian("ktfx", "ktfx-controlsfx", VERSION_KTFX))
    implementation(hendraanggrian("ktfx", "ktfx-jfoenix", VERSION_KTFX))

    implementation(apache("commons-lang3", VERSION_COMMONS_LANG))
    implementation(apache("commons-math3", VERSION_COMMONS_MATH))
    implementation(apache("poi-ooxml", VERSION_POI))
    implementation("commons-validator:commons-validator:$VERSION_COMMONS_VALIDATOR")

    implementation(google("guava", "guava", "$VERSION_GUAVA-jre"))

    testImplementation(kotlin("test-junit", VERSION_KOTLIN))
    testImplementation(kotlin("reflect", VERSION_KOTLIN))

    testImplementation(hendraanggrian("ktfx", "ktfx-testfx", VERSION_KTFX))
    testImplementation(testFx("junit"))
}

tasks {
    named<org.jetbrains.dokka.gradle.DokkaTask>("dokka") {
        outputDirectory = "$buildDir/docs"
        doFirst { file(outputDirectory).deleteRecursively() }
    }

    named<com.hendraanggrian.r.RTask>("generateR") {
        resourcesDirectory = "res"
        // exclude("font", "license")
        useCss {
            isJavaFx = true
        }
        useProperties()
    }

    packr {
        mainClass = application.mainClassName
        executable = RELEASE_ARTIFACT
        classpath = files("build/install/desktop/lib")
        resources = files("res")
        vmArgs("Xmx2G")
        macOS {
            name = "$RELEASE_NAME.app"
            icon = rootProject.projectDir.resolve("art/$RELEASE_NAME.icns")
            bundleId = RELEASE_GROUP
        }
        windows64 {
            name = RELEASE_NAME
            jdk = "/Users/hendraanggrian/Desktop/jdk1.8.0_181"
        }
        verbose = true
        openOnDone = true
    }

    named<Jar>("jar") {
        manifest {
            attributes(mapOf("Main-Class" to application.mainClassName))
        }
    }

    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        destinationDir = buildDir.resolve("releases")
        baseName = RELEASE_ARTIFACT
        version = RELEASE_VERSION
        classifier = null
    }

    withType<com.hendraanggrian.packr.PackTask> {
        dependsOn("installDist")
    }
}
