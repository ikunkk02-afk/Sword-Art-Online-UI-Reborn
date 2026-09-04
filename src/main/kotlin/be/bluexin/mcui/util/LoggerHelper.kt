package be.bluexin.mcui.util

import org.slf4j.Logger

inline fun Logger.trace(message: () -> String) {
    if (isTraceEnabled) trace(message())
}

inline fun Logger.debug(message: () -> String) {
    if (isDebugEnabled) debug(message())
}

inline fun Logger.info(message: () -> String) {
    if (isInfoEnabled) info(message())
}

inline fun Logger.warn(message: () -> String) {
    if (isWarnEnabled) warn(message())
}

inline fun Logger.error(message: () -> String) {
    if (isErrorEnabled) error(message())
}
