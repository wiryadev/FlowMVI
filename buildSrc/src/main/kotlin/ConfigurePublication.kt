@file:Suppress("MissingPackageDeclaration", "unused")

import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.LibraryPublishing
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.internal.crash.afterEvaluate
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.android.build.gradle.tasks.BundleAar
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.Sign

/**
 * Configures Maven publishing to sonatype for this project
 */
fun Project.publishMultiplatform() {
    val properties = gradleLocalProperties(rootDir)
    val isReleaseBuild = properties["release"]?.toString().toBoolean()
    val dokkaJavadocJar = tasks.named("dokkaJavadocJar")

    afterEvaluate {
        requireNotNull(extensions.findByType<PublishingExtension>()).apply {
            sonatypeRepository(isReleaseBuild, properties)

            publications.withType<MavenPublication>().configureEach {
                artifact(dokkaJavadocJar)
                configurePom()
                configureVersion(isReleaseBuild)
            }
        }
        signPublications(properties)
    }

    tasks.withType<AbstractPublishToMaven> {
        dependsOn(dokkaJavadocJar)
    }
}

const val variant = "release"

/**
 * Publish the android artifact
 */
fun Project.publishAndroid() {

    requireNotNull(extensions.findByType<LibraryExtension>()).apply {
        publishing {
            singleVariant(variant) {
                withSourcesJar()
                withJavadocJar()
            }
        }
        testFixtures {
            enable = true
        }
    }

    afterEvaluate {
        val properties = gradleLocalProperties(rootDir)
        val isReleaseBuild = properties["release"]?.toString().toBoolean()

        requireNotNull(extensions.findByType<PublishingExtension>()).apply {
            sonatypeRepository(isReleaseBuild, properties)

            publications {
                create(variant, MavenPublication::class.java) {
                    from(components[variant])
                    suppressPomMetadataWarningsFor(variant)
                    groupId = rootProject.group.toString()
                    artifactId = project.name

                    configurePom()
                    configureVersion(isReleaseBuild)
                }
            }
        }
        signPublications(properties)
    }

    tasks.withType<Sign>().configureEach {
        dependsOn(tasks.withType<BundleAar>())
    }
}
