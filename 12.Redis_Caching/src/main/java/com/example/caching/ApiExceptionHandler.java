package com.example.caching;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({
        HandlerMethodValidationException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ProblemDetail invalidId(Exception exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Product id must be a positive integer.");
    }

    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail infrastructureFailure(DataAccessException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "A database or cache operation could not complete. A DELETE may already "
                + "be committed; retrying the same product id is safe.");
    }
}
