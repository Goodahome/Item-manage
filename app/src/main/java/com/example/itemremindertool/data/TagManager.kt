package com.example.itemremindertool.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TagManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tag_prefs", Context.MODE_PRIVATE)
    private val TAG_KEY = "all_tags"
    
    private val _allTags = MutableStateFlow<Set<String>>(loadTags())
    val allTags: StateFlow<Set<String>> = _allTags.asStateFlow()
    
    private fun loadTags(): Set<String> {
        val tagsString = prefs.getString(TAG_KEY, "") ?: ""
        return if (tagsString.isEmpty()) {
            emptySet()
        } else {
            tagsString.split(",").filter { it.isNotEmpty() }.toSet()
        }
    }
    
    fun addTag(tag: String) {
        val currentTags = _allTags.value.toMutableSet()
        if (currentTags.add(tag)) {
            saveTags(currentTags)
            _allTags.value = currentTags
        }
    }
    
    fun removeTag(tag: String) {
        val currentTags = _allTags.value.toMutableSet()
        if (currentTags.remove(tag)) {
            saveTags(currentTags)
            _allTags.value = currentTags
        }
    }
    
    fun updateTag(oldTag: String, newTag: String) {
        val currentTags = _allTags.value.toMutableSet()
        if (currentTags.remove(oldTag)) {
            currentTags.add(newTag)
            saveTags(currentTags)
            _allTags.value = currentTags
        }
    }
    
    fun setAllTags(tags: Set<String>) {
        saveTags(tags)
        _allTags.value = tags
    }
    
    private fun saveTags(tags: Set<String>) {
        prefs.edit()
            .putString(TAG_KEY, tags.joinToString(","))
            .apply()
    }
    
    fun getAllTags(): Set<String> = _allTags.value
}

