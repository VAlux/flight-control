package com.flightcontrol.web;

import com.flightcontrol.domain.exception.BusinessRuleViolationException;
import com.flightcontrol.domain.exception.DuplicateFlightNumberException;
import com.flightcontrol.domain.exception.FlightNotDeletableException;
import com.flightcontrol.domain.exception.FlightNotFoundException;
import com.flightcontrol.domain.exception.FlightNotModifiableException;
import com.flightcontrol.domain.exception.InvalidStatusTransitionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String BASE_TYPE_URI = "https://flightcontrol.com/errors/";
    private static final MediaType PROBLEM_JSON = MediaType.valueOf("application/problem+json");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        ProblemDetail problem = buildProblem(
                HttpStatus.BAD_REQUEST,
                "validation-failed",
                "Validation Failed",
                "Request body contains invalid fields",
                request);

        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toErrorEntry)
                .toList();
        problem.setProperty("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        ProblemDetail problem = buildProblem(
                HttpStatus.BAD_REQUEST,
                "validation-failed",
                "Validation Failed",
                "Request body is missing or malformed",
                request);
        problem.setProperty("errors", List.of());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ProblemDetail> handleBusinessRuleViolation(
            BusinessRuleViolationException ex, HttpServletRequest request) {

        ProblemDetail problem = buildProblem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "validation-failed",
                "Business Rule Violation",
                ex.getMessage(),
                request);

        problem.setProperty("errors", List.of(
                Map.of("field", "", "code", "business_rule_violation", "message", ex.getMessage())
        ));

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(FlightNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleFlightNotFound(
            FlightNotFoundException ex, HttpServletRequest request) {

        ProblemDetail problem = buildProblem(
                HttpStatus.NOT_FOUND,
                "flight-not-found",
                "Flight Not Found",
                ex.getMessage(),
                request);

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(DuplicateFlightNumberException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateFlightNumber(
            DuplicateFlightNumberException ex, HttpServletRequest request) {

        ProblemDetail problem = buildProblem(
                HttpStatus.CONFLICT,
                "duplicate-flight-number",
                "Duplicate Flight Number",
                ex.getMessage(),
                request);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(FlightNotModifiableException.class)
    public ResponseEntity<ProblemDetail> handleFlightNotModifiable(
            FlightNotModifiableException ex, HttpServletRequest request) {

        ProblemDetail problem = buildProblem(
                HttpStatus.CONFLICT,
                "flight-not-modifiable",
                "Flight Not Modifiable",
                ex.getMessage(),
                request);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(FlightNotDeletableException.class)
    public ResponseEntity<ProblemDetail> handleFlightNotDeletable(
            FlightNotDeletableException ex, HttpServletRequest request) {

        ProblemDetail problem = buildProblem(
                HttpStatus.CONFLICT,
                "flight-not-deletable",
                "Flight Not Deletable",
                ex.getMessage(),
                request);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ProblemDetail> handleInvalidStatusTransition(
            InvalidStatusTransitionException ex, HttpServletRequest request) {

        ProblemDetail problem = buildProblem(
                HttpStatus.CONFLICT,
                "invalid-status-transition",
                "Invalid Status Transition",
                ex.getMessage(),
                request);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        List<Map<String, String>> errors = ex.getConstraintViolations().stream()
                .map(cv -> Map.of(
                        "field", extractFieldName(cv),
                        "code", cv.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
                        "message", cv.getMessage()))
                .collect(Collectors.toList());

        ProblemDetail problem = buildProblem(
                HttpStatus.BAD_REQUEST,
                "validation-failed",
                "Validation Failed",
                "One or more request parameters are invalid",
                request);
        problem.setProperty("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        ProblemDetail problem = buildProblem(
                HttpStatus.BAD_REQUEST,
                "validation-failed",
                "Validation Failed",
                "Invalid value '" + Objects.toString(ex.getValue(), "<missing>") + "' for parameter '" + ex.getName() + "'",
                request);
        problem.setProperty("errors", List.of(Map.of(
                "field", ex.getName(),
                "code", "InvalidType",
                "message", "Invalid value for parameter")));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error processing request", ex);

        ProblemDetail problem = buildProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal-error",
                "Internal Server Error",
                "An unexpected error occurred",
                request);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(PROBLEM_JSON)
                .body(problem);
    }

    // --- private helpers ---

    private ProblemDetail buildProblem(HttpStatus status, String typeSlug, String title,
                                       String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create(BASE_TYPE_URI + typeSlug));
        problem.setTitle(title);
        problem.setDetail(detail);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString());
        String requestId = MDC.get("requestId");
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        problem.setProperty("correlation_id", requestId);
        return problem;
    }

    private String extractFieldName(ConstraintViolation<?> cv) {
        String path = cv.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot + 1) : path;
    }

    private Map<String, String> toErrorEntry(FieldError fieldError) {
        return Map.of(
                "field", fieldError.getField(),
                "code", fieldError.getCode() != null ? fieldError.getCode() : "invalid",
                "message", fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : ""
        );
    }
}
