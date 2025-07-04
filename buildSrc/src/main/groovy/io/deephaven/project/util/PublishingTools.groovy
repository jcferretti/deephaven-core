package io.deephaven.project.util

import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import groovy.transform.CompileStatic
import io.deephaven.tools.License
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.publish.Publication
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.plugins.signing.SigningExtension

@CompileStatic
class PublishingTools {
    static final String DEVELOPER_ID = 'deephaven'
    static final String DEVELOPER_NAME = 'Deephaven Developers'
    static final String DEVELOPER_EMAIL = 'developers@deephaven.io'

    static final String PROJECT_URL = 'https://github.com/deephaven/deephaven-core'
    static final String ORG_NAME = 'Deephaven Data Labs'
    static final String ORG_URL = 'https://deephaven.io/'

    static final String ISSUES_SYSTEM = 'GitHub Issues'
    static final String ISSUES_URL = 'https://github.com/deephaven/deephaven-core/issues'

    static final String SCM_URL = 'https://github.com/deephaven/deephaven-core'
    static final String SCM_CONNECTION = 'scm:git:git://github.com/deephaven/deephaven-core.git'
    static final String SCM_DEV_CONNECTION = 'scm:git:ssh://github.com/deephaven/deephaven-core.git'

    static final String REPO_NAME = 'ossrh'
    static final String SNAPSHOT_REPO = 'https://s01.oss.sonatype.org/content/repositories/snapshots/'
    static final String RELEASE_REPO = 'https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/'

    static final String SHADOW_PUBLICATION_NAME = 'shadow'

    static void setupPublications(Project project, Closure closure) {
        setupPublications(project, new Action<MavenPublication>() {
            @Override
            void execute(MavenPublication mavenPublication) {
                project.configure(mavenPublication, closure)
            }
        })
    }

    static void setupPublications(Project project, Action<MavenPublication> action) {
        project.extensions
                .getByType(PublishingExtension)
                .publications
                .create('mavenJava', MavenPublication) { publication ->
                    action.execute(publication)
                    setupLicense(project, publication)
                }
    }

    static void setupLicense(Project project, MavenPublication publication) {
        def projectLicense = project.extensions.extraProperties.get('license') as License
        publication.pom {pom ->
            pom.licenses { licenses ->
                licenses.license { license ->
                    license.name.set projectLicense.name
                    license.url.set projectLicense.url
                }
            }
        }
    }

    static boolean isSnapshot(Project project) {
        return ((String)project.version).endsWith('-SNAPSHOT')
    }

    static void setupRepositories(Project project) {
        PublishingExtension publishingExtension = project.extensions.getByType(PublishingExtension)
        publishingExtension.repositories { repoHandler ->
            repoHandler.maven { MavenArtifactRepository repo ->
                repo.name = REPO_NAME
                repo.url = isSnapshot(project) ? SNAPSHOT_REPO : RELEASE_REPO
                // ossrhUsername, ossrhPassword
                repo.credentials(PasswordCredentials)
            }
        }
    }

    static void setupMavenPublication(Project project, MavenPublication mavenPublication) {
        mavenPublication.pom {pom ->
            pom.url.set PROJECT_URL
            pom.organization {org ->
                org.name.set ORG_NAME
                org.url.set ORG_URL
            }
            pom.scm { scm ->
                scm.url.set SCM_URL
                scm.connection.set SCM_CONNECTION
                scm.developerConnection.set SCM_DEV_CONNECTION
            }
            pom.issueManagement { im ->
                im.system.set ISSUES_SYSTEM
                im.url.set ISSUES_URL
            }
            pom.developers { devs ->
                devs.developer { dev ->
                    dev.id.set DEVELOPER_ID
                    dev.name.set DEVELOPER_NAME
                    dev.email.set DEVELOPER_EMAIL
                    dev.organization.set ORG_NAME
                    dev.organizationUrl.set ORG_URL
                }
            }
        }

        def publishToOssrhTask = project.tasks.getByName("publish${mavenPublication.getName().capitalize()}PublicationToOssrhRepository")

        publishToOssrhTask.dependsOn assertIsReleaseTask(project)

        project.afterEvaluate { Project p ->
            // https://central.sonatype.org/publish/requirements/
            if (p.description == null) {
                throw new IllegalStateException("Project '${project.name}' is missing a description, which is required for publishing to maven central")
            }
            BasePluginExtension base = p.extensions.findByType(BasePluginExtension)
            // The common-conventions plugin should take care of this, but we'll double-check here
            String archivesName = base.archivesName.get()
            if (!archivesName.contains('deephaven')) {
                throw new IllegalStateException("Project '${project.name}' archiveBaseName '${archivesName}' does not contain 'deephaven'")
            }
            mavenPublication.artifactId = archivesName
            mavenPublication.pom { pom ->
                pom.name.set archivesName
                pom.description.set p.description
            }
        }
    }

    static void setupSigning(Project project, Publication publication) {
        SigningExtension signingExtension = project.extensions.getByType(SigningExtension)
        signingExtension.required = "true" == project.findProperty('signingRequired')
        signingExtension.sign(publication)
        String signingKey = project.findProperty('signingKey')
        String signingPassword = project.findProperty('signingPassword')
        if (signingKey != null && signingPassword != null) {
            // In CI, it's harder to pass a file; so if specified, we use the in-memory version.
            signingExtension.useInMemoryPgpKeys(signingKey, signingPassword)
        }
    }

    static TaskProvider<Task> assertIsReleaseTask(Project p) {
        // todo: can we register this task once globally instead?
        return p.tasks.register('assertIsRelease') {task ->
            task.doLast {
                if (System.getenv('CI') != 'true') {
                    throw new IllegalStateException('Release error: env CI must be true')
                }
                def actualGithubRef = System.getenv('GITHUB_REF')
                def expectedGithubRef = isSnapshot(p)
                        ? 'refs/heads/main'
                        : "refs/heads/release/v${p.version}".toString()
                if (actualGithubRef != expectedGithubRef) {
                    throw new IllegalStateException("Release error: env GITHUB_REF '${actualGithubRef}' does not match expected '${expectedGithubRef}'. Bad tag? Bump version?")
                }
            }
        }
    }

    static void setupShadowName(Project project, String name) {
        project.tasks.named(ShadowJavaPlugin.SHADOW_JAR_TASK_NAME, ShadowJar) {
            it.archiveBaseName.set(name)
        }
        project.extensions.getByType(PublishingExtension).publications.named(SHADOW_PUBLICATION_NAME, MavenPublication) {
            it.artifactId = name
        }
        project.extensions.getByType(BasePluginExtension).archivesName.set(name)
    }
}
