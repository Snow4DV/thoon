package com.mvlog.common.serialization.di

import kotlinx.serialization.json.Json

interface JsonComponent {

    fun json(): Json
}