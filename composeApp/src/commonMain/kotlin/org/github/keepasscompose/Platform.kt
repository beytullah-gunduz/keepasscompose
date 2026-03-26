package org.github.keepasscompose

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
