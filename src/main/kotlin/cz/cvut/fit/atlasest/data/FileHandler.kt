package cz.cvut.fit.atlasest.data

import cz.cvut.fit.atlasest.exceptionHandling.InvalidDataException
import cz.cvut.fit.atlasest.utils.toJsonObject
import kotlinx.io.files.FileNotFoundException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.io.File

/**
 * A service for reading and writing JSON files
 */
class FileHandler {
    private val json = Json { prettyPrint = true }

    /**
     * Reads the specified JSON file and parses its contents into a JSON object
     *
     * @param filename The name of the file to read
     *
     * @return The parsed [JsonObject] from the JSON file
     *
     * @throws InvalidDataException If there is an issue parsing the data
     * @throws NullPointerException If the file cannot be found
     */
    fun readJsonFile(filename: String): JsonObject {
        try {
            val content =
                File(filename).readText().toJsonObject()
            return content
        } catch (e: SerializationException) {
            throw InvalidDataException("Failed to parse JSON from $filename", e)
        } catch (e: FileNotFoundException) {
            throw FileNotFoundException("File not found: $filename")
        }
    }

    /**
     * Saves a JSON object to a specified JSON file
     *
     * @param filename The name of the file to save the JSON object
     * @param data The saved JSON object
     */
    fun saveJsonFile(
        filename: String,
        data: JsonObject,
    ) {
        File(filename).writeText(json.encodeToString(data))
    }
}
