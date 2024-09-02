package org.uooc.richtext

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform