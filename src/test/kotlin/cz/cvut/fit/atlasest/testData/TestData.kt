package cz.cvut.fit.atlasest.testData

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class TestData {
    val csvHeader =
        listOf(
            "name",
            "age",
            "gpa",
            "student",
            "birthday.year",
            "birthday.month",
            "birthday.day",
            "birthday.place",
            "classmates[0]",
            "classmates[1]",
            "teachers[0]",
            "schools[0].name",
            "subjects[0].name",
            "subjects[0].code",
            "subjects[0].grade",
            "subjects[0].passed",
            "subjects[1].name",
            "subjects[1].code",
            "subjects[1].grade",
            "subjects[1].passed",
        )

    val csvRow =
        listOf(
            "Alice",
            "25",
            "1.0",
            "true",
            "2000",
            "1",
            "2",
            "Prague",
            "Bob",
            "Charles",
            "David",
            "CVUT",
            "subject1",
            "1",
            "1.0",
            "true",
            "subject2",
            "2",
            "2.0",
            "true",
        )

    val csvData =
        csvWriter {
            this.delimiter = ';'
        }.writeAllAsString(listOf(csvHeader, csvRow))

    val complexXmlString =
        """
        <item>
            <name>Alice</name>
            <age>25</age>
            <gpa>1.0</gpa>
            <student>true</student>
            <birthday>
                <year>2000</year>
                <month>1</month>
                <day>2</day>
                <place>Prague</place>
            </birthday>
            <classmates>Bob</classmates>
            <classmates>Charles</classmates>
            <teachers>David</teachers>
            <schools>
                <name>CVUT</name>
            </schools>
            <subjects>
                <name>subject1</name>
                <code>1</code>
                <grade>1.0</grade>
                <passed>true</passed>
            </subjects>
            <subjects>
                <name>subject2</name>
                <code>2</code>
                <grade>2.0</grade>
                <passed>true</passed>
            </subjects>
        </item>
        """.trimIndent()

    fun generateSimpleJsonObject(
        simpleProp1: JsonPrimitive? = JsonPrimitive("a"),
        simpleProp2: JsonPrimitive? = JsonPrimitive("b"),
        objectProp: JsonObject? =
            JsonObject(
                mapOf(
                    "prop" to JsonPrimitive("value"),
                ),
            ),
        arrayProp: JsonArray? =
            JsonArray(
                listOf(
                    JsonPrimitive("value"),
                ),
            ),
    ): JsonObject =
        JsonObject(
            buildMap {
                simpleProp1?.let { put("simpleProperty1", it) }
                simpleProp2?.let { put("simpleProperty2", it) }
                objectProp?.let { put("objectProperty", it) }
                arrayProp?.let { put("arrayProperty", it) }
            },
        )

    fun generateComplexJsonObject(
        strings: Boolean = false,
        flattenArrayWithOneItem: Boolean = false,
        name: JsonPrimitive? = JsonPrimitive("Alice"),
        age: JsonPrimitive? = if (strings) JsonPrimitive("25") else JsonPrimitive(25),
        gpa: JsonPrimitive? = if (strings) JsonPrimitive("1.0") else JsonPrimitive(1.0),
        student: JsonPrimitive? = if (strings) JsonPrimitive("true") else JsonPrimitive(true),
        birthday: JsonObject? =
            JsonObject(
                mapOf(
                    "year" to if (strings) JsonPrimitive("2000") else JsonPrimitive(2000),
                    "month" to if (strings) JsonPrimitive("1") else JsonPrimitive(1),
                    "day" to if (strings) JsonPrimitive("2") else JsonPrimitive(2),
                    "place" to JsonPrimitive("Prague"),
                ),
            ),
        classmates: JsonArray? =
            JsonArray(
                listOf(
                    JsonPrimitive("Bob"),
                    JsonPrimitive("Charles"),
                ),
            ),
        teachers: JsonElement? =
            if (flattenArrayWithOneItem) {
                JsonPrimitive("David")
            } else {
                JsonArray(
                    listOf(JsonPrimitive("David")),
                )
            },
        schools: JsonElement? =
            if (flattenArrayWithOneItem) {
                JsonObject(
                    mapOf(
                        "name" to JsonPrimitive("CVUT"),
                    ),
                )
            } else {
                JsonArray(
                    listOf(
                        JsonObject(
                            mapOf(
                                "name" to JsonPrimitive("CVUT"),
                            ),
                        ),
                    ),
                )
            },
        subjects: JsonArray? =
            JsonArray(
                listOf(
                    JsonObject(
                        mapOf(
                            "name" to JsonPrimitive("subject1"),
                            "code" to if (strings) JsonPrimitive("1") else JsonPrimitive(1),
                            "grade" to if (strings) JsonPrimitive("1.0") else JsonPrimitive(1.0),
                            "passed" to if (strings) JsonPrimitive("true") else JsonPrimitive(true),
                        ),
                    ),
                    JsonObject(
                        mapOf(
                            "name" to JsonPrimitive("subject2"),
                            "code" to if (strings) JsonPrimitive("2") else JsonPrimitive(2),
                            "grade" to if (strings) JsonPrimitive("2.0") else JsonPrimitive(2.0),
                            "passed" to if (strings) JsonPrimitive("true") else JsonPrimitive(true),
                        ),
                    ),
                ),
            ),
    ): JsonObject =
        JsonObject(
            buildMap {
                name?.let { put("name", it) }
                age?.let { put("age", it) }
                gpa?.let { put("gpa", it) }
                student?.let { put("student", it) }
                birthday?.let { put("birthday", it) }
                classmates?.let { put("classmates", it) }
                teachers?.let { put("teachers", it) }
                schools?.let { put("schools", it) }
                subjects?.let { put("subjects", it) }
            },
        )

    private val simpleProperty1 =
        JsonObject(
            mapOf(
                "type" to JsonPrimitive("string"),
            ),
        )

    private val simpleProperty2 =
        JsonObject(
            mapOf(
                "type" to JsonPrimitive("integer"),
            ),
        )

    private val objectProperty =
        JsonObject(
            mapOf(
                "type" to JsonPrimitive("object"),
                "properties" to
                    JsonObject(
                        mapOf(
                            "prop" to
                                JsonObject(
                                    mapOf(
                                        "type" to JsonPrimitive("string"),
                                    ),
                                ),
                        ),
                    ),
                "required" to
                    JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                        ),
                    ),
            ),
        )

    private val arrayProperty =
        JsonObject(
            mapOf(
                "type" to JsonPrimitive("array"),
                "items" to
                    JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                        ),
                    ),
            ),
        )

    fun generateSimpleSchema(
        simpleProp1: JsonObject? = simpleProperty1,
        simpleProp2: JsonObject? = simpleProperty2,
        objectProp: JsonObject? = objectProperty,
        arrayProp: JsonObject? = arrayProperty,
        schemaVersion: String? = "https://json-schema.org/draft/2020-12/schema#",
    ): JsonObject =
        JsonObject(
            buildMap {
                schemaVersion?.let { put("\$schema", JsonPrimitive(it)) }
                put("type", JsonPrimitive("object"))
                put(
                    "properties",
                    JsonObject(
                        buildMap {
                            simpleProp1?.let { put("simpleProperty1", simpleProp1) }
                            simpleProp2?.let { put("simpleProperty2", simpleProp2) }
                            objectProp?.let { put("objectProperty", objectProp) }
                            arrayProp?.let { put("arrayProperty", arrayProp) }
                        },
                    ),
                )
                put(
                    "required",
                    JsonArray(
                        listOf(
                            JsonPrimitive("simpleProperty1"),
                            JsonPrimitive("simpleProperty2"),
                            JsonPrimitive("objectProperty"),
                            JsonPrimitive("arrayProperty"),
                        ),
                    ),
                )
            },
        )

    val complexSchema =
        """
        {
          "${"$"}schema": "https://json-schema.org/draft/2020-12/schema#",
          "type": "object",
          "properties": {
            "name": {
              "type": "string"
            },
            "age": {
              "type": "integer"
            },
            "gpa": {
              "type": "number"
            },
            "student": {
              "type": "boolean"
            },
            "birthday": {
              "type": "object",
              "properties": {
                "year": {
                  "type": "integer"
                },
                "month": {
                  "type": "integer"
                },
                "day": {
                  "type": "integer"
                },
                "place": {
                  "type": "string"
                }
              },
              "additionalProperties": false,
              "required": [
                "year",
                "month",
                "day",
                "place"
              ]
            },
            "classmates": {
              "type": "array",
              "items": {
                "type": "string"
              }
            },
            "teachers": {
              "type": "array",
              "items": {
                "type": "string"
              }
            },
            "schools": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "name": {
                    "type": "string"
                  }
                },
                "additionalProperties": false,
                "required": [
                  "name"
                ]
              }
            },
            "subjects": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "name": {
                    "type": "string"
                  },
                  "code": {
                    "type": "integer"
                  },
                  "grade": {
                    "type": "number"
                  },
                  "passed": {
                    "type": "boolean"
                  }
                },
                "additionalProperties": false,
                "required": [
                  "name",
                  "code",
                  "grade",
                  "passed"
                ]
              }
            }
          },
          "additionalProperties": false,
          "required": [
            "name",
            "age",
            "gpa",
            "student",
            "birthday",
            "classmates",
            "teachers",
            "schools",
            "subjects"
          ]
        }
        """.trimIndent()

    val schemaWithIdAsNumber =
        """
        {
          "books": {
            "${"$"}schema": "https://json-schema.org/draft/2020-12/schema#",
            "type": "object",
            "properties": {
              "id": {
                "type": "number"
              }
            },
            "additionalProperties": false,
            "required": [
              "id"
            ]
          }
        }
        """.trimIndent()
}
