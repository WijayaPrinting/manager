buildscript {
    repositories {
        google()
        jcenter()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
    dependencies {
        classpath(kotlin("gradle-plugin", VERSION_KOTLIN))
        classpath(android())
        classpath(dokka())
        classpath(dokka("android"))
        classpath(hendraanggrian("r-gradle-plugin", VERSION_PLUGIN_R))
        classpath(hendraanggrian("buildconfig-gradle-plugin", VERSION_PLUGIN_BUILDCONFIG))
        classpath(hendraanggrian("packr-gradle-plugin", VERSION_PLUGIN_PACKR))
        classpath(shadow())
        classpath(gitPublish())
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        maven("https://kotlin.bintray.com/kotlinx")
        maven("http://repository.jetbrains.com/kotlin-nosql")
        maven("https://dl.bintray.com/hendraanggrian/defaults/")
    }
    tasks {
        withType<Delete> {
            delete(projectDir.resolve("out"))
        }
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
}

tasks {
    register<Delete>("clean") {
        delete(buildDir)
    }
    named<Wrapper>("wrapper") {
        gradleVersion = VERSION_GRADLE
    }
}