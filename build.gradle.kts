plugins {
    kotlin("jvm") version "2.3.10"
    `maven-publish`
    signing
}

group = "io.github.ezrnest"
version = "0.0.2"

repositories {
    maven {
        url = uri("https://maven.aliyun.com/repository/public/")
    }
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

sourceSets {
    create("samples") {
        kotlin.srcDir("src/samples/kotlin")
        compileClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["main"].output
    }
}

dependencies {
    testImplementation(kotlin("test"))
    "samplesImplementation"(sourceSets["main"].output)
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withJavadocJar()
    withSourcesJar()
}

kotlin {
    jvmToolchain(25)
    compilerOptions{
        freeCompilerArgs.add("-jvm-default=no-compatibility")
    }
}


publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("MathSymK")
                description.set("A modern Kotlin library for symbolic mathematics.")
                url.set("https://github.com/ezrnest/mathsymk")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("ezrnest")
                        name.set("Ezrnest")
                        email.set("1403718476@qq.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/ezrnest/mathsymk.git")
                    developerConnection.set("scm:git:ssh://github.com:ezrnest/mathsymk.git")
                    url.set("https://github.com/Ezrnest/MathSymK")
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
//                username = project.findProperty("ossrhToken") as String? ?: ""
//                password = ""
            }
        }
    }
}

//signing {
//    useInMemoryPgpKeys(
//        project.findProperty("signing.key") as String?,
//        project.findProperty("signing.password") as String?
//    )
//    sign(publishing.publications["mavenJava"])
//}