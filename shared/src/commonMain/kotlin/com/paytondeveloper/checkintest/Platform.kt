package com.paytondeveloper.checkintest

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform