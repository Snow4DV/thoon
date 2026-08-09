package com.mvlog.thoon

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform