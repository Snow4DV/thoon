package com.mvlog.common.serialization.di

import com.mvlog.di.LazyComponentHolder

object JsonComponentHolder : LazyComponentHolder<JsonComponent>() {
    override fun build(): JsonComponent {
        return JsonComponentImpl()
    }
}
