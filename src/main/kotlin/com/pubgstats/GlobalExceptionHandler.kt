package com.pubgstats

import org.slf4j.LoggerFactory
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

/** Turns any unhandled exception into a simple, styled error page instead of a stack trace. */
@ControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(Exception::class)
    fun handle(e: Exception, model: Model): String {
        log.error("Unexpected error rendering a page", e)
        model.addAttribute("message", e.message ?: "Something went wrong.")
        return "error"
    }
}
