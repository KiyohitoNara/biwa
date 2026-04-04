package io.github.kiyohitonara.biwa

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform