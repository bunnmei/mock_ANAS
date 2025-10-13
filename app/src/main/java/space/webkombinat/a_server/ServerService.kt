package space.webkombinat.a_server

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.AndroidEntryPoint
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.defaultForFile
import io.ktor.http.defaultForFilePath
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
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        val root = sd.getDocumentRoot()

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

                get("/public/{path...}") {
                    val segments = call.parameters.getAll("path") ?: return@get call.respondText("Not found", status = HttpStatusCode.NotFound)
                    val file = if (segments.isEmpty()) root else findDocumentFile(root, segments.joinToString("/"))


                    if (file == null) {
                        call.respond(HttpStatusCode.NotFound)
                        return@get
                    }

                    if (file.isDirectory) {
                        println("in directory")
                        // 🔹 ディレクトリ → HTMLリスト表示
                        val parentPath = segments.dropLast(1).joinToString("/")
                        val files = file.listFiles().sortedBy { it.name ?: "" }

                        val html = buildString {
                            append("<html><head><meta charset='utf-8'><title>")
                            append(file.name ?: "Folder")
                            append("</title><style>")
                            append("body{font-family:sans-serif;background:#fafafa;margin:1em}")
                            append("a{text-decoration:none;color:#007bff}")
                            append("a:hover{text-decoration:underline}")
                            append("table{border-collapse:collapse;width:100%;max-width:800px}")
                            append("th,td{padding:6px;border-bottom:1px solid #eee;text-align:left}")
                            append("</style></head><body>")
                            append("<h3>📂 /public/${segments.joinToString("/")}</h3>")
                            if (segments.isNotEmpty()) {
                                append("<div><a href='/public/$parentPath'>⬆️ 上へ戻る</a></div><hr>")
                            }
                            append("<table>")
                            append("<tr><th>名前</th><th>種類</th><th>サイズ</th></tr>")
                            for (f in files) {
                                val name = f.name ?: "(無名)"
                                val path = (segments + name).joinToString("/")
                                if (f.isDirectory) {
                                    append("<tr><td><a href='/public/$path'>📁 $name</a></td><td>フォルダ</td><td></td></tr>")
                                } else {
                                    val sizeKB = if (f.length() > 0) String.format("%.1f KB", f.length() / 1024.0) else ""
                                    append("<tr><td><a href='/public/$path'>📄 $name</a></td><td>${f.type ?: "ファイル"}</td><td>$sizeKB</td></tr>")
                                }
                            }
                            append("</table></body></html>")
                        }
                        call.respondText(html, ContentType.Text.Html)
                        return@get
                    }



                    withContext(Dispatchers.IO) {
                        val mime = contentResolver.getType(file.uri)
                            ?: ContentType.defaultForFilePath(file.uri.toString()).toString()
                        val filename = file.name ?: "file"

                        contentResolver.openInputStream(file.uri)?.use { input ->
//                            val bytes = input.readBytes()

                            // Content-Disposition を条件で変える
                            if (mime.startsWith("application/pdf") ||
                                mime.startsWith("image/") ||
                                mime.startsWith("text/") ||
                                mime.startsWith("video/") ||
                                mime.startsWith("audio/")
//                                mime.startsWith("application/")
                            ) {
                                // 🔹 ブラウザ内表示
                                call.response.header(HttpHeaders.ContentDisposition, "inline; filename=\"$filename\"")
                            } else {
                                // 🔹 ダウンロード
                                call.response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"$filename\"")
                            }

                            // 🔹 ここでストリーム転送
                           contentResolver.openInputStream(file.uri)?.use { input ->
                                call.respondOutputStream(contentType = ContentType.parse(mime)) {
                                    input.copyTo(this)  // バッファ単位でコピーするのでメモリ消費が少ない
                                }
                            } ?: call.respond(HttpStatusCode.InternalServerError)
                        } ?: call.respond(HttpStatusCode.InternalServerError)
                    }
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

    fun findDocumentFile(root: DocumentFile,  path: String): DocumentFile? {
        var current = root
        if (path.isBlank()) return root
        val parts = path.split("/").filter { it.isNotBlank() }
        for (p in parts) {
            current = current.findFile(p) ?: return null
        }
        return current
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
