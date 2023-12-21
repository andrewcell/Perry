package webapi.controller

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import tools.StringXmlParser
import webapi.tools.ApiResponse
import webapi.tools.ResponseMessage

@Serializable
data class SearchRequest(
    val type: SearchType,
    val byName: String? = null,
    val byId: Int? = null
)

@Serializable(with = SearchTypeSerializer::class)
enum class SearchType(val value: String) {
    MAP("map"),
    ITEM("item"),
    NPC("npc"),
    SKILL("skill"),
    MOB("mob")
}

private class SearchTypeSerializer : EnumValueSerializer<SearchType>(
    "searchType", { it.value }, { v -> SearchType.entries.find { it.value == v } ?: SearchType.MAP }
)

open class EnumValueSerializer<T : Enum<*>>(
    serialName: String,
    val serialize: (v: T) -> String,
    val deserialize: (v: String) -> T
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString(serialize(value))
    }

    override fun deserialize(decoder: Decoder): T {
        val v = decoder.decodeString()
        return deserialize(v)
    }
}

data class ItemSearchRequest(
    val id: Int? = null,
    val name: String? = null
)


/**
 * Controller for search of items, maps, npcs, skills and mobs ONLY for Admins
 */
fun Route.adminSearch() {
    authenticate("auth") {
        route("/admin") {
            post("/search") {
                val principal = call.principal<JWTPrincipal>() ?: return@post
                val isAdmin = (principal.payload.getClaim("gm").asInt() ?: -1) > 1
                val name = principal.payload.getClaim("name").asString()
                if (!isAdmin || name == null) return@post
                val request = call.receive<SearchRequest>()
                val result = StringXmlParser.find(request.type, request.byId, request.byName)
                call.respond(ApiResponse(true, ResponseMessage.SUCCESS, Json.encodeToJsonElement(result)))

            }
        }
//        route("/admin/search") {
//            post("/item") {
//                val principal = call.principal<JWTPrincipal>()
//                if (principal == null || principal.payload.getClaim("gm").isNull)
//                    return@post
//                val body = call.receive<ItemSearchRequest>()
//                if (body.id == null && body.name == null) {
//                    return@post call.respond(
//                        HttpStatusCode.NotFound, ApiResponse(
//                            success = false,
//                            message = ResponseMessage.NO_TARGET_FOUND
//                        )
//                    )
//                }
//                val items = ItemInformationProvider.getAllItems()
//            }
//        }
    }
}
