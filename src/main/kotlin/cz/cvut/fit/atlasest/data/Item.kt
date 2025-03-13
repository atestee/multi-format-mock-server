package cz.cvut.fit.atlasest.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Item(
    val identifier: Int,
    val data: JsonObject,
)
