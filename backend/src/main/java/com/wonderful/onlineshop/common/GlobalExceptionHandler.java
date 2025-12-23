package com.wonderful.onlineshop.common;

import cn.dev33.satoken.exception.NotLoginException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        if (fieldError == null) {
            return ApiResponse.fail("validation failed");
        }
        String msg = fieldError.getDefaultMessage();
        if (msg == null || msg.isBlank()) {
            msg = "validation failed";
        }
        return ApiResponse.fail(fieldError.getField() + ": " + msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = "validation failed";
        }
        return ApiResponse.fail(msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        return ApiResponse.fail("invalid request body");
    }

    @ExceptionHandler(NotLoginException.class)
    public ApiResponse<Void> handleNotLogin(NotLoginException e) {
        return ApiResponse.fail("not login");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleOther(Exception e) {
        return ApiResponse.fail("internal server error");
    }
}
