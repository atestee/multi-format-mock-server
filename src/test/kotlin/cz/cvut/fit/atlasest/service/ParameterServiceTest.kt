package cz.cvut.fit.atlasest.service

import BaseTest
import cz.cvut.fit.atlasest.utils.getFieldValue
import io.ktor.server.plugins.BadRequestException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class ParameterServiceTest : BaseTest() {
    private val parameterService by inject<ParameterService>()
    private val documentService by inject<DocumentService>()

    private val books =
        documentService
            .readJsonFile("db-test.json")["books"]!!
            .jsonArray
            .map { it.jsonObject }
            .toMutableList()

    @Test
    fun `applyFilter - EQ with nested object key`() {
        val result = parameterService.applyFilter(books, mapOf("published.place" to listOf("USA")))

        val titles = result.map { it["title"]!!.jsonPrimitive.content }

        assertEquals(4, result.size)
        assertContains(titles, "The Catcher in the Rye")
        assertContains(titles, "To Kill a Mockingbird")
        assertContains(titles, "Moby Dick")
        assertContains(titles, "The Great Gatsby")
    }

    @Test
    fun `applyFilter - EQ with author key`() {
        val result = parameterService.applyFilter(books, mapOf("author" to listOf("Jane Austen")))

        val titles = result.map { it["title"]!!.jsonPrimitive.content }

        assertEquals(2, result.size)
        assertContains(titles, "Pride and Prejudice")
        assertContains(titles, "Sense and Sensibility")
    }

    @Test
    fun `applyFilter - EQ with array key`() {
        val result = parameterService.applyFilter(books, mapOf("genre" to listOf("Dystopian")))

        val titles = result.map { it["title"]!!.jsonPrimitive.content }

        assertEquals(2, result.size)
        assertContains(titles, "1984")
        assertContains(titles, "Brave New World")
    }

    @Test
    fun `applyFilter - EQ with array key and value`() {
        val result = parameterService.applyFilter(books, mapOf("genre" to listOf("Fiction", "Tragedy")))

        val titles = result.map { it["title"]!!.jsonPrimitive.content }

        assertEquals(2, result.size)
        assertContains(titles, "The Great Gatsby")
        assertContains(titles, "Les Miserables")
    }

    @Test
    fun `applyFilter - EQ with published year`() {
        val result = parameterService.applyFilter(books, mapOf("published.year" to listOf("1925")))

        val titles = result.map { it["title"]!!.jsonPrimitive.content }

        assertEquals(1, result.size)
        assertContains(titles, "The Great Gatsby")
    }

    @Test
    fun `applyFilter - EQ with array values matched with index`() {
        val result = parameterService.applyFilter(books, mapOf("genre[0]" to listOf("Science Fiction")))

        val titles = result.map { it["title"]!!.jsonPrimitive.content }

        assertEquals(1, result.size)
        assertContains(titles, "Brave New World")
    }

    @Test
    fun `applyFilter - NE with nested object`() {
        val result = parameterService.applyFilter(books, mapOf("published.place_ne" to listOf("USA")))

        val titles = result.map { it["title"]!!.jsonPrimitive.content }

        assertEquals(6, result.size)
        assertContains(titles, "1984")
        assertContains(titles, "Pride and Prejudice")
        assertContains(titles, "Sense and Sensibility")
        assertContains(titles, "Les Miserables")
        assertContains(titles, "Brave New World")
        assertContains(titles, "Frankenstein")
    }

    @Test
    fun `applyFilter - NE with author property`() {
        val result = parameterService.applyFilter(books, mapOf("author_ne" to listOf("Jane Austen")))

        val titles = result.map { it["title"]!!.jsonPrimitive.content }

        assertEquals(8, result.size)
        assertFalse(titles.contains("Pride and Prejudice"))
        assertFalse(titles.contains("Sense and Sensibility"))
    }

    @Test
    fun `applyFilter - NE with array`() {
        val result = parameterService.applyFilter(books, mapOf("genre_ne" to listOf("Dystopian")))

        val titles = result.map { it["title"]!!.jsonPrimitive.content }

        assertEquals(8, result.size)
        assertFalse(titles.contains("1984"))
        assertFalse(titles.contains("Brave New World"))
    }

    @Test
    fun `applyFilter - LT with published year`() {
        val result = parameterService.applyFilter(books, mapOf("published.year_lt" to listOf("1925")))

        val titles = result.map { it["title"]!!.jsonPrimitive.content }

        assertEquals(5, result.size)
        assertContains(titles, "Moby Dick")
        assertContains(titles, "Pride and Prejudice")
        assertContains(titles, "Sense and Sensibility")
        assertContains(titles, "Les Miserables")
        assertContains(titles, "Frankenstein")
    }

    @Test
    fun `applyFilter - LTE with published year`() {
        val result = parameterService.applyFilter(books, mapOf("published.year_lte" to listOf("1925")))

        val titles = result.map { it["title"]!!.jsonPrimitive.content }

        assertEquals(6, result.size)
        assertContains(titles, "The Great Gatsby")
        assertContains(titles, "Moby Dick")
        assertContains(titles, "Pride and Prejudice")
        assertContains(titles, "Sense and Sensibility")
        assertContains(titles, "Les Miserables")
        assertContains(titles, "Frankenstein")
    }

    @Test
    fun `applyFilter - GT with published year`() {
        val result = parameterService.applyFilter(books, mapOf("published.year_gt" to listOf("1925")))

        val titles = result.map { it["title"]!!.jsonPrimitive.content }

        assertEquals(4, result.size)
        assertContains(titles, "The Catcher in the Rye")
        assertContains(titles, "To Kill a Mockingbird")
        assertContains(titles, "1984")
        assertContains(titles, "Brave New World")
    }

    @Test
    fun `applyFilter - GTE with published year`() {
        val result = parameterService.applyFilter(books, mapOf("published.year_gte" to listOf("1925")))

        val titles = result.map { it["title"]!!.jsonPrimitive.content }

        assertEquals(5, result.size)
        assertContains(titles, "The Great Gatsby")
        assertContains(titles, "The Catcher in the Rye")
        assertContains(titles, "To Kill a Mockingbird")
        assertContains(titles, "1984")
        assertContains(titles, "Brave New World")
    }

    @Test
    fun `applyFilter - LIKE with genre array`() {
        val result = parameterService.applyFilter(books, mapOf("genre_like" to listOf("Fiction")))

        val titles = result.map { it["title"]!!.jsonPrimitive.content }

        assertEquals(7, result.size)
        assertContains(titles, "The Catcher in the Rye")
        assertContains(titles, "1984")
        assertContains(titles, "Moby Dick")
        assertContains(titles, "The Great Gatsby")
        assertContains(titles, "Les Miserables")
        assertContains(titles, "Brave New World")
        assertContains(titles, "Frankenstein")
    }

    @Test
    fun `applyFilter - LIKE with published year`() {
        val result = parameterService.applyFilter(books, mapOf("published.year_like" to listOf("19")))

        val titles = result.map { it.jsonObject["title"]!!.jsonPrimitive.content }

        assertEquals(5, result.size)
        assertContains(titles, "The Catcher in the Rye")
        assertContains(titles, "To Kill a Mockingbird")
        assertContains(titles, "1984")
        assertContains(titles, "The Great Gatsby")
        assertContains(titles, "Brave New World")
    }

    @Test
    fun `applyFilter - LIKE with array values matched with index`() {
        val result = parameterService.applyFilter(books, mapOf("genre[0]_like" to listOf("Science")))

        val titles = result.map { it["title"]!!.jsonPrimitive.content }

        assertEquals(1, result.size)
        assertContains(titles, "Brave New World")
    }

    @Test
    fun `applyPagination - when page=2 limit=3 - should return items from id=3 to id=5`() {
        val page = "2"
        val limit = "3"
        val result =
            parameterService.applyPagination(
                collectionItems = books,
                mapOf(
                    "_page" to listOf(page),
                    "_limit" to listOf(limit),
                ),
            )

        val ids = result.map { it["id"]!!.jsonPrimitive.content }

        assertEquals(limit.toInt(), result.size)
        assertEquals("4", ids.first())
        assertEquals("6", ids.last())
    }

    @Test
    fun `applyPagination - when page=2 limit=null - should return empty list`() {
        val page = "2"
        val result =
            parameterService.applyPagination(
                collectionItems = books,
                mapOf(
                    "_page" to listOf(page),
                ),
            )

        assertEquals(0, result.size)
    }

    @Test
    fun `applyPagination - when page=null limit=null - should return all items`() {
        val result =
            parameterService.applyPagination(
                collectionItems = books,
                mapOf(),
            )

        assertEquals(10, result.size)
    }

    @Test
    fun `applyPagination - when page=null limit=2 - should throw BadRequestException`() {
        val exception =
            assertFailsWith<BadRequestException> {
                parameterService.applyPagination(
                    collectionItems = books,
                    mapOf(
                        "_limit" to listOf("2"),
                    ),
                )
            }
        assertEquals("Pagination parameter _limit is without _page", exception.message)
    }

    @Test
    fun `applySorting - when sort=produced year order=null - should sort asc by produced year`() {
        val publishedYearKey = "published.year"
        val result =
            parameterService.applySorting(
                books,
                mapOf(
                    "_sort" to listOf(publishedYearKey),
                ),
            )

        val years = result.map { it.getFieldValue(publishedYearKey)!!.jsonPrimitive.content }

        assertEquals("1811", years.first())
        assertEquals("1960", years.last())
    }

    @Test
    fun `applySorting - when sort=produced year order=desc - should sort desc by produced year`() {
        val publishedYearKey = "published.year"
        val result =
            parameterService.applySorting(
                books,
                mapOf(
                    "_sort" to listOf(publishedYearKey),
                    "_order" to listOf("desc"),
                ),
            )

        val years = result.map { it.getFieldValue(publishedYearKey)!!.jsonPrimitive.content }

        assertEquals("1960", years.first())
        assertEquals("1811", years.last())
    }

    @Test
    fun `applySorting - when sort=author order=asc - should sort asc by author`() {
        val authorKey = "author"
        val result =
            parameterService.applySorting(
                books,
                mapOf(
                    "_sort" to listOf(authorKey),
                    "_order" to listOf("asc"),
                ),
            )

        val authors = result.map { it.getFieldValue(authorKey)!!.jsonPrimitive.content }

        assertEquals("Aldous Huxley", authors.first())
        assertEquals("Victor Hugo", authors.last())
    }

    @Test
    fun `applySorting - when sort=null order=null - should return item unsorted`() {
        val result =
            parameterService.applySorting(
                books,
                mapOf(),
            )

        val authors = result.map { it.getFieldValue("id")!!.jsonPrimitive.content }

        assertEquals("1", authors.first())
        assertEquals("10", authors.last())
    }

    @Test
    fun `applySorting - when sort=author order is invalid - should throw BadRequestException`() {
        val exception =
            assertFailsWith<BadRequestException> {
                parameterService.applySorting(
                    books,
                    mapOf(
                        "_sort" to listOf("author"),
                        "_order" to listOf("wrong"),
                    ),
                )
            }
        assertEquals("Invalid sorting parameter $ORDER (must be 'asc' or 'desc')", exception.message)
    }

    @Test
    fun `applySorting - when sorting key is invalid - should throw BadRequestException`() {
        val invalidKey = "wrongKey"
        val exception =
            assertFailsWith<BadRequestException> {
                parameterService.applySorting(
                    books,
                    mapOf(
                        "_sort" to listOf(invalidKey),
                    ),
                )
            }
        assertEquals("Value of '$invalidKey' is not present", exception.message)
    }

    @Test
    fun `applySorting - when sort=null and order=asc - should throw BadRequestException`() {
        val exception =
            assertFailsWith<BadRequestException> {
                parameterService.applySorting(
                    books,
                    mapOf(
                        "_order" to listOf("asc"),
                    ),
                )
            }
        assertEquals("Sorting parameter $ORDER is without $SORT", exception.message)
    }

    @Test
    fun `applyEmbedAndExpand on collection - when given 2 embed values - returns the collection with embedded items from both collections`() {
        val result =
            parameterService.applyEmbedAndExpand(
                mapOf("_embed" to listOf("loans", "libraryBooks")),
                "books",
                collectionService,
            )

        val firstBookLoans = result.first()["loans"]
        val loan = collectionService.getItemById("loans", "1")

        val firstBooklibraryBooks = result.first()["libraryBooks"]
        val secondBooklibraryBooks = result[1]["libraryBooks"]
        val ownership1 = collectionService.getItemById("libraryBooks", "1")
        val ownership2 = collectionService.getItemById("libraryBooks", "2")
        val ownership3 = collectionService.getItemById("libraryBooks", "3")

        assertEquals(JsonArray(listOf(loan)), firstBookLoans)
        assertEquals(JsonArray(listOf(ownership1, ownership2)), firstBooklibraryBooks)
        assertEquals(JsonArray(listOf(ownership3)), secondBooklibraryBooks)
    }

    @Test
    fun `applyEmbedAndExpand on item - when given 2 embed values - returns the item with embedded items from both collections`() {
        val result =
            parameterService.applyEmbedAndExpand(
                mapOf("_embed" to listOf("loans", "libraryBooks")),
                "books",
                "1",
                collectionService,
            )

        val loan = collectionService.getItemById("loans", "1")
        val ownership1 = collectionService.getItemById("libraryBooks", "1")
        val ownership2 = collectionService.getItemById("libraryBooks", "2")
        assertEquals(JsonArray(listOf(loan)), result["loans"])
        assertEquals(JsonArray(listOf(ownership1, ownership2)), result["libraryBooks"])
    }

    @Test
    fun `applyEmbedAndExpand on collection - when given 2 expand values - returns the collection with items expanded`() {
        val result =
            parameterService.applyEmbedAndExpand(
                mapOf("_expand" to listOf("book", "user")),
                "loans",
                collectionService,
            )

        val book = collectionService.getItemById("books", "1")
        val user = collectionService.getItemById("users", "1")
        assertEquals(book, result.first()["book"])
        assertEquals(user, result.first()["user"])
    }

    @Test
    fun `applyEmbedAndExpand on item - when given 2 expand values - returns the item expanded`() {
        val result =
            parameterService.applyEmbedAndExpand(
                mapOf("_expand" to listOf("book", "user")),
                "loans",
                "1",
                collectionService,
            )

        val book = collectionService.getItemById("books", "1")
        val user = collectionService.getItemById("users", "1")
        assertEquals(book, result["book"])
        assertEquals(user, result["user"])
    }

    @Test
    fun `applyEmbedAndExpand on item - when given embed and expand values - returns the item embedded and expanded`() {
        val result =
            parameterService.applyEmbedAndExpand(
                mapOf(
                    "_expand" to listOf("libraryRegistration"),
                    "_embed" to listOf("loans"),
                ),
                "users",
                "1",
                collectionService,
            )

        val libraryRegistration = collectionService.getItemById("libraryRegistrations", "1")
        val loan = collectionService.getItemById("loans", "1")
        assertEquals(libraryRegistration, result["libraryRegistration"])
        assertEquals(JsonArray(listOf(loan)), result["loans"])
    }
}
