package cz.cvut.fit.atlasest.data

import kotlinx.serialization.json.JsonObject

data class Item(
    val identifier: Int,
    val data: JsonObject,
)
