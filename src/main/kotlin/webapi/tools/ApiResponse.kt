package webapi.tools

@kotlinx.serialization.Serializable
open class ApiResponse(
    val success: Boolean,
    val message: ResponseMessage,
    val data: String = ""
)