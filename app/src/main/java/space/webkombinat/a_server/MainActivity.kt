package space.webkombinat.a_server

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import space.webkombinat.a_server.ui.theme.A_serverTheme
import java.net.Inet4Address
import java.net.NetworkInterface


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sd: DirParser

    val openDocumentTreeLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                // 永続的なアクセス権を保持する
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val newUri = SD_M2_SSD_Uri(uri)
                newUri.checkerRun(context = applicationContext)
                sd.storageList.add(newUri)
                println("選択されたURI: $uri")
            } else {
                println("選択されなかった")
            }
        }


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            A_serverTheme {
                val context = LocalContext.current
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding)
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
//                        verticalArrangement = Arrangement.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            sd.storageList.forEach { uri ->
                                Column {
                                    uri.folderCheckList.forEach { checkObj ->
                                        CheckStatus(
                                            label = checkObj.text,
                                            status = checkObj.status.value,
                                        )
                                    }
                                }
                                HorizontalDivider(
                                    Modifier,
                                    DividerDefaults.Thickness,
                                    DividerDefaults.color
                                )
                            }

                            if (sd.storageList.isEmpty()) {
                                Text("SD,M2,SSD未選択")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(0.8f),
                            ) {
                                Spacer(modifier = Modifier.weight(1f))
                                Button(onClick = {
                                    openDocumentTreeLauncher.launch(null)
                                }) {
                                    Text("Add External Storage")
                                }
                            }
                        }
                        // ex storage setting 上
                        Spacer(modifier = Modifier.weight(1f))
                        // Server setting
                        sd.storageList.forEach { uri ->
                            Text("Port: ${getLocalIpAddress()}/${uri.getUriUuid()}")
                        }
                        Row(modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(60.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(onClick = {
                            }) {
                                Text("Server Stop")
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Button(onClick = {
    //                             val intent = Intent(application, ServerService::class.java)
    //                             application.startForegroundService(intent)
                            }) {
                                Text("Server Start")
                            }
                        }

                    }
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

//val storageManager = context.getSystemService(StorageManager::class.java)
//val volumes = storageManager.storageVolumes
//
//for (vol in volumes) {
//    val uuid = vol.uuid // USBやSDカードごとにユニーク
//    val desc = vol.getDescription(context) // "SDカード", "USBメモリ"など
//
////                                val path = vol.directory?.absolutePath
//    val s = vol.state
//
//    val e = vol.directory
//
//    println("UUID: $uuid, Description: $desc  -- $s ::: $e")
//}
