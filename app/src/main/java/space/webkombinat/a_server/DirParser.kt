package space.webkombinat.a_server

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.rememberTransition
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.documentfile.provider.DocumentFile
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.Url
import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import io.ktor.server.application.hooks.ResponseBodyReadyForSend
import io.ktor.util.asStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


enum class FolderMake{
    ALREADY_EXIST,
    MADE,
    FAIL
}

class DirParser(
    private val context: Context
) {

//    SAF strage access framework
    private var sdcardPath: Uri? = null

    var storageList = mutableStateListOf<SD_M2_SSD_Uri>()

//    @RequiresApi(Build.VERSION_CODES.R)
    fun setUri(uri: Uri) {
        sdcardPath = uri
//        getStorageStats()
    }

//    @RequiresApi(Build.VERSION_CODES.R)
//    fun getStorageStats(){
//        val docId = DocumentsContract.getTreeDocumentId(sdcardPath)
//        val parts = docId.split(":")
//        val volumeId = parts.firstOrNull()
//
//
//        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
//        val storageVolumes = storageManager.storageVolumes
//
//        // UUID またはパスにマッチするボリュームを探す
//        val volume = storageVolumes.find { it.uuid == volumeId || it.getDescription(context) == volumeId }
//
//
//        val path = volume?.directory
//
//        val stat = StatFs(path?.absolutePath)
//        val total = stat.totalBytes
//        val free = stat.availableBytes
//
//        println("free : ${free / (1024 * 1024 * 1024)}GB / total : ${total / (1024 * 1024 * 1024)}GB")
//    }

//    fun findVolumeAndStats(context: Context, uri: Uri) {
//        val sm = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
//        for (v in sm.storageVolumes) {
//            val path = v.directory ?: continue
//            if (uri.toString().contains(v.uuid ?: "")) {
//                val stat = StatFs(path.absolutePath)
//                Log.d("Storage", "Volume: ${v.getDescription(context)}")
//                Log.d("Storage", "Total: ${stat.totalBytes / (1024 * 1024)} MB")
//                Log.d("Storage", "Free: ${stat.availableBytes / (1024 * 1024)} MB")
//            }
//        }
//    }

//    @Suppress("DEPRECATION")
//    fun getStorageStatsCompat(context: Context, treeUri: Uri): Pair<Long, Long>? {
//        val docId = DocumentsContract.getTreeDocumentId(treeUri)
//        val volumeId = docId.substringBefore(":")
//
//        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
//
//        // --- Android 11以降（API30+） ---
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            val volume = storageManager.storageVolumes.find { it.uuid == volumeId || it.isPrimary && volumeId == "primary" }
//            val path = volume?.directory ?: return null
//            val stat = StatFs(path.absolutePath)
//            return stat.totalBytes to stat.availableBytes
//        }
//
//        // --- Android 10以前（API29以下） ---
//        else {
//            val path = when (volumeId) {
//                "primary" -> Environment.getExternalStorageDirectory()
//                else -> {
//                    // 外部ストレージのリストからUUIDが一致するものを探す
//                    val candidates = context.getExternalFilesDirs(null)
//                    candidates.firstOrNull { it != null && it.path.contains(volumeId, ignoreCase = true) }
//                }
//            } ?: return null
//
//            val stat = StatFs(path.path)
//            return stat.totalBytes to stat.availableBytes
//        }
//    }
    fun openAssets(): ByteArray {
        val bytes = context.assets.open("index.html").readBytes()
        return bytes
    }

    fun  openAssetsRewrite(): String {
        val text = context.assets.open("index.html").bufferedReader().use { it.readText() }
        var linkTagString = ""
        storageList.forEach { uri ->
            linkTagString += "<a href=\"/${uri.getUriUuid()}\">/${uri.getUriUuid()}</a>"
        }
        val reWrite = text.replace("*", linkTagString)
        println(reWrite)
        return reWrite
    }

    fun checkUir(): Boolean {
        if (sdcardPath != null) {
            return true
        }
        return false
    }

    fun getDocumentRoot(): DocumentFile {
        var currentDir = DocumentFile.fromTreeUri(context, sdcardPath!!)
        return currentDir!!
    }

    fun makeZip(path: String): File {
        val folderName = path.split("/").filter { it.isNotEmpty() }
        val outputPath = context.cacheDir.resolve("${folderName.last()}.zip").absolutePath

        zipFolderFromDocumentFile(
            relativePath = path,
            outputZipPath = outputPath
        )

        println("ZIP作成完了: $outputPath")
        val zipFile = File(context.cacheDir, "${folderName.last()}.zip")
        return zipFile
    }

    fun zipFolderFromDocumentFile( relativePath: String, outputZipPath: String) {
        // relativePath を辿って目的フォルダを取得
        var targetDir = getDocumentRoot()
        relativePath.split("/").forEach { part ->
            if (part.isNotEmpty()) {
                targetDir = targetDir.findFile(part) ?: throw Exception("フォルダが見つかりません: $part")
            }
        }

        // ZIP出力先（例：/data/data/your.app/cache/folder.zip）
        val zipOut = ZipOutputStream(FileOutputStream(outputZipPath))

        // 再帰的にフォルダを圧縮
        fun addDocumentToZip(dir: DocumentFile, basePath: String) {
            for (file in dir.listFiles()) {
                if (file.isDirectory) {
                    addDocumentToZip(file, "$basePath${file.name}/")
                } else if (file.isFile) {
                    val entry = ZipEntry("$basePath${file.name}")
                    zipOut.putNextEntry(entry)

                    val inputStream: InputStream? = try {
                        context.contentResolver.openInputStream(file.uri)
                    } catch (e: Exception) {
                        null
                    }

                    inputStream?.use {
                        it.copyTo(zipOut)
                    }

                    zipOut.closeEntry()
                }
            }
        }

        addDocumentToZip(targetDir, "")
        zipOut.close()
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

    fun makeDir(path: String, dirName: String): FolderMake {
        var currentDir = DocumentFile.fromTreeUri(context, sdcardPath!!)

        val parts = path.split("/").filter { it.isNotEmpty() }

        for (part in parts) {
            val nextDir = currentDir?.findFile(part)
            currentDir = if (nextDir == null || !nextDir.isDirectory) {
                // 無ければ作る
                currentDir?.createDirectory(part)
            } else {
                nextDir
            }
        }

        val existing = currentDir?.findFile(dirName)
        if (existing != null && existing.isDirectory) {
            Log.d("makeDir", "既に存在します: ${existing.uri}")
            return FolderMake.ALREADY_EXIST
        }

        val newDir = currentDir?.createDirectory(dirName)
        Log.d("makeDir", "作成しました: ${newDir?.uri}")
        return FolderMake.MADE
    }

    fun writeFile(path: String) {

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