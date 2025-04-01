package cz.cvut.fit.atlasest.routing.routes

import cz.cvut.fit.atlasest.service.CollectionService
import io.github.smiley4.ktoropenapi.delete
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route

fun Route.deleteRoute(
    collectionService: CollectionService,
    collectionName: String,
) {
    delete("/$collectionName/{id}", {
        tags(collectionName)
    }) {
        val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing ID")
        collectionService.deleteItemFromCollection(collectionName, id)
        call.respond(HttpStatusCode.OK)
    }
}
