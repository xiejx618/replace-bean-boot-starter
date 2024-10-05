plugins {
    id("java-library")
    id("maven-publish")
    id("signing")
}

group = "io.github.xiejx618"
version = "0.0.5-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
    (options as? CoreJavadocOptions)?.addStringOption("Xdoclint:none", "-quiet")
}

repositories {
    mavenLocal()
    maven("https://maven.aliyun.com/repository/public/")
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:2.7.18"))
    implementation("org.springframework.boot:spring-boot-starter")
}


publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "replace-bean-boot-starter"
            from(components["java"])
            pom {
                name = project.name
                description = "Implement the function of replacing spring beans."
                url = "https://github.com/xiejx618/replace-bean-boot-starter"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "xiejx618"
                        name = "xiejx618"
                        email = "xiejx618@qq.com"
                    }
                }
                scm {
                    connection = "scm:git:https://github.com/xiejx618/replace-bean-boot-starter.git"
                    developerConnection = "scm:git:git@github.com:xiejx618/replace-bean-boot-starter.git"
                    url = "https://github.com/xiejx618/replace-bean-boot-starter"
                }
            }
        }
    }
    repositories {
        maven {
            name = "sonatype"
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = project.property("OSSRH_USERNAME").toString()
                password = project.property("OSSRH_PASSWORD").toString()
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}
