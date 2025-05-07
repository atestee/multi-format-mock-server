package cz.cvut.fit.atlasest.data

import BaseTest
import cz.cvut.fit.atlasest.exceptionHandling.InvalidDataException
import cz.cvut.fit.atlasest.utils.toJsonObject
import kotlinx.io.files.FileNotFoundException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FileHandlerTest : BaseTest() {
    @Test
    fun `readJsonFile - when given file with json object - should return the Json object`() {
        val filename = "test.json"
        val data =
            JsonObject(
                mapOf(
                    "name" to JsonPrimitive("test"),
                ),
            )
        val file = File(filename)
        file.writeText(data.toString())
        file.createNewFile()
        file.deleteOnExit()

        val result = fileHandler.readJsonFile(filename)

        assertEquals(data, result)
    }

    @Test
    fun `readJsonFile - when given text file - should throw InvalidDataException`() {
        val filename = "test.txt"
        val file = File(filename)
        file.writeText("test text")
        file.createNewFile()

        val exception =
            assertFailsWith<InvalidDataException> {
                fileHandler.readJsonFile(filename)
            }

        assertEquals("Failed to parse JSON from $filename", exception.message)

        file.delete()
    }

    @Test
    fun `readJsonFile - when given non-existent file - should throw FileNotFoundException`() {
        val filename = "test.txt"

        val exception =
            assertFailsWith<FileNotFoundException> {
                fileHandler.readJsonFile(filename)
            }

        assertEquals("File not found: $filename", exception.message)
    }

    @Test
    fun `saveJsonFile - when given json object - should write the data to file`() {
        val filename = "test.json"
        val data =
            JsonObject(
                mapOf(
                    "name" to JsonPrimitive("test"),
                ),
            )
        val file = File(filename)
        file.createNewFile()
        file.deleteOnExit()

        assertDoesNotThrow {
            fileHandler.saveJsonFile(filename, data)
        }
        val result = File(filename).readText().toJsonObject()
        assertEquals(data, result)
    }
}
