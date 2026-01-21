package com.example.itemremindertool.utils

import android.content.Context
import com.example.itemremindertool.R

object CurrencyUtils {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_CURRENCY_SYMBOL = "currency_symbol"

    fun getCurrencySymbol(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultSymbol = context.getString(R.string.default_currency_symbol)
        val symbol = prefs.getString(KEY_CURRENCY_SYMBOL, defaultSymbol)?.trim().orEmpty()
        return if (symbol.isBlank()) defaultSymbol else symbol
    }

    fun formatPrice(context: Context, price: Double): String {
        val symbol = getCurrencySymbol(context)
        return context.getString(R.string.price_with_symbol, symbol, price)
    }
}
