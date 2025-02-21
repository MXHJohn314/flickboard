package se.nullable.flickboard.build

import com.android.build.api.variant.AndroidComponentsExtension
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory

abstract class DependencyCollectorTask : DefaultTask() {
    @get:Input
    abstract val variant: Property<String>

    @get:Input
    val resolution: Provider<List<DependencyInfo>> =
        project.provider {
            val configuration = project.configurations.getByName("${variant.get()}RuntimeClasspath")
            // createArtifactResolutionQuery is soft-deprecated, but I'm not smart enough to get
            // artifactView to spit out the set of POMs
            val res = project.dependencies.createArtifactResolutionQuery()
                .forComponents(configuration.incoming.resolutionResult.allComponents.map { it.id })
//.map { (it as ResolvedDependencyResult).selected.id })
                .withArtifacts(MavenModule::class.java, MavenPomArtifact::class.java)
                .execute()
            val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val xpath = XPathFactory.newInstance().newXPath()
            res.resolvedComponents.asSequence()
                .flatMap { it.getArtifacts(MavenPomArtifact::class.java) }
                .map { result ->
                    val pomFile = (result as ResolvedArtifactResult).file
                    val pom = documentBuilder.parse(pomFile)
                    val group = xpath.evaluate("/project/groupId", pom)
                        .takeIf(String::isNotEmpty)
                        ?: xpath.evaluate("/project/parent/groupId", pom)
                    DependencyInfo(
                        name = xpath.evaluate("/project/name", pom)
                            // Normalize dependency names
                            .removeSuffix("-Common")
                            .removeSuffix(" Common")
                            .removeSuffix(" - Annotations")
                            .removeSuffix(": Google Core Libraries for Java").let { name ->
                                when {
                                    // A lot of jetpack libraries aren't clearly demarcated as such
                                    // in their POMs
                                    group.startsWith("androidx.") &&
                                            !name.startsWith("Android ") &&
                                            !name.startsWith("Jetpack ") -> "Jetpack $name"

                                    else -> name
                                }
                            },
                        group = group,
                        url = xpath.evaluate("/project/url", pom)
                            .takeIf(String::isNotEmpty),
                        license = xpath.evaluate("/project/licenses/license/name", pom)
                            .takeIf(String::isNotEmpty)
                            ?: when (group) {
                                // Guava is Apache2, but not documented as such in the POM
                                "com.google.guava" -> "The Apache Software License, Version 2.0"
                                else -> null
                            },
                    )
                }
                // Group related dependencies as one entry
                .groupBy { it.group }.map { group -> group.value.minBy { it.name.length } }
                .sortedBy { it.name }.toList()
        }

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @OptIn(ExperimentalSerializationApi::class)
    @TaskAction
    fun taskAction() {
        val outDir = outputDirectory.get().asFile
        // Delete old build results to avoid stale data
        outDir.deleteRecursively()
        val outRawResDir = outDir.resolve("raw")
        outRawResDir.mkdirs()
        outRawResDir.resolve("dependencies.json").outputStream()
            .use { Json.encodeToStream(resolution.get(), it) }
    }
}

/**
 * Must match [se.nullable.flickboard.model.credits.DependencyInfo]
 */
@Serializable
data class DependencyInfo(
    val name: String,
    val group: String,
    val url: String?,
    val license: String?,
) : java.io.Serializable

abstract class DependencyCollectorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        androidComponents.onVariants { variant ->
            val task = project.tasks.register(
                "collect${variant.name.capitalized()}DependencyInfo",
                DependencyCollectorTask::class.java,
            ) {
                it.variant.set(variant.name)
            }
            variant.sources.res?.addGeneratedSourceDirectory(
                task,
                DependencyCollectorTask::outputDirectory,
            )
        }
    }
}
