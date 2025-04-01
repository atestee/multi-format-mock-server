package cz.cvut.fit.atlasest.data

import kotlinx.serialization.json.JsonObject

data class Schema(
    val jsonObject: JsonObject,
    val properties: JsonObject,
    val required: List<String>,
)
