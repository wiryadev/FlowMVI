@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("pro.respawn.shared-library")
    id(libs.plugins.atomicfu.id)
}

kotlin {
    configureMultiplatform(
        this,
        android = false,
        ios = true,
        jvm = true,
    )
}

dependencies {
    commonMainApi(libs.kotlinx.coroutines.core)

    commonTestImplementation(libs.bundles.unittest)
}
