package space.webkombinat.a_server

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.documentfile.provider.DocumentFile


//data class FolderCheck(
//    val status: MutableState<Boolean>,
//    val text: String
//) {
//    enum class Type {
//        Exists, Readable, Writable
//    }
//}

data class FolderCheck(
    val status: MutableState<Boolean>,
    val text: String,
    val checker: (DocumentFile) -> Boolean
)

class SD_M2_SSD_Uri(
    val uri: Uri
) {
    val folderCheckList = listOf(
        FolderCheck(mutableStateOf(false), ".ANASフォルダか") { it.name == ".ANAS" },
        FolderCheck(mutableStateOf(false), ".ANASフォルダRead") { it.canRead() },
        FolderCheck(mutableStateOf(false), ".ANASフォルダWrite") { it.canWrite() })

    fun checkerRun(context: Context) {
        val rootDir = DocumentFile.fromTreeUri(context, uri)
        if (rootDir == null) return
        folderCheckList.forEachIndexed { i, checkObj ->
            checkObj.status.value = checkObj.checker(rootDir)
        }
    }
}

