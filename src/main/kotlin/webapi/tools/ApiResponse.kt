package webapi.tools

import kotlinx.serialization.json.JsonElement

@kotlinx.serialization.Serializable
open class ApiResponse(
    val success: Boolean,
    val message: ResponseMessage,
    val data: JsonElement? = null
) {
    companion object {
        val internalError = ApiResponse(false, ResponseMessage.INTERNAL_ERROR)
    }
}