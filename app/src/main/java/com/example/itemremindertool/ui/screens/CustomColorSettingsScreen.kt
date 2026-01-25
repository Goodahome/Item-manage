package com.example.itemremindertool.ui.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.itemremindertool.R
import com.example.itemremindertool.billing.BillingManager
import com.example.itemremindertool.billing.PremiumFeatureManager
import com.example.itemremindertool.config.FeatureFlags
import com.example.itemremindertool.ui.components.PremiumFeatureDialog
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.theme.OnSurfaceHighContrast
import com.example.itemremindertool.ui.theme.OnSurfaceLowContrast
import com.example.itemremindertool.ui.theme.RedBlueBackground
import com.example.itemremindertool.ui.theme.RedBlueGradientEnd
import com.example.itemremindertool.ui.theme.RedBlueGradientStart
import com.example.itemremindertool.ui.theme.RedBluePrimary
import com.example.itemremindertool.ui.theme.RedBluePrimaryContainer
import com.example.itemremindertool.ui.theme.RedBlueOnPrimaryContainer
import com.example.itemremindertool.ui.theme.RedBlueSearchBoxBg
import com.example.itemremindertool.ui.theme.RedBlueSearchBoxBorder
import com.example.itemremindertool.ui.theme.RedBlueSurface
import com.example.itemremindertool.ui.theme.RedBlueSurfaceVariant
import com.example.itemremindertool.ui.theme.RedBlueTertiary
import org.json.JSONObject

data class CustomColorField(val key: String, val label: String, val defaultColor: Color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomColorSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }
    var showPremiumFeatureDialog by remember { mutableStateOf(false) }
    var canAccessPremiumFeatures by remember {
        mutableStateOf(PremiumFeatureManager.canAccessPremiumFeatures(context))
    }
    val billingManager = remember {
        if (FeatureFlags.ENABLE_PURCHASE_FEATURE) {
            BillingManager(
                context,
                listOf(
                    BillingManager.PRODUCT_REMOVE_ADS,
                    BillingManager.PRODUCT_PREMIUM_FEATURES,
                    BillingManager.PRODUCT_PREMIUM_LIFETIME
                )
            ).apply {
                initialize()
            }
        } else {
            null
        }
    }
    val rawColorScheme = prefs.getString("color_scheme", "red_blue") ?: "red_blue"
    val normalizedColorScheme = if (rawColorScheme == "cold_blue") "red_blue" else rawColorScheme
    var customEnabled by remember { mutableStateOf(normalizedColorScheme == "custom") }

    val customFields = listOf(
        CustomColorField("custom_color_primary", stringResource(R.string.custom_color_field_primary), RedBluePrimary),
        CustomColorField("custom_color_tertiary", stringResource(R.string.custom_color_field_tertiary), RedBlueTertiary),
        CustomColorField("custom_color_primary_container", stringResource(R.string.custom_color_field_primary_container), RedBluePrimaryContainer),
        CustomColorField("custom_color_on_primary_container", stringResource(R.string.custom_color_field_on_primary_container), RedBlueOnPrimaryContainer),
        CustomColorField("custom_color_background", stringResource(R.string.custom_color_field_background), RedBlueBackground),
        CustomColorField("custom_color_surface", stringResource(R.string.custom_color_field_surface), RedBlueSurface),
        CustomColorField("custom_color_surface_variant", stringResource(R.string.custom_color_field_surface_variant), RedBlueSurfaceVariant),
        CustomColorField("custom_color_gradient_start", stringResource(R.string.custom_color_field_gradient_start), RedBlueGradientStart),
        CustomColorField("custom_color_gradient_end", stringResource(R.string.custom_color_field_gradient_end), RedBlueGradientEnd),
        CustomColorField("custom_color_search_box_bg", stringResource(R.string.custom_color_field_search_box_bg), RedBlueSearchBoxBg),
        CustomColorField("custom_color_search_box_border", stringResource(R.string.custom_color_field_search_box_border), RedBlueSearchBoxBorder),
        CustomColorField("custom_color_search_box_text", stringResource(R.string.custom_color_field_search_box_text), OnSurfaceHighContrast),
        CustomColorField("custom_color_on_surface_low_contrast", stringResource(R.string.custom_color_field_on_surface_low_contrast), OnSurfaceLowContrast)
    )

    val customFieldState = remember {
        mutableStateMapOf<String, String>()
    }

    val savedSchemes = remember {
        mutableStateMapOf<String, Map<String, String>>()
    }
    var selectedSchemeName by remember {
        mutableStateOf(prefs.getString("custom_color_selected", "") ?: "")
    }
    var schemeNameInput by remember { mutableStateOf(selectedSchemeName) }
    var schemeNameError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        customFields.forEach { field ->
            val stored = prefs.getString(field.key, null)
            val defaultHex = String.format("#%08X", field.defaultColor.toArgb())
            customFieldState[field.key] = stored ?: defaultHex
        }
        val loadedSchemes = readCustomColorSchemes(prefs)
        savedSchemes.clear()
        savedSchemes.putAll(loadedSchemes)
        val currentScheme = selectedSchemeName.trim()
        if (currentScheme.isNotBlank() && loadedSchemes.containsKey(currentScheme)) {
            applyCustomScheme(
                prefs = prefs,
                customFields = customFields,
                fieldState = customFieldState,
                schemeName = currentScheme,
                scheme = loadedSchemes.getValue(currentScheme),
                setAsActive = false
            )
        }
    }

    DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "color_scheme") {
                val value = prefs.getString("color_scheme", "red_blue") ?: "red_blue"
                customEnabled = (if (value == "cold_blue") "red_blue" else value) == "custom"
            }
            if (key == "premium_features" || key == "premium_lifetime" || key == "premium_trial_used" || key == "premium_trial_start_time") {
                canAccessPremiumFeatures = PremiumFeatureManager.canAccessPremiumFeatures(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.custom_color_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.custom_color_enable)) },
                supportingContent = { Text(stringResource(R.string.custom_color_enable_desc)) },
                trailingContent = {
                    Switch(
                        checked = customEnabled,
                        onCheckedChange = { enabled ->
                            if (!canAccessPremiumFeatures) {
                                showPremiumFeatureDialog = true
                                return@Switch
                            }
                            customEnabled = enabled
                            if (enabled) {
                                val current = prefs.getString("color_scheme", "red_blue") ?: "red_blue"
                                if (current != "custom") {
                                    prefs.edit().putString("color_scheme_prev", current).apply()
                                }
                                prefs.edit().putString("color_scheme", "custom").apply()
                            } else {
                                val previous = prefs.getString("color_scheme_prev", "red_blue") ?: "red_blue"
                                prefs.edit().putString("color_scheme", previous).apply()
                            }
                        }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            Text(
                text = stringResource(R.string.custom_color_save_title),
                style = MaterialTheme.typography.titleSmall
            )

            val schemeNameRequiredMessage = stringResource(R.string.custom_color_scheme_name_required)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = schemeNameInput,
                    onValueChange = { input ->
                        schemeNameInput = input
                        schemeNameError = null
                    },
                    label = { Text(stringResource(R.string.custom_color_scheme_name)) },
                    singleLine = true,
                    isError = schemeNameError != null,
                    supportingText = {
                        schemeNameError?.let { message ->
                            Text(text = message)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                    if (!canAccessPremiumFeatures) {
                        showPremiumFeatureDialog = true
                        return@Button
                    }
                        val name = schemeNameInput.trim()
                        if (name.isBlank()) {
                            schemeNameError = schemeNameRequiredMessage
                            return@Button
                        }
                        val scheme = buildCustomScheme(customFields, customFieldState)
                        savedSchemes[name] = scheme
                        selectedSchemeName = name
                        prefs.edit().putString("custom_color_selected", name).apply()
                        writeCustomColorSchemes(prefs, savedSchemes)
                        applyCustomScheme(
                            prefs = prefs,
                            customFields = customFields,
                            fieldState = customFieldState,
                            schemeName = name,
                            scheme = scheme,
                            setAsActive = customEnabled
                        )
                    }
                ) {
                    Text(stringResource(R.string.custom_color_scheme_save))
                }
            }

            Text(
                text = stringResource(R.string.custom_color_saved_title),
                style = MaterialTheme.typography.titleSmall
            )

            if (savedSchemes.isEmpty()) {
                Text(
                    text = stringResource(R.string.custom_color_scheme_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                savedSchemes.keys.sorted().forEach { schemeName ->
                    ListItem(
                        leadingContent = {
                            RadioButton(
                                selected = schemeName == selectedSchemeName,
                                onClick = {
                                    if (!canAccessPremiumFeatures) {
                                        showPremiumFeatureDialog = true
                                        return@RadioButton
                                    }
                                    val scheme = savedSchemes[schemeName] ?: return@RadioButton
                                    selectedSchemeName = schemeName
                                    schemeNameInput = schemeName
                                    prefs.edit().putString("custom_color_selected", schemeName).apply()
                                    applyCustomScheme(
                                        prefs = prefs,
                                        customFields = customFields,
                                        fieldState = customFieldState,
                                        schemeName = schemeName,
                                        scheme = scheme,
                                        setAsActive = customEnabled
                                    )
                                }
                            )
                        },
                        headlineContent = { Text(schemeName) },
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    savedSchemes.remove(schemeName)
                                    if (selectedSchemeName == schemeName) {
                                        selectedSchemeName = ""
                                        schemeNameInput = ""
                                        prefs.edit().remove("custom_color_selected").apply()
                                    }
                                    writeCustomColorSchemes(prefs, savedSchemes)
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.custom_color_scheme_delete)
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            if (!canAccessPremiumFeatures) {
                                showPremiumFeatureDialog = true
                                return@clickable
                            }
                            val scheme = savedSchemes[schemeName] ?: return@clickable
                            selectedSchemeName = schemeName
                            schemeNameInput = schemeName
                            prefs.edit().putString("custom_color_selected", schemeName).apply()
                            applyCustomScheme(
                                prefs = prefs,
                                customFields = customFields,
                                fieldState = customFieldState,
                                schemeName = schemeName,
                                scheme = scheme,
                                setAsActive = customEnabled
                            )
                        }
                    )
                }
            }

            customFields.forEach { field ->
                val value = customFieldState[field.key] ?: ""
                val normalized = normalizeHex(value)
                val isValid = normalized.isEmpty() || isValidHex(normalized)
                val previewColor = runCatching {
                    val cleaned = normalized.removePrefix("#")
                    val argb = when (cleaned.length) {
                        6 -> 0xFF000000L or cleaned.toLong(16)
                        8 -> cleaned.toLong(16)
                        else -> null
                    }
                    argb?.let { Color(it.toInt()) }
                }.getOrNull() ?: field.defaultColor

                OutlinedTextField(
                    value = value,
                    onValueChange = { input ->
                        if (!canAccessPremiumFeatures) {
                            showPremiumFeatureDialog = true
                            return@OutlinedTextField
                        }
                        val sanitized = sanitizeHexInput(input)
                        val updated = enforceOpaqueAlpha(sanitized)
                        customFieldState[field.key] = updated
                        val normalizedInput = normalizeHex(updated)
                        if (normalizedInput.isEmpty()) {
                            prefs.edit().remove(field.key).apply()
                        } else if (isValidHex(normalizedInput)) {
                            prefs.edit().putString(field.key, normalizedInput).apply()
                        }
                    },
                    label = { Text(field.label) },
                    placeholder = { Text(stringResource(R.string.custom_color_hex_placeholder)) },
                    isError = !isValid,
                    supportingText = {
                        if (!isValid) {
                            Text(stringResource(R.string.custom_color_hex_invalid))
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    trailingIcon = {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(previewColor, RoundedCornerShape(4.dp))
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                text = stringResource(R.string.custom_color_preview_title),
                style = MaterialTheme.typography.titleSmall
            )

            CustomHomePreview(
                background = previewColorFrom("custom_color_background", RedBlueBackground, customFieldState),
                surface = previewColorFrom("custom_color_surface", RedBlueSurface, customFieldState),
                surfaceVariant = previewColorFrom("custom_color_surface_variant", RedBlueSurfaceVariant, customFieldState),
                primary = previewColorFrom("custom_color_primary", RedBluePrimary, customFieldState),
                tertiary = previewColorFrom("custom_color_tertiary", RedBlueTertiary, customFieldState),
                primaryContainer = previewColorFrom("custom_color_primary_container", RedBluePrimaryContainer, customFieldState),
                onPrimaryContainer = previewColorFrom("custom_color_on_primary_container", RedBlueOnPrimaryContainer, customFieldState),
                gradientStart = previewColorFrom("custom_color_gradient_start", RedBlueGradientStart, customFieldState),
                gradientEnd = previewColorFrom("custom_color_gradient_end", RedBlueGradientEnd, customFieldState),
                searchBoxBg = previewColorFrom("custom_color_search_box_bg", RedBlueSearchBoxBg, customFieldState),
                searchBoxBorder = previewColorFrom("custom_color_search_box_border", RedBlueSearchBoxBorder, customFieldState),
                breadcrumbText = previewColorFrom("custom_color_on_primary_container", RedBlueOnPrimaryContainer, customFieldState),
                breadcrumbIcon = previewColorFrom("custom_color_on_primary_container", RedBlueOnPrimaryContainer, customFieldState),
                subWarehouseName = previewColorFrom("custom_color_on_primary_container", RedBlueOnPrimaryContainer, customFieldState)
            )
        }
    }

    if (FeatureFlags.ENABLE_PURCHASE_FEATURE && showPremiumFeatureDialog && billingManager != null) {
        PremiumFeatureDialog(
            billingManager = billingManager,
            onDismiss = { showPremiumFeatureDialog = false }
        )
    }
}

private fun isValidHex(input: String): Boolean {
    val text = input.trim().removePrefix("#")
    val lengthOk = text.length == 6 || text.length == 8
    if (!lengthOk || !text.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
        return false
    }
    if (text.length == 8 && !text.startsWith("FF", ignoreCase = true)) {
        return false
    }
    return true
}

private fun sanitizeHexInput(input: String): String {
    val trimmed = input.trim()
    val hasPrefix = trimmed.startsWith("#")
    val cleaned = trimmed.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
    val limited = cleaned.take(8).uppercase()
    if (limited.isEmpty()) {
        return if (hasPrefix) "#" else ""
    }
    return if (hasPrefix) "#$limited" else limited
}

private fun enforceOpaqueAlpha(input: String): String {
    val trimmed = input.trim()
    val hasPrefix = trimmed.startsWith("#")
    val cleaned = trimmed.removePrefix("#").uppercase()
    if (cleaned.length != 8) return input
    if (cleaned.startsWith("FF")) return input
    val fixed = "FF" + cleaned.substring(2)
    return if (hasPrefix) "#$fixed" else fixed
}

private fun normalizeHex(input: String): String {
    val cleaned = input.trim().removePrefix("#")
    return if (cleaned.isEmpty()) "" else "#${cleaned.uppercase()}"
}

private fun colorFromHex(input: String?, fallback: Color): Color {
    val normalized = input?.let { normalizeHex(it) }.orEmpty()
    val cleaned = normalized.removePrefix("#")
    val value = when (cleaned.length) {
        6 -> 0xFF000000L or cleaned.toLong(16)
        8 -> cleaned.toLong(16)
        else -> return fallback
    }
    return Color(value.toInt())
}

private fun previewColorFrom(
    key: String,
    fallback: Color,
    values: Map<String, String>
): Color {
    val raw = values[key]
    return if (raw.isNullOrBlank() || !isValidHex(normalizeHex(raw))) {
        fallback
    } else {
        colorFromHex(raw, fallback)
    }
}

private fun contrastText(background: Color): Color {
    val luminance = 0.299 * background.red + 0.587 * background.green + 0.114 * background.blue
    return if (luminance > 0.5f) Color.Black else Color.White
}

private fun buildCustomScheme(
    customFields: List<CustomColorField>,
    fieldState: Map<String, String>
): Map<String, String> {
    val scheme = mutableMapOf<String, String>()
    customFields.forEach { field ->
        val raw = fieldState[field.key].orEmpty()
        val normalized = normalizeHex(raw)
        if (normalized.isNotBlank() && isValidHex(normalized)) {
            scheme[field.key] = normalized
        }
    }
    return scheme
}

private fun applyCustomScheme(
    prefs: SharedPreferences,
    customFields: List<CustomColorField>,
    fieldState: MutableMap<String, String>,
    schemeName: String,
    scheme: Map<String, String>,
    setAsActive: Boolean
) {
    if (setAsActive) {
        val currentScheme = prefs.getString("color_scheme", "red_blue") ?: "red_blue"
        if (currentScheme != "custom") {
            prefs.edit().putString("color_scheme_prev", currentScheme).apply()
        }
    }
    customFields.forEach { field ->
        val defaultHex = String.format("#%08X", field.defaultColor.toArgb())
        val nextValue = scheme[field.key] ?: defaultHex
        fieldState[field.key] = nextValue
        if (nextValue.isBlank()) {
            prefs.edit().remove(field.key).apply()
        } else {
            prefs.edit().putString(field.key, normalizeHex(nextValue)).apply()
        }
    }
    prefs.edit().putString("custom_color_selected", schemeName).apply()
    if (setAsActive) {
        prefs.edit().putString("color_scheme", "custom").apply()
    }
}

private fun readCustomColorSchemes(prefs: SharedPreferences): Map<String, Map<String, String>> {
    val raw = prefs.getString("custom_color_schemes", null) ?: return emptyMap()
    return runCatching {
        val root = JSONObject(raw)
        val result = mutableMapOf<String, Map<String, String>>()
        val names = root.keys()
        while (names.hasNext()) {
            val name = names.next()
            val schemeObject = root.optJSONObject(name) ?: continue
            val colors = mutableMapOf<String, String>()
            val keys = schemeObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = schemeObject.optString(key, "")
                if (value.isNotBlank()) {
                    colors[key] = value
                }
            }
            result[name] = colors
        }
        result
    }.getOrDefault(emptyMap())
}

private fun writeCustomColorSchemes(
    prefs: SharedPreferences,
    schemes: Map<String, Map<String, String>>
) {
    val root = JSONObject()
    schemes.forEach { (name, scheme) ->
        val schemeObject = JSONObject()
        scheme.forEach { (key, value) ->
            if (value.isNotBlank()) {
                schemeObject.put(key, value)
            }
        }
        root.put(name, schemeObject)
    }
    prefs.edit().putString("custom_color_schemes", root.toString()).apply()
}

@Composable
private fun CustomHomePreview(
    background: Color,
    surface: Color,
    surfaceVariant: Color,
    primary: Color,
    tertiary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    gradientStart: Color,
    gradientEnd: Color,
    searchBoxBg: Color,
    searchBoxBorder: Color,
    breadcrumbText: Color,
    breadcrumbIcon: Color,
    subWarehouseName: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(
                    Brush.horizontalGradient(listOf(gradientStart, gradientEnd)),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = stringResource(R.string.custom_color_preview_home),
                color = contrastText(gradientStart),
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(searchBoxBg, RoundedCornerShape(10.dp))
                .then(
                    Modifier
                        .border(1.dp, searchBoxBorder, RoundedCornerShape(10.dp))
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = stringResource(R.string.custom_color_preview_search),
                color = contrastText(searchBoxBg).copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = breadcrumbIcon,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.custom_color_preview_breadcrumb_path),
                color = breadcrumbText,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = contrastText(primary),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.custom_color_preview_container_icon),
                    color = contrastText(background),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Category,
                        contentDescription = null,
                        tint = contrastText(primary),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.custom_color_preview_item_icon),
                    color = contrastText(background),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .background(primaryContainer, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(text = stringResource(R.string.custom_color_preview_container), color = onPrimaryContainer, style = MaterialTheme.typography.bodySmall)
            }
            Box(
                modifier = Modifier
                    .background(tertiary, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(text = stringResource(R.string.custom_color_preview_tertiary), color = contrastText(tertiary), style = MaterialTheme.typography.bodySmall)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceVariant, RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(primary, RoundedCornerShape(50))
                )
                Text(text = stringResource(R.string.custom_color_preview_item), color = contrastText(surfaceVariant), style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.custom_color_preview_quantity, 2), color = contrastText(surfaceVariant).copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                Text(text = stringResource(R.string.custom_color_preview_price), color = primary, style = MaterialTheme.typography.bodySmall)
            }
            Text(text = stringResource(R.string.custom_color_preview_sub_warehouse), color = subWarehouseName, style = MaterialTheme.typography.bodySmall)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(primary, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.custom_color_preview_action_button),
                color = contrastText(primary),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
