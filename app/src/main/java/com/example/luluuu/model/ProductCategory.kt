package com.example.luluuu.model

enum class ProductCategory {
    SOAP_BAR,
    DETERGENT_POWDER;

    override fun toString(): String {
        return when (this) {
            SOAP_BAR -> "Soap Bar"
            DETERGENT_POWDER -> "Detergent Powder"
        }
    }

    companion object {
        fun fromString(value: String): ProductCategory {
            return when (value.lowercase()) {
                "soap bar" -> SOAP_BAR
                "detergent powder" -> DETERGENT_POWDER
                else -> throw IllegalArgumentException("Unknown category: $value")
            }
        }
    }
}
