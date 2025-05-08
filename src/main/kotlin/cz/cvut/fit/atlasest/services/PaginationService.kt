package cz.cvut.fit.atlasest.services

import io.ktor.server.plugins.BadRequestException
import kotlinx.serialization.json.JsonObject
import kotlin.math.min

/**
 * A service for handling pagination of JSON object collections.
 */
class PaginationService(
    val defaultLimit: Int,
) {
    /**
     * Applies pagination to a collection of JSON objects based on the provided page and limit parameters.
     *
     * @param collectionItems The list of JSON objects to be paginated.
     * @param page The page number (indexed from 1).
     * @param limit The number of items per page. If `null`, the default limit is used.
     * @param paramsString The query parameters separated by `&`, to include in the pagination links.
     * @param baseUrl The base URL.
     *
     * @return A pair containing the paginated list of JSON objects and an optional string of pagination links.
     * @throws BadRequestException If the `_limit` parameter is provided without `_page`.
     */
    internal fun applyPagination(
        collectionItems: MutableList<JsonObject>,
        page: Int?,
        limit: Int?,
        paramsString: String,
        baseUrl: String,
    ): Pair<MutableList<JsonObject>, String?> {
        if (page == null) {
            if (limit == null) {
                return collectionItems to null
            } else {
                throw BadRequestException("Pagination parameter $LIMIT is without $PAGE")
            }
        }
        val links =
            createPaginationLinks(
                baseUrl,
                page,
                limit ?: defaultLimit,
                collectionItems.size,
                paramsString,
            )

        val newLimit = limit ?: defaultLimit
        val start = (page - 1) * newLimit
        val end = min(start + newLimit, collectionItems.size)

        return if (start < collectionItems.size) {
            collectionItems.subList(start, end).toMutableList() to links
        } else {
            mutableListOf<JsonObject>() to links
        }
    }

    /**
     * Generates a string of pagination links.
     *
     * This function builds navigation links for paginated results, such as "first", "prev", "next", and "last",
     * depending on the current page and total number of pages. The links are returned as a comma-separated string
     * suitable for use in the HTTP header `Link`.
     *
     * @param baseUrl The base URL to which pagination parameters will be appended.
     * @param page The current page number (1-based).
     * @param limit The number of items per page.
     * @param totalItems The total number of items across all pages.
     * @param params A string of additional query parameters. Should not include `page` or `limit`.
     *
     * @return A string containing pagination links or an empty string if no links are applicable.
     */
    private fun createPaginationLinks(
        baseUrl: String,
        page: Int,
        limit: Int,
        totalItems: Int,
        params: String,
    ): String {
        val totalPages = (totalItems + limit - 1) / limit
        val links = mutableListOf<String>()

        fun buildLink(
            page: Int,
            rel: String,
        ) {
            val link =
                if (params != "") {
                    "<$baseUrl?$params&$PAGE=$page&$LIMIT=$limit>; rel=\"$rel\""
                } else {
                    "<$baseUrl?$PAGE=$page&$LIMIT=$limit>; rel=\"$rel\""
                }
            links.add(link)
        }

        if (page > 1) {
            buildLink(1, "first")
            buildLink(page - 1, "prev")
        }

        if (page < totalPages) {
            buildLink(page + 1, "next")
            buildLink(totalPages, "last")
        }

        return links.joinToString(", ")
    }
}
