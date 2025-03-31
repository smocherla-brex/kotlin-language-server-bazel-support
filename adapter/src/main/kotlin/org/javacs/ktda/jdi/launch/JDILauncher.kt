package org.javacs.ktda.jdi.launch

import com.sun.jdi.Bootstrap
import com.sun.jdi.VirtualMachineManager
import com.sun.jdi.connect.AttachingConnector
import com.sun.jdi.connect.Connector
import com.sun.jdi.connect.LaunchingConnector
import org.javacs.kt.LOG
import org.javacs.kt.proto.LspInfo
import org.javacs.ktda.core.DebugContext
import org.javacs.ktda.core.launch.AttachConfiguration
import org.javacs.ktda.core.launch.DebugLauncher
import org.javacs.ktda.core.launch.LaunchConfiguration
import org.javacs.ktda.jdi.JDIDebuggee
import org.javacs.ktda.util.KotlinDAException
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

class JDILauncher(
	private val attachTimeout: Int = 50,
	private val vmArguments: String? = null,
	private val modulePaths: String? = null,
) : DebugLauncher {
	private val vmManager: VirtualMachineManager
		get() = Bootstrap.virtualMachineManager()

	override fun launch(config: LaunchConfiguration, context: DebugContext): JDIDebuggee {
		val connector = createLaunchConnector()
		LOG.info("Starting JVM debug session with main class {}", config.mainClass)

		LOG.debug("Launching VM")
		val vm = connector.launch(createLaunchArgs(config, connector)) ?: throw KotlinDAException("Could not launch a new VM")

		LOG.debug("Finding sourcesRoots")

		return JDIDebuggee(vm, sourceFiles(config.workspaceRoot), context, config.sourcesJVMClassNames, config.workspaceRoot)
	}

	override fun attach(config: AttachConfiguration, context: DebugContext): JDIDebuggee {
		val connector = createAttachConnector()
		LOG.info("Attaching JVM debug session on {}:{}", config.hostName, config.port)
		return JDIDebuggee(
			connector.attach(createAttachArgs(config, connector)) ?: throw KotlinDAException("Could not attach the VM"),
			sourceFiles(config.workspaceRoot),
			context,
            config.sourcesJVMClassNames,
            config.workspaceRoot
		)
	}

	private fun createLaunchArgs(config: LaunchConfiguration, connector: Connector): Map<String, Connector.Argument> = connector.defaultArguments()
		.also { args ->
			args["suspend"]!!.setValue("true")
			args["options"]!!.setValue(formatOptions(config))
			args["main"]!!.setValue(formatMainClass(config))
		}

	private fun createAttachArgs(config: AttachConfiguration, connector: Connector): Map<String, Connector.Argument> = connector.defaultArguments()
		.also { args ->
			args["hostname"]!!.setValue(config.hostName)
			args["port"]!!.setValue(config.port.toString())
			args["timeout"]!!.setValue(config.timeout.toString())
		}

	private fun createAttachConnector(): AttachingConnector = vmManager.attachingConnectors()
		.let { it.find { it.name() == "com.sun.jdi.SocketAttach" } ?: it.firstOrNull() }
		?: throw KotlinDAException("Could not find an attaching connector (for a new debuggee VM)")

	private fun createLaunchConnector(): LaunchingConnector = vmManager.launchingConnectors()
		// Workaround for JDK 11+ where the first launcher (RawCommandLineLauncher) does not properly support args
		.let { it.find { it.javaClass.name == "com.sun.tools.jdi.SunCommandLineLauncher" } ?: it.firstOrNull() }
		?: throw KotlinDAException("Could not find a launching connector (for a new debuggee VM)")

	private fun formatOptions(config: LaunchConfiguration): String {
		var options = config.vmArguments
		modulePaths?.let { options += " --module-path \"$modulePaths\"" }
		options += " -classpath \"${formatClasspath(config)}:\""
		return options
	}

	private fun formatMainClass(config: LaunchConfiguration): String {
		val mainClasses = config.mainClass.split("/")
		return if ((modulePaths != null) || (mainClasses.size == 2)) {
			// Required for Java 9 compatibility
			"-m ${config.mainClass}"
		} else config.mainClass
	}

    private fun sourceFiles(workspaceRoot: Path): Set<Path> {
        val bazelOut = workspaceRoot.resolve("bazel-out")
        if(!bazelOut.exists()) return emptySet()

        return Files.walk(bazelOut, FileVisitOption.FOLLOW_LINKS).use { paths ->
            paths.filter { it.isRegularFile() && it.fileName.toString().endsWith("kotlin-lsp.json") }
                .map { path: Path -> LspInfo.fromJson(path) }
                .map { it.sourceFilesList }
                .collect(Collectors.toList())
                .flatten()
                .map { Paths.get(it.path) }
                .toSet()
        }
    }

	private fun formatClasspath(config: LaunchConfiguration): String = config.classpath
		.map { it.toAbsolutePath().toString() }
		.reduce { prev, next -> "$prev${File.pathSeparatorChar}$next" }

	private fun urlEncode(arg: Collection<String>?) = arg
		?.map { URLEncoder.encode(it, StandardCharsets.UTF_8.name()) }
		?.reduce { a, b -> "$a\n$b" }

	private fun urlDecode(arg: String?) = arg
		?.split("\n")
		?.map { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
		?.toList()
}
