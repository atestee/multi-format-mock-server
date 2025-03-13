package cz.cvut.fit.atlasest.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.everit.json.schema.Schema
import org.everit.json.schema.loader.SchemaLoader
import org.json.JSONArray
import org.json.JSONObject
import javax.validation.ValidationException
import org.everit.json.schema.ValidationException as EveritValidationException

fun inferJsonSchema(
    collectionData: String,
    identifier: String,
): Schema {
    val objectMapper = ObjectMapper()
    val jsonNode = objectMapper.readTree(collectionData)

    val schemaObject = JSONObject()

    if (jsonNode.isArray) {
        val firstObject = jsonNode.get(0)

        val allProperties = mutableSetOf<String>()
        val requiredProperties = mutableSetOf<String>()
        val properties = JSONObject()

        jsonNode.forEach { obj ->
            val presentProperties = mutableSetOf<String>()
            obj.fields().forEach { (key, _) ->
                allProperties.add(key)
                presentProperties.add(key)
            }
            if (requiredProperties.isEmpty()) {
                requiredProperties.addAll(presentProperties)
            } else {
                requiredProperties.retainAll(presentProperties)
            }
        }

        allProperties.forEach { property ->
            val propType = inferJsonType(firstObject.get(property))
            properties.put(property, propType)
        }

        val requiredArray = JSONArray()
        allProperties.forEach {
            if (requiredProperties.contains(it) && it != identifier) {
                requiredArray.put(it)
            }
        }

        schemaObject.put("type", "object")
        schemaObject.put("properties", properties)
        schemaObject.put("required", requiredArray)
    }

    return SchemaLoader.load(schemaObject)
}

fun inferJsonType(node: JsonNode): JSONObject {
    val type = JSONObject()

    when {
        node.isObject -> type.put("type", "object")
        node.isArray -> type.put("type", "array")
        node.isTextual -> type.put("type", "string")
        node.isInt -> type.put("type", "integer")
        node.isBoolean -> type.put("type", "boolean")
        node.isDouble -> type.put("type", "number")
        node.isNull -> type.put("type", "null")
        else -> type.put("type", "string")
    }

    return type
}

fun validateDataAgainstSchema(
    data: String,
    schema: Schema,
) {
    val objectMapper = ObjectMapper()
    val dataNode = objectMapper.readTree(data)

    val jsonObject = JSONObject(objectMapper.writeValueAsString(dataNode))

    try {
        schema.validate(jsonObject)
    } catch (e: EveritValidationException) {
        if (e.causingExceptions.isNotEmpty()) {
            val errors = mutableListOf<String>()
            e.causingExceptions.forEach { errors.add(it.message.toString()) }
            throw ValidationException(errors.toString())
        } else {
            throw ValidationException(e.message)
        }
    }
}
