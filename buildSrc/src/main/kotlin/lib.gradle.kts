import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
    id("base-lib")
    id("com.vanniktech.maven.publish")
}

group = "io.github.osobolev.sqlg3"
version = "3.2"

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates("${project.group}", "${project.name}", "${project.version}")
    configure(JavaLibrary(
        javadocJar = JavadocJar.Javadoc(),
        sourcesJar = true
    ))
}

mavenPublishing.pom {
    name.set("${project.group}:${project.name}")
    description.set("SQLG is a preprocessor and a library that uses code generation to simplify writing JDBC code")
    url.set("https://github.com/osobolev/sqlg3")
    licenses {
        license {
            name.set("The Apache License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
        }
    }
    developers {
        developer {
            name.set("Oleg Sobolev")
            organizationUrl.set("https://github.com/osobolev")
        }
    }
    scm {
        connection.set("scm:git:https://github.com/osobolev/sqlg3.git")
        developerConnection.set("scm:git:https://github.com/osobolev/sqlg3.git")
        url.set("https://github.com/osobolev/sqlg3")
    }
}
