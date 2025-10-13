package space.webkombinat.a_server

import androidx.compose.runtime.mutableStateOf

class ServerStatus {

    val server_status = mutableStateOf(false)
    val server_status_text = "local web serverが動いているか。"

    val server_port_state = mutableStateOf("")
}

//StartWebServer
//StopWebServer