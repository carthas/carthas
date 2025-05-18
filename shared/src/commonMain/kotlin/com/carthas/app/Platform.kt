package com.carthas.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform