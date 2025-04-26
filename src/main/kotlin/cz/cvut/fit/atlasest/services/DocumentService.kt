package cz.cvut.fit.atlasest.services

import cz.cvut.fit.atlasest.exceptionHandling.ParsingException
import cz.cvut.fit.atlasest.utils.toJsonObject
import kotlinx.io.files.FileNotFoundException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.io.File

/**
 * A service for reading and writing JSON documents.
 */
class DocumentService(
    private val isTest: Boolean,
) {
    private val json = Json { prettyPrint = true }

    /**
     * Reads the specified JSON file and parses its contents into a JSON object.
     *
     * @param fileName The name of the file to read.
     *
     * @return The parsed [JsonObject] from the JSON file.
     *
     * @throws ParsingException If there is an issue parsing the data.
     * @throws NullPointerException If the file cannot be found.
     */
    fun readJsonFile(fileName: String): JsonObject {
        try {
            val content =
                if (isTest) {
                    (
                        this.javaClass.classLoader
                            .getResource(fileName)
                            ?.readText() as String
                    ).toJsonObject()
                } else {
                    File(fileName).readText().toJsonObject()
                }
            return content
        } catch (e: SerializationException) {
            throw ParsingException("Failed to parse JSON from $fileName", e)
        } catch (e: NullPointerException) {
            throw FileNotFoundException("File not found: $fileName")
        }
    }

    /**
     * Saves a JSON object to a specified JSON file.
     *
     * @param fileName The name of the file to save the JSON object.
     * @param data The saved JSON object.
     */
    fun saveJsonFile(
        fileName: String,
        data: JsonObject,
    ) {
        try {
            File(fileName).writeText(json.encodeToString(data))
        } catch (e: SerializationException) {
            throw ParsingException("Failed to save JSON object to $fileName", e)
        } catch (e: FileNotFoundException) {
            throw FileNotFoundException("File not found: $fileName")
        }
    }
}
