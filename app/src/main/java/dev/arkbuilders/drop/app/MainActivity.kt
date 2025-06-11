package dev.arkbuilders.drop.app

import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import dev.arkbuilders.drop.app.ui.home.Home
import dev.arkbuilders.drop.app.ui.receive.Receive
import dev.arkbuilders.drop.app.ui.send.Send
import dev.arkbuilders.drop.app.ui.theme.DropTheme
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            DropTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val innerModifier = Modifier.padding(innerPadding)
                    NavHost(
                        modifier = innerModifier,
                        navController = navController,
                        startDestination = "home",
                    ) {
                        composable("home") {
                            Home(navController = navController)
                        }
                        composable("send?uris={uris}") {
                            Send(navController = navController)
                        }
                        composable(
                            route = "receive?ticket={ticket}&confirmations={confirmations}",
                            deepLinks = listOf(
                                navDeepLink {
                                    uriPattern =
                                        "drop://receive?ticket={ticket}&confirmations={confirmations}"
                                })
                        ) {
                            val args = it.arguments
                            val ticket = args?.getString("ticket")
                            val confirmations =
                                args?.getString("confirmations")?.split(",")?.map { it.toUByte() }
                                    ?: emptyList()
                            val downloadDir =
                                navController.context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
                            val receiveDir = downloadDir.resolve(UUID.randomUUID().toString())
                            Receive(
                                ticket = ticket,
                                confirmations = confirmations,
                                onBack = { navController.popBackStack() },
                                onReceive = { chunks ->
                                    println("Received chunks: $chunks")
                                    if (!receiveDir.exists()) {
                                        receiveDir.mkdirs()
                                    }
                                    chunks.forEach { chunk ->
                                        val file = receiveDir.resolve(chunk.name)
                                        println("Appending chunk to file: ${file.absolutePath}")
                                        file.appendBytes(chunk.data.map { it.toByte() }
                                            .toByteArray())
                                    }
                                },
                                onScanQRCode = { deepLink ->
                                    navController.navigate(deepLink)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
