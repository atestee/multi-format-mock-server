package cz.cvut.fit.atlasest.routing.collectionRoutes

import cz.cvut.fit.atlasest.services.CollectionService
import io.github.smiley4.ktoropenapi.delete
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route

fun Route.deleteRoute(
    collectionName: String,
    collectionService: CollectionService,
) {
    delete("/$collectionName/{id}", {
        tags(collectionName)
        response {
            code(HttpStatusCode.OK) {
                description = "Request processed successfully"
            }
        }
    }) {
        val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing ID")
        collectionService.deleteItemFromCollection(collectionName, id)
        call.respond(HttpStatusCode.OK)
    }
}
