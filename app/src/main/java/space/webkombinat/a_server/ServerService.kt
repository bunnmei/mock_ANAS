package space.webkombinat.a_server

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import dagger.hilt.android.AndroidEntryPoint
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class ServerService: Service() {
    @Inject lateinit var sd: DirParser

    private var server: NettyApplicationEngine? = null
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MyApp::WebServerWakeLock"
        )
        wakeLock.acquire() // ← CPUスリープ防止
        startForegroundNotification()
        startServer()
    }

    private fun startForegroundNotification() {
        val channelId = "webserver_channel"
        val channel = NotificationChannel(
            channelId,
            "Local Web Server",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Webサーバー稼働中")
            .setContentText("スマホがローカルサーバーとして動作しています")
            .setSmallIcon(R.drawable.stat_sys_upload)
            .build()

        startForeground(1, notification)
    }

    private fun startServer() {
        server = embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
            install(CORS) {
                anyHost()
                allowHeader(HttpHeaders.ContentType)
            }
//            install(Content)
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                    classDiscriminator = "kind"
                })
            }
            routing {
                get("/") {
                    call.respondText("Server is running", ContentType.Text.Plain)
                }

                post("/sd") {
                    val body = call.receive<RequestBody>()
                    println("受け取った文字列: ${body.query}")
                    val folders = sd.readDir(body.query)

                    call.respond(folders)
                }

                post("/upload") {
                    val multipart = call.receiveMultipart()
                    var savedFileName: String? = null

                    multipart.forEachPart { part ->
                        if (part is PartData.FileItem) {
                            savedFileName = part.originalFileName ?: "uploaded_file"
                            sd.writePartDataToDirectory(part)
                            part.dispose()
                        }
                    }
                    println("アップロードされたファイル名: $savedFileName")
                    call.respondText("File uploaded: $savedFileName", status = HttpStatusCode.OK)
                }
            }
        }.start(wait = false)
    }

    override fun onDestroy() {
        server?.stop(1000, 2000)
        wakeLock.release()
        super.onDestroy()
    }
}

//                        if (part is PartData.FileItem) {
//                            val fileName = part.originalFileName ?: "uploaded_file"
//                            val file = File("${sd}/uploads/$fileName")
//                            file.parentFile.mkdirs()
//
//                            part.streamProvider().use { input ->
//                                file.outputStream().use { output ->
//                                    input.copyTo(output)
//                                }
//                            }
//                            savedFileName = fileName
//                        }



//                    val folders = listOf(
//                        A_Folder(
//                            absolutePath = "/",
//                            name = "root",
//                            open = true,
//                            type = "folder",
//                            children = listOf(
//                                FolderWrapper(
//                                    A_Folder(
//                                        absolutePath = "/root/docs",
//                                        name = "docs",
//                                        open = false,
//                                        type = "folder",
//                                        children = listOf()
//                                    )
//                                ),
//                                FileWrapper(
//                                    A_File(
//                                        absolutePath = "/root/readme.txt",
//                                        name = "readme.txt",
//                                        type = "file"
//                                    )
//                                )
//                            ),
//                        )
//                    )
