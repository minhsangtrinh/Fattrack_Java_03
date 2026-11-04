package com.example.inventory;

import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HandlerMethodValidationException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class
    })
    public ProblemDetail invalidRequest(Exception exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Invalid input: positive productId/quantity; productName 1-200 characters; "
                + "non-negative version/offset; limit 1-200.");
    }

    @ExceptionHandler({
        OptimisticLockingFailureException.class,
        OptimisticLockException.class
    })
    public ProblemDetail optimisticConflict(Exception exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Inventory was changed by another transaction. Reload and review before retry.");
    }

    @ExceptionHandler({
        PessimisticLockingFailureException.class,
        LockTimeoutException.class,
        PessimisticLockException.class
    })
    public ProblemDetail lockFailure(Exception exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.LOCKED,
                "Could not acquire the inventory lock. This transaction was rolled back.");
    }

    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail databaseFailure(DataAccessException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Database operation could not complete. If a stock-in response was lost, "
                + "check inventory/history before submitting the operation again.");
    }
}
