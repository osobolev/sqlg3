import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

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
        sourcesJar = SourcesJar.Sources()
    ))
}

mavenPublishing.pom {
    name = "${project.group}:${project.name}"
    description = "SQLG is a preprocessor and a library that uses code generation to simplify writing JDBC code"
    url = "https://github.com/osobolev/sqlg3"
    licenses {
        license {
            name = "The Apache License, Version 2.0"
            url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
        }
    }
    developers {
        developer {
            name = "Oleg Sobolev"
            organizationUrl = "https://github.com/osobolev"
        }
    }
    scm {
        connection = "scm:git:https://github.com/osobolev/sqlg3.git"
        developerConnection = "scm:git:https://github.com/osobolev/sqlg3.git"
        url = "https://github.com/osobolev/sqlg3"
    }
}
