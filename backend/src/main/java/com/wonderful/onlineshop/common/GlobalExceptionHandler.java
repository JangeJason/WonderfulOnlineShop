package com.wonderful.onlineshop.common;

import cn.dev33.satoken.exception.NotLoginException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = "business error";
        }
        return ApiResponse.fail(msg);
    }

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
    public ApiResponse<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        e.printStackTrace();
        return ApiResponse.fail("invalid request body: " + e.getMessage());
    }

    @ExceptionHandler(NotLoginException.class)
    public ApiResponse<Void> handleNotLogin(NotLoginException e) {
        return ApiResponse.fail("not login");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleOther(Exception e) {
        log.error("Unhandled exception", e);
        return ApiResponse.fail("internal server error: " + e.getMessage());
    }
}
