package cz.cvut.fit.atlasest.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.saasquatch.jsonschemainferrer.AdditionalPropertiesPolicies
import com.saasquatch.jsonschemainferrer.EnumExtractors
import com.saasquatch.jsonschemainferrer.FormatInferrers
import com.saasquatch.jsonschemainferrer.JsonSchemaInferrer
import com.saasquatch.jsonschemainferrer.RequiredPolicies
import com.saasquatch.jsonschemainferrer.SpecVersion
import cz.cvut.fit.atlasest.exceptions.ParsingException
import cz.cvut.fit.atlasest.utils.toJsonElement
import cz.cvut.fit.atlasest.utils.toJsonObject
import io.github.optimumcode.json.schema.JsonSchema
import io.github.optimumcode.json.schema.ValidationError
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Schema
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.DayOfWeek
import java.time.Month
import javax.validation.ValidationException

/**
 * A service for handling JSON schema operations such as inference, validation, and conversion.
 */
class SchemaService {
    private val mapper = ObjectMapper()
    private val specVersion = SpecVersion.DRAFT_2020_12
    private val specVersionUrl = "https://json-schema.org/draft/2020-12/schema#"

    /**
     * Infers a JSON schema using the DRAFT_2020_12 version based on the provided JSON array.
     *
     * @param collection JSON array with objects representing a data collection.
     *
     * @return The JSON object representing the inferred JSON schema.
     */
    fun inferJsonSchema(collection: JsonArray): JsonObject {
        val inferrer =
            JsonSchemaInferrer
                .newBuilder()
                .setSpecVersion(specVersion)
                .addFormatInferrers(FormatInferrers.email(), FormatInferrers.ip())
                .setAdditionalPropertiesPolicy(AdditionalPropertiesPolicies.notAllowed())
                .setRequiredPolicy(RequiredPolicies.nonNullCommonFields())
                .addEnumExtractors(
                    EnumExtractors.validEnum(Month::class.java),
                    EnumExtractors.validEnum(DayOfWeek::class.java),
                ).build()

        val jsonNodeList: List<JsonNode> =
            collection.map { jsonElement ->
                mapper.readTree(jsonElement.toString())
            }

        val inferredSchema = inferrer.inferForSamples(jsonNodeList)
        return mapper.writeValueAsString(inferredSchema).toJsonObject()
    }

    /**
     * Converts all data types in the given JSON object according to the provided JSON schema.
     * For example the string "10" will be transformed to integer 10, if the schema defines the property as integer.
     *
     * @param jsonSchema JSON schema describing the structure of expected data structure.
     * @param jsonObject The JSON object to be converted.
     *
     * @return The JSON object with correct data types.
     * @throws ValidationException If the JSON schema is invalid.
     */
    fun convertTypes(
        jsonSchema: JsonObject,
        jsonObject: JsonObject,
    ): JsonObject {
        val properties =
            jsonSchema["properties"]?.jsonObject ?: throw ValidationException("#: missing properties field")
        val jsonObjectWithTypes = convertTypesInJson("#", jsonObject, properties)
        return jsonObjectWithTypes
    }

    /**
     * Validates a JSON object against a JSON schema.
     *
     * @param jsonObject The JSON object to be validated.
     * @param jsonSchema The JSON schema describing the expected structure.
     * @throws ValidationException If validation errors occur.
     */
    fun validateDataAgainstSchema(
        jsonObject: JsonObject,
        jsonSchema: JsonObject,
    ) {
        val validator = JsonSchema.fromDefinition(jsonSchema.toString())
        val errors = mutableListOf<ValidationError>()
        val valid = validator.validate(jsonObject, errors::add)

        if (!valid) {
            if (errors.size == 1) {
                val error = errors.first()
                throw ValidationException("#${error.objectPath}: ${error.message}")
            } else {
                throw ValidationException(errors.map { "#${it.objectPath}: ${it.message}" }.toString())
            }
        }
    }

    /**
     * Retrieves a JSON schema for a specified collection from a JSON schema collection.
     *
     * @param collectionName The name of the collection whose schema is retrieved.
     * @param schemaCollection A JSON object containing multiple schemas.
     *
     * @return The JSON schema for the specified collection.
     * @throws ValidationException If the schema cannot be found or parsed.
     */
    fun getCollectionSchema(
        collectionName: String,
        schemaCollection: JsonObject,
    ): JsonObject? {
        val schema = schemaCollection[collectionName] ?: return null
        val schemaJsonObject =
            schema as? JsonObject
                ?: throw ParsingException("Schema for collection '$collectionName' is not a JSON object")
        validateSchemaVersion(schemaJsonObject)
        return schema
    }

    /**
     * Converts a JSON schema to a OpenAPI schema.
     *
     * @param jsonSchema The JSON schema to be converted.
     *
     * @return The corresponding OpenAPI schema.
     */
    fun convertJsonSchemaToOpenApi(jsonSchema: JsonElement): Schema<Any> {
        val openApiSchema = Schema<Any>().apply { specVersion = io.swagger.v3.oas.models.SpecVersion.V31 }

        val type = jsonSchema.jsonObject["type"]?.jsonPrimitive?.content
        if (type != null) {
            openApiSchema.addType(type)
        }

        if (type == "object") {
            val propertiesNode = jsonSchema.jsonObject["properties"]?.jsonObject
            val propertiesMap = mutableMapOf<String, Schema<Any>>()

            propertiesNode?.forEach { (key, valueNode) ->
                val obj = valueNode.toString().toJsonElement()
                propertiesMap[key] = convertJsonSchemaToOpenApi(obj)
            }
            openApiSchema.properties = propertiesMap

            val requiredNode = jsonSchema.jsonObject["required"]?.jsonArray
            requiredNode?.let { openApiSchema.required = it.map { property -> property.jsonPrimitive.content } }
        } else if (type == "array") {
            val itemsNode = jsonSchema.jsonObject["items"]
            if (itemsNode != null) {
                val arraySchema = ArraySchema()
                arraySchema.items = convertJsonSchemaToOpenApi(itemsNode)
                return arraySchema
            }
        }

        jsonSchema.jsonObject["format"]?.jsonPrimitive?.content?.let { format ->
            openApiSchema.format = format
        }

        return openApiSchema
    }

    /**
     * Recursively converts object properties to their correct data types based on the schema.
     * If the cast is not possible the original property value is kept.
     *
     * @param jsonPath The current JSON path.
     * @param jsonObject The JSON object that is being transformed.
     * @param schemaProperties The schema properties defining the expected data types.
     *
     * @return A JSON object with correct property values.
     * @throws ValidationException If the schema is invalid.
     */
    private fun convertTypesInJson(
        jsonPath: String,
        jsonObject: JsonObject,
        schemaProperties: JsonObject,
    ): JsonObject {
        val updatedJson = mutableMapOf<String, JsonElement>()
        jsonObject.forEach { key, value ->
            val property =
                schemaProperties[key]?.jsonObject
                    ?: throw ValidationException("$jsonPath: missing property $key in schema")
            val expectedType =
                property["type"]
                    ?.jsonPrimitive
                    ?.content ?: throw ValidationException("$jsonPath: missing type field")
            updatedJson[key] = getNewPropertyValue("$jsonPath/$key", expectedType, value, property)
        }
        return JsonObject(updatedJson)
    }

    /**
     * Converts a JSON property value to the expected type based on the schema.
     *
     * @param path The current JSON path
     * @param expectedType The expected type of the value.
     * @param property The JSON property to be converted.
     * @param schema The property JSON schema.
     *
     * @return The converted JSON element.
     */
    private fun getNewPropertyValue(
        path: String,
        expectedType: String,
        property: JsonElement,
        schema: JsonObject,
    ): JsonElement =
        when (expectedType) {
            "integer" -> property.jsonPrimitive.intOrNull?.let { JsonPrimitive(it) } ?: property
            "number" -> property.jsonPrimitive.doubleOrNull?.let { JsonPrimitive(it) } ?: property
            "boolean" -> property.jsonPrimitive.booleanOrNull?.let { JsonPrimitive(it) } ?: property
            "object" ->
                if (property is JsonObject) {
                    val properties =
                        schema["properties"]?.jsonObject
                            ?: throw ValidationException("$path: missing properties")
                    convertTypesInJson(path, property, properties)
                } else {
                    property
                }

            "array" -> {
                val items = schema["items"]?.jsonObject ?: throw ValidationException("$path: missing items field")
                val type =
                    items["type"]?.jsonPrimitive?.content
                        ?: throw ValidationException("$path: missing items type field")
                if (property is JsonArray) {
                    JsonArray(
                        property.mapIndexed { index, element ->
                            getNewPropertyValue("$path[$index]/", type, element, items)
                        },
                    )
                } else {
                    JsonArray(
                        listOf(getNewPropertyValue("$path[0]/", type, property, items)),
                    )
                }
            }

            else -> property
        }

    /**
     * Validates if the schema version is supported. The currently supported version is Draft 2020-12.
     *
     * @throws ValidationException if the version is not supported
     * @throws ParsingException if the
     */
    private fun validateSchemaVersion(collectionSchema: JsonObject) {
        val schemaVersion =
            collectionSchema["\$schema"]?.takeIf { it is JsonPrimitive }?.jsonPrimitive?.content
                ?: throw ParsingException("Invalid or missing schema version (must be a JSON primitive)")
        if (schemaVersion != specVersionUrl) {
            throw ValidationException(
                "Unsupported schema version. The supported version is $specVersionUrl",
            )
        }
    }
}
