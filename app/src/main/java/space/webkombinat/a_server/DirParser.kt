package space.webkombinat.a_server

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.documentfile.provider.DocumentFile
import androidx.core.net.toUri

class DirParser(
    private val activity: ComponentActivity
) {
//    content://com.android.externalstorage.documents/tree/AC0C-E948%3A
    var sdcardPath: Uri? = null
    private val openDocumentTreeLauncher =
        activity.registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                // 永続的なアクセス権を保持する
                activity.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                // 選択されたURIを表示
                sdcardPath = uri
                println("選択されたURI: $uri")
            } else {
                println("選択されなかった")
            }
        }



    fun searchANASFolderSDCard() {
        openDocumentTreeLauncher.launch(null)
    }

    fun perfomFileOperations() {
        if (sdcardPath == null) return
        val rootDir = DocumentFile.fromTreeUri(activity, sdcardPath!!)
        if (rootDir == null) {
            println("rootDir null")
        }
        if (!rootDir.canWrite()){
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
                        activity.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
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
                        activity.contentResolver.openInputStream(newFile.uri)?.use { inputStream ->
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