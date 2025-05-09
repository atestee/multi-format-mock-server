package cz.cvut.fit.atlasest.services

/**
 * Represents all filter operators
 */
enum class FilterOperator {
    EQ,
    NE,
    LT,
    GT,
    LTE,
    GTE,
    LIKE,
}

/**
 * Represents filter operators available for JSON types `number`,
 * `integer`, `date`, and `date-time`
 */
enum class FilterOperatorNumeric {
    LT,
    GT,
    LTE,
    GTE,
}
