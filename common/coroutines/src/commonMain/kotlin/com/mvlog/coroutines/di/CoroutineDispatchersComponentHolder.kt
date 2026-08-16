package com.mvlog.coroutines.di

import com.mvlog.di.LazyComponentHolder

object CoroutineDispatchersComponentHolder : LazyComponentHolder<CoroutineDispatchersComponent>() {
    override fun build(): CoroutineDispatchersComponent {
        return CoroutineDispatchersComponentImpl()
    }
}
