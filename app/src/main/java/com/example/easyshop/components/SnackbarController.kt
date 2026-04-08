package com.example.easyshop.components

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class SnackbarType { SUCCESS, ERROR, WARNING, INFO }

data class SnackbarEvent(
    val message: String,
    val subtext: String? = null,
    val type: SnackbarType = SnackbarType.INFO
)

object SnackbarController {
    private val _events = MutableSharedFlow<SnackbarEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun show(message: String, subtext: String? = null, type: SnackbarType = SnackbarType.INFO) {
        _events.tryEmit(SnackbarEvent(message, subtext, type))
    }
    fun success(message: String, subtext: String? = null) = show(message, subtext, SnackbarType.SUCCESS)
    fun error(message: String, subtext: String? = null) = show(message, subtext, SnackbarType.ERROR)
    fun warning(message: String, subtext: String? = null) = show(message, subtext, SnackbarType.WARNING)
    fun info(message: String, subtext: String? = null) = show(message, subtext, SnackbarType.INFO)
}
