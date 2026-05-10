package com.example.order_manager.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(ex.statusCode).body(ErrorResponse(ex.reason ?: ex.message))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = ex.bindingResult.fieldErrors.joinToString(", ") { it.defaultMessage ?: it.field }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(message))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse("Unexpected error: ${ex.message}"))
    }

    data class ErrorResponse(val message: String?)
}
