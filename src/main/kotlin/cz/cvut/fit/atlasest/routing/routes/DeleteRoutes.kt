package cz.cvut.fit.atlasest.routing.routes

import com.cesarferreira.pluralize.singularize
import cz.cvut.fit.atlasest.service.CollectionService
import cz.cvut.fit.atlasest.utils.getFieldValue
import io.github.smiley4.ktoropenapi.delete
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlinx.serialization.json.jsonPrimitive

fun Route.deleteRoute(
    collectionService: CollectionService,
    collectionName: String,
) {
    delete("/$collectionName/{id}", {
        tags(collectionName)
    }) {
        val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing ID")
        collectionService.deleteItemFromCollection(collectionName, id)

        val foreignKey = collectionName.singularize() + "Id"

        collectionService.collections.forEach { collectionName, collection ->
            collection.items.removeIf { it.getFieldValue(foreignKey)?.jsonPrimitive?.content == id }
        }

        call.respond(HttpStatusCode.OK)
    }
}
