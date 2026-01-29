package com.example.itemremindertool.network.dto

import com.google.gson.annotations.SerializedName

data class ItemReminderDto(
    @SerializedName("uuid")
    val uuid: String,
    @SerializedName("itemUuid")
    val itemUuid: String,
    @SerializedName("reminderType")
    val reminderType: String,
    @SerializedName("reminderTime")
    val reminderTime: String? = null,
    @SerializedName("dailyTime")
    val dailyTime: String? = null,
    @SerializedName("monthlyDay")
    val monthlyDay: Int? = null,
    @SerializedName("monthlyTime")
    val monthlyTime: String? = null,
    @SerializedName("yearlyMonth")
    val yearlyMonth: Int? = null,
    @SerializedName("yearlyDay")
    val yearlyDay: Int? = null,
    @SerializedName("yearlyTime")
    val yearlyTime: String? = null,
    @SerializedName("reason")
    val reason: String? = null,
    @SerializedName("isEnabled")
    val isEnabled: Boolean? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

data class ItemReminderListResponse(
    @SerializedName("reminders")
    val reminders: List<ItemReminderDto>,
    @SerializedName("total")
    val total: Int,
    @SerializedName("page")
    val page: Int,
    @SerializedName("pageSize")
    val pageSize: Int
)
