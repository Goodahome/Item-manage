package com.example.itemremindertool.utils

fun formatQuantityWithUnit(quantity: Int, unit: String?): String {
    val trimmedUnit = unit?.trim().orEmpty()
    return if (trimmedUnit.isEmpty()) {
        quantity.toString()
    } else {
        "$quantity $trimmedUnit"
    }
}
