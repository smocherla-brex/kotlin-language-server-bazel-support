package org.javacs.ktda

import java.nio.file.Path
import java.nio.file.Paths
import org.eclipse.lsp4j.debug.ConfigurationDoneArguments
import org.eclipse.lsp4j.debug.InitializeRequestArguments
import org.eclipse.lsp4j.debug.DisconnectArguments
import org.eclipse.lsp4j.debug.OutputEventArguments
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import org.javacs.ktda.adapter.KotlinDebugAdapter
import org.javacs.ktda.jdi.launch.JDILauncher
import org.junit.After
import org.junit.Before
import org.javacs.ktda.builder.BuildService
import java.util.concurrent.CompletableFuture

abstract class DebugAdapterTestFixture(
    relativeWorkspaceRoot: String,
    private val mainClass: String,
    private val vmArguments: String = ""
) : IDebugProtocolClient {
    val absoluteWorkspaceRoot: Path = Paths.get(DebugAdapterTestFixture::class.java.getResource("/bazel/WORKSPACE").toURI()).parent.resolve(relativeWorkspaceRoot)
    lateinit var debugAdapter: KotlinDebugAdapter

    class MockBuildService : BuildService {
        override fun build(workspaceRoot: Path, targets: List<String>, args: List<String>): CompletableFuture<Void> {
            return CompletableFuture.completedFuture(null)
        }
    }

    @Before fun startDebugAdapter() {

        debugAdapter = JDILauncher()
            .let{
                KotlinDebugAdapter(it, MockBuildService())
            }
            .also {
                it.connect(this)
                val configDone = it.configurationDone(ConfigurationDoneArguments())
                it.initialize(InitializeRequestArguments().apply {
                    adapterID = "test-debug-adapter"
                    linesStartAt1 = true
                    columnsStartAt1 = true
                }).join()
                // Slightly hacky workaround to ensure someone is
                // waiting on the ConfigurationDoneResponse. See
                // KotlinDebugAdapter.kt:performInitialization for
                // details.
                Thread {
                    configDone.join()
                }.start()
                // Wait until the thread has blocked on the future
                while (configDone.numberOfDependents == 0) {
                    Thread.sleep(100)
                }
            }
    }

    fun launch() {
        println("Launching...")
        debugAdapter.launch(mapOf(
            "workspaceRoot" to absoluteWorkspaceRoot.toString(),
            "bazelTarget" to "//foo/bar",
            "mainClass" to mainClass,
            "buildFlags" to listOf<String>(),
            "vmArguments" to vmArguments
        )).join()
        println("Launched")
    }

    @After fun closeDebugAdapter() {
        debugAdapter.disconnect(DisconnectArguments()).join()
    }

    override fun output(args: OutputEventArguments) {
        println(args.output)
    }
}
