package space.webkombinat.a_server

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.documentfile.provider.DocumentFile
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.Url
import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import io.ktor.server.application.hooks.ResponseBodyReadyForSend
import io.ktor.util.asStream
import java.io.InputStream

class DirParser(
    private val context: Context
) {

//    SAF strage access framework
    private var sdcardPath: Uri? = null

    fun setUri(uri: Uri) {
        sdcardPath = uri
    }

    fun checkUir(): Boolean {
        if (sdcardPath != null) {
            return true
        }
        return false
    }

    fun readDir(path : String): MutableList<A_FolderOrFile> {
        var folders: MutableList<A_FolderOrFile> = mutableListOf()
        if (sdcardPath == null) return folders
        var currentDir = DocumentFile.fromTreeUri(context, sdcardPath!!)
        val segments = path.split("/").filter { it.isNotEmpty() }
        for (segment in segments) {
            currentDir = currentDir?.listFiles()?.firstOrNull {
                it.isDirectory && it.name == segment
            }
        }


        // 最終的なフォルダにあるファイルを列挙
        currentDir?.listFiles()?.forEach { file ->
            if(file.isDirectory) { //dir
                val newDir =
                    A_Folder(
                        absolutePath = getRelativePathFromUri(file.uri).toString(),
                        name = file.name ?: "no name",
                        open = false,
                        type = "folder",
                        children = listOf()
                    )
                folders.add(newDir)
            } else { //file
                val newFile =
                    A_File(
                        absolutePath = getRelativePathFromUri(file.uri).toString(),
                        name = file.name ?: "no name",
                        type = "file"
                    )

                folders.add(newFile)
            }
            Log.d("LIST", "${if (file.isDirectory) "[DIR]" else "[FILE]"} ${file.name}")
        } ?: run {
            Log.e("LIST", "フォルダが見つかりません: $path")
        }
        folders.forEach { Log.d("LIST", it.toString()) }
        return folders
//        val children = docFile?.listFiles()
//
//        children?.forEach { file ->
//            Log.d("NAME", file.name ?: "(no name)")
//            if (file.isDirectory) {
//                Log.d("TYPE", "Directory")
//            } else {
//                Log.d("TYPE", "File")
//            }
//        }
    }

    fun getRelativePathFromUri(uri: Uri): String? {
        val docId = DocumentsContract.getDocumentId(uri)
        // 例: "AC0C-E948:xzs/MyDataFolder"
        val parts = docId.split(":")
        if (parts.size <= 1) return null

        val path = Uri.decode(parts[1]) // → "xzs/MyDataFolder"
        val index = path.indexOf('/')

        // "/"があるなら、それ以降を返す
        return if (index >= 0) {
            path.substring(index) // → "/MyDataFolder"
        } else {
            "/" // xzsだけならルート扱い
        }
    }
    fun writePartDataToDirectory(partData: PartData) {

        val contentResolver = context.contentResolver
        val rootDir = DocumentFile.fromTreeUri(context, sdcardPath!!)

        if (rootDir == null || !rootDir.canWrite()) {
            println("rootDir null または書き込み権限がありません。")
            return
        }

        // 1. ファイル名とMIMEタイプを取得
        val fileName = partData.name ?: "default_file"
        val mimeType = partData.contentType?.toString() ?: "*/*"

        // 2. 新しいファイルをDocumentFileとして作成
        val newFile = rootDir.createFile(mimeType, fileName)

        if (newFile != null) {
            try {
                // 3. PartDataのコンテンツInputStreamを取得
                // 💡 KtorのPartDataからInputStreamを取得する適切な方法に依存
                val partInputStream: InputStream = when (partData) {
                    is PartData.FileItem -> partData.streamProvider()
                    is PartData.BinaryItem -> partData.provider().asStream()
                    // 他のPartData型（FormItemなど）はここでは除外
                    else -> {
                        println("このPartDataの型はファイル書き込みに対応していません。")
                        return
                    }
                }

                // 4. ContentResolverからOutputStreamを開き、コピー
                contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                    partInputStream.use { inputStream ->
                        // inputStreamの内容をoutputStreamに効率的にコピー
                        inputStream.copyTo(outputStream)
                    }
                    println("ファイル書き込み成功: ${newFile.uri}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("ファイル書き込み失敗: ${e.message}")
            }
        } else {
            println("ファイル作成失敗")
        }
    }


    fun perfomFileOperations() {
        if (!checkUir()) return
        val contentResolver = context.contentResolver
        val rootDir = DocumentFile.fromTreeUri(context, sdcardPath!!)

        if (rootDir == null) {
            println("rootDir null")
        }
        if (!rootDir!!.canWrite()){
            println("rootDir can't Write")
        }
        if (rootDir != null && rootDir.canWrite()) {
            val newFolder = rootDir.createDirectory("MyDataFolder")
            if (newFolder != null) {
                println("フォルダ作成成功: ${newFolder.name}")

                // 3. ファイルの作成
                // ファイル名は、MIMEタイプと一緒に指定します。
                val newFile = newFolder.createFile("text/plain", "log_file.txt")

                if (newFile != null) {
                    println("ファイル作成成功: ${newFile.name}")

                    // 4. ファイルへの書き込み

                    try {
                        contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                            val text = "これはUSBストレージに書き込むテストデータです。\n"
                            outputStream.write(text.toByteArray())
                            println("ファイル書き込み成功")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        println("ファイル書き込み失敗: ${e.message}")
                    }

                    // 5. ファイルの読み取り
                    try {
                        contentResolver.openInputStream(newFile.uri)?.use { inputStream ->
                            val content = inputStream.bufferedReader().use { it.readText() }
                            println("ファイル読み取り成功:\n$content")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        println("ファイル読み取り失敗: ${e.message}")
                    }

                    // 6. ファイルの削除 (必要に応じて)
                    // val isDeleted = newFile.delete()
                    // println("ファイル削除: $isDeleted")
                } else {
                    println("ファイル作成失敗")
                }
            } else {
                println("フォルダ作成失敗")
            }
        } else {
            println("指定されたURIでの書き込みができません。")
        }
    }

}