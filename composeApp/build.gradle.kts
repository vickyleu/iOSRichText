import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.cocoapods)
}

kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    cocoapods {
        summary = "ComposeApp"
        homepage = "."
        version = "1.0.0"
        license = "MIT"
        ios.deploymentTarget = "13.0"
        source = "https://cdn.cocoapods.org"

        podfile = project.file("../iosApp/Podfile")
        framework {
            baseName = "ComposeApp"
            isStatic = true
            // 不要使用transitiveExport = true。使用 transitive export 在许多情况下会禁用死代码消除：
            // 编译器必须处理大量未使用的代码。它会增加编译时间。export明确用于导出所需的项目和依赖项。
        }
        pod("DTCoreText"){
            this.source = git("https://github.com/vickyleu/DTCoreText.git")
            packageName="uooc.DTCoreText"
        }
        pod("DTFoundation")
        pod("iosMath"){
            version="~> 0.9"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }

    }
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.ksoup)


            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }
    }
}
