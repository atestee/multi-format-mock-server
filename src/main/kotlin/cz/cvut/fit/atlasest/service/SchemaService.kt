package cz.cvut.fit.atlasest.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.saasquatch.jsonschemainferrer.AdditionalPropertiesPolicies
import com.saasquatch.jsonschemainferrer.EnumExtractors
import com.saasquatch.jsonschemainferrer.FormatInferrers
import com.saasquatch.jsonschemainferrer.JsonSchemaInferrer
import com.saasquatch.jsonschemainferrer.RequiredPolicies
import com.saasquatch.jsonschemainferrer.SpecVersion
import io.github.optimumcode.json.schema.JsonSchema
import io.github.optimumcode.json.schema.ValidationError
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Schema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.DayOfWeek
import java.time.Month
import javax.validation.ValidationException

class SchemaService {
    private val mapper = ObjectMapper()

    fun inferJsonSchema(collectionData: JsonArray): JsonObject {
        val inferrer =
            JsonSchemaInferrer
                .newBuilder()
                .setSpecVersion(SpecVersion.DRAFT_2020_12)
                .addFormatInferrers(FormatInferrers.email(), FormatInferrers.ip())
                .setAdditionalPropertiesPolicy(AdditionalPropertiesPolicies.notAllowed())
                .setRequiredPolicy(RequiredPolicies.nonNullCommonFields())
                .addEnumExtractors(
                    EnumExtractors.validEnum(Month::class.java),
                    EnumExtractors.validEnum(DayOfWeek::class.java),
                ).build()

        val jsonNodeList: List<JsonNode> =
            collectionData.map { jsonElement ->
                mapper.readTree(jsonElement.toString())
            }

        val inferredSchema = inferrer.inferForSamples(jsonNodeList)
        val schemaString = mapper.writeValueAsString(inferredSchema)
        val schemaJsonElement = Json.parseToJsonElement(schemaString)

        if (schemaJsonElement is JsonObject) {
            return schemaJsonElement
        } else {
            throw ValidationException("Inferred schema is not a json object")
        }
    }

    fun validateDataAgainstSchema(
        data: JsonObject,
        schema: JsonObject,
    ) {
        val validator = JsonSchema.fromDefinition(schema.toString())
        val errors = mutableListOf<ValidationError>()
        val valid = validator.validate(data, errors::add)

        if (!valid) {
            if (errors.size == 1) {
                val error = errors.first()
                throw ValidationException("#${error.objectPath}: ${error.message}")
            } else {
                throw ValidationException(errors.map { "#${it.objectPath}: ${it.message}" }.toString())
            }
        }
    }

    fun getCollectionSchema(
        collectionName: String,
        schemaString: String,
    ): JsonObject {
        val schemaObject: JsonObject = Json.parseToJsonElement(schemaString).jsonObject
        val collectionSchema = schemaObject[collectionName]
        return if (collectionSchema is JsonObject) {
            collectionSchema
        } else {
            throw ValidationException("Schema for collection '$collectionName' is missing!")
        }
    }

    fun convertJsonSchemaToOpenApi(jsonSchema: JsonElement): Schema<Any> {
        val openApiSchema = Schema<Any>()

        jsonSchema.jsonObject["\$ref"]?.jsonPrimitive?.content?.let { ref ->
            openApiSchema.`$ref` = ref
            return openApiSchema
        }

        jsonSchema.jsonObject["\$defs"]?.let { defs ->
            println(defs)
        }

        val type = jsonSchema.jsonObject["type"]?.jsonPrimitive?.content
        if (type != null) {
            openApiSchema.addType(type)
        }

        if (type == "object") {
            val propertiesNode = jsonSchema.jsonObject["properties"]?.jsonObject
            val propertiesMap = mutableMapOf<String, Schema<Any>>()

            propertiesNode?.forEach { (key, valueNode) ->
                val obj = Json.parseToJsonElement(valueNode.toString())
                propertiesMap[key] = convertJsonSchemaToOpenApi(obj)
            }
            openApiSchema.properties = propertiesMap
        } else if (type == "array") {
            val itemsNode = jsonSchema.jsonObject["items"]
            if (itemsNode != null) {
                val arraySchema = ArraySchema()
                arraySchema.items = convertJsonSchemaToOpenApi(itemsNode)
                return arraySchema
            }
        }

        val enumValues = jsonSchema.jsonObject["enum"]?.jsonArray?.map { it.jsonPrimitive.content }
        if (enumValues != null) {
            openApiSchema.enum = enumValues
        }

        jsonSchema.jsonObject["format"]?.jsonPrimitive?.content?.let { format ->
            openApiSchema.format = format
        }

        val requiredNode = jsonSchema.jsonObject["required"]?.jsonArray
        requiredNode?.let {
            openApiSchema.required = it.map { it.jsonPrimitive.content }
        }

        return openApiSchema
    }
}
