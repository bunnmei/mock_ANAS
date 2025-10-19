package space.webkombinat.a_server


import kotlinx.serialization.Serializable

@Serializable
sealed interface A_FolderOrFile

@Serializable
data class A_File(
    val type: String,
    val absolutePath: String,
    val name: String,
): A_FolderOrFile

@Serializable
data class A_Folder(
    val type: String,
    val absolutePath: String,
    val name: String,
    val open: Boolean,
    val children: List<A_FolderOrFile>? = null
): A_FolderOrFile



@Serializable
data class RequestBody(val query: String)

@Serializable
data class RequestFolderBody(
    val folderName: String,
    val path: String,
)

//enum class {}

@Serializable
data class RequestDownloadBody(
    val uri: String,
    val type: String,
)

@Serializable
data class ResponseMessage(
    val message: String
)
//@Serializable
//data class A_FolderOrFile(
//    val type: String,
//    val absolutePath: String,
//    val name: String,
//    val open: Boolean? = null,
//    val children: List<A_FolderOrFile>? = null
//)

//@Serializable
//sealed interface A_FolderOrFile
//
//@Serializable
//data class FolderWrapper(val folder: A_Folder) : A_FolderOrFile
//@Serializable
//data class FileWrapper(val file: A_File) : A_FolderOrFile
//
//@Serializable
//data class RequestBody(val query: String)