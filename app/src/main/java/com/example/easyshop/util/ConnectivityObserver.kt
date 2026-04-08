package com.example.easyshop.util

interface ConnectivityObserver {
    enum class Status {
        Available, Unavailable, Losing, Lost
    }
}
