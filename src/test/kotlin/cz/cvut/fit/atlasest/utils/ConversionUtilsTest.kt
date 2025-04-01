package cz.cvut.fit.atlasest.utils

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import cz.cvut.fit.atlasest.testData.TestData
import io.ktor.server.plugins.BadRequestException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class ConversionUtilsTest {
    private val testData = TestData()
    private val xmlMapper = XmlMapper()

    @Test
    fun `JsonElement toXML - when given valid JSON object - should return corresponding XML string`() {
        val json = testData.generateComplexJsonObject()
        val jsonElement = json.toString().toJsonElement()
        val actualXmlString = jsonElement.toXML()

        compareXmlStrings(testData.complexXmlString, actualXmlString)
    }

    @Test
    fun `JsonElement toXML - when given valid JSON array with JSON objects - should return corresponding XML string`() {
        val json = testData.generateComplexJsonObject()
        val jsonArray = JsonArray(listOf(json))
        val jsonElement = jsonArray.toString().toJsonElement()
        val expectedXmlItemsString = "<items>${testData.complexXmlString}</items>"

        val actualXmlString = jsonElement.toXML()

        compareXmlStrings(expectedXmlItemsString, actualXmlString)
    }

    @Test
    fun `JsonElement toXML - when given JSON array with JSON primitive value - should throw BadRequestException`() {
        val json = "[${JsonPrimitive("string")}]".toJsonElement()

        val exception = assertThrows<BadRequestException> { json.toXML() }

        assertEquals("Invalid JSON body, should either be object or array of object", exception.message)
    }

    @Test
    fun `JsonElement toXML - when given JsonPrimitive - should throw BadRequestException`() {
        val jsonPrimitive = JsonPrimitive("string")

        val exception = assertThrows<BadRequestException> { jsonPrimitive.toXML() }

        assertEquals("Invalid JSON body, should either be object or array of object", exception.message)
    }

    @Test
    fun `xmlToJson - when given XML with complex structures - should return corresponding JSON string`() {
        val expectedJson = testData.generateComplexJsonObject(strings = true, flattenArrayWithOneItem = true)

        val actualJson = xmlToJson(testData.complexXmlString)

        assertEquals(expectedJson, actualJson)
    }

    @Test
    fun `csvToJsonTest - when given CSV with complex structures - should return corresponding JSON string`() {
        val expectedJson = testData.generateComplexJsonObject(strings = true)

        val actualJson = csvToJson(testData.csvData)

        assertEquals(expectedJson, actualJson)
    }

    @Test
    fun `jsonToCsv - when given JSON object with complex structures - should return corresponding CSV string`() {
        val json = testData.generateComplexJsonObject()

        val actualCsv = jsonToCsv(json)

        assertEquals(testData.csvData, actualCsv)
    }

    @Test
    fun `jsonToCsv - when given JSON array with complex structures - should return corresponding CSV string`() {
        val json = testData.generateComplexJsonObject()
        val jsonWith2Items = JsonArray(listOf(json, json))
        val csvWith2Rows =
            csvWriter {
                this.delimiter = ';'
            }.writeAllAsString(listOf(testData.csvHeader, testData.csvRow, testData.csvRow))

        val result = jsonToCsv(jsonWith2Items)

        assertEquals(csvWith2Rows, result)
    }

    @Test
    fun `jsonToCsv - when given JSON primitive - should throw BadRequestException`() {
        val exception =
            assertThrows<BadRequestException> {
                jsonToCsv(JsonPrimitive("string"))
            }
        assertEquals("Invalid request body. It should be JSON object or array.", exception.message)
    }

    private fun compareXmlStrings(
        expected: String,
        actual: String,
    ) {
        assertEquals(
            xmlMapper.readValue(expected, Map::class.java),
            xmlMapper.readValue(actual, Map::class.java),
        )
    }
}
