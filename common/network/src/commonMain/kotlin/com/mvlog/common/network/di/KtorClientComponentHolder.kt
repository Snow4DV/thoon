package com.mvlog.common.network.di

import com.mvlog.di.LazyComponentHolder

object KtorClientComponentHolder : LazyComponentHolder<KtorClientComponent>() {
    override fun build(): KtorClientComponent {
        return KtorClientComponentImpl()
    }
}
