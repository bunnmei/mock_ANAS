package space.webkombinat.a_server

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.routing.get
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import space.webkombinat.a_server.ui.theme.A_serverTheme
import java.net.Inet4Address
import java.net.NetworkInterface


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sd = DirParser(this)
        enableEdgeToEdge()
        setContent {
            A_serverTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding)
                            .fillMaxSize()
                        ,
                        horizontalAlignment = Alignment.CenterHorizontally
                        ,
                        verticalArrangement = Arrangement.Center
                    ) {

                        Button(onClick = {
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
                                        routing {
                                            get("/") {
                                                call.respondText("Hello, world!")
                                            }
                                        }
                                    }.start(wait = false)

                                    Log.d("KtorServer", "Server started on port 8080")
                                } catch (e: Exception) {
                                    Log.e("KtorServer", "Error starting server", e)
                                }
                            }
                        }) {
                            Text("Server Start")
                        }

                        Button(onClick = {
                           sd.searchANASFolderSDCard()
                        }){
                            Text("Search SD")
                        }

                        Button(onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                sd.perfomFileOperations()
                            }
                        }){
                            Text("File Operations")
                        }

                        Text(getLocalIpAddress())
                    }
//                    Greeting(
//                        name = "Android",
//                        modifier = Modifier.padding(innerPadding)
//                    )
                }


            }
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.hostAddress.toString()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("KtorServer", "Error getting IP address", e)
        }
        return "Unknown IP"
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    A_serverTheme {
        Greeting("Android")
    }
}