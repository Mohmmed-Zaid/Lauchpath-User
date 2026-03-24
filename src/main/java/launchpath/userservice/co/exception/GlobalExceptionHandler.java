// Catches ALL exceptions app-wide
// Returns clean JSON — never expose stack traces to client
// ============================================================
package launchpath.userservice.co.exception;

import launchpath.userservice.co.dto.response.ApiResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // @Valid failed on request DTO
    // Returns map of field → error message
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });

        log.warn("Validation failed: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.error("Validation failed: " + errors));
    }

    // Entity not found in DB
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleNotFound(
            ResourceNotFoundException ex) {
        log.warn("Not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDTO.error(ex.getMessage()));
    }

    // Duplicate email, duplicate plan etc
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleConflict(
            ResourceAlreadyExistsException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponseDTO.error(ex.getMessage()));
    }

    // ATS credits or download limit exhausted
    @ExceptionHandler(InsufficientCreditsException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleInsufficientCredits(
            InsufficientCreditsException ex) {
        log.warn("Insufficient credits: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.PAYMENT_REQUIRED)
                .body(ApiResponseDTO.error(ex.getMessage()));
    }

    // Accessing another user's data or wrong role
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleUnauthorized(
            UnauthorizedAccessException ex) {
        log.warn("Unauthorized: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponseDTO.error(ex.getMessage()));
    }

    // Razorpay signature mismatch
    @ExceptionHandler(PaymentVerificationException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handlePaymentVerification(
            PaymentVerificationException ex) {
        log.error("Payment verification failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.error(ex.getMessage()));
    }

    // Business rule violated
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleIllegalState(
            IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponseDTO.error(ex.getMessage()));
    }

    // Bad input value
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleIllegalArgument(
            IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.error(ex.getMessage()));
    }

    // Missing required request param
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleMissingParam(
            MissingServletRequestParameterException ex) {
        log.warn("Missing parameter: {}", ex.getParameterName());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.error(
                        "Required parameter missing: " + ex.getParameterName()
                ));
    }

    // Wrong type in path variable or param
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.error(
                        "Invalid value for parameter: " + ex.getName()
                ));
    }

    // Catch-all — never expose internal errors or stack traces
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleGeneric(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO.error(
                        "Something went wrong. Please try again later."
                ));
    }
}