package org.example.hive.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthException(AuthException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(new ErrorResponseDto(ex.getStatus().value(), ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ErrorResponseDto> handleUserException(UserException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(new ErrorResponseDto(ex.getStatus().value(), ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponseDto(401, "Invalid email or password", LocalDateTime.now()));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponseDto> handleDisabled(DisabledException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponseDto(403, "Account is deactivated", LocalDateTime.now()));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleUserNotFound(UsernameNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponseDto(401, "Invalid email or password", LocalDateTime.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDto(400, message, LocalDateTime.now()));
    }

    @ExceptionHandler(CompanyException.class)
    public ResponseEntity<ErrorResponseDto> handleCompanyException(CompanyException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(new ErrorResponseDto(ex.getStatus().value(), ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(EventException.class)
    public ResponseEntity<ErrorResponseDto> handleEventException(EventException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(new ErrorResponseDto(ex.getStatus().value(), ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(PostException.class)
    public ResponseEntity<ErrorResponseDto> handlePostException(PostException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(new ErrorResponseDto(ex.getStatus().value(), ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(TeamException.class)
    public ResponseEntity<ErrorResponseDto> handleTeamException(TeamException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(new ErrorResponseDto(ex.getStatus().value(), ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponseDto(409, "Could not save changes due to a data conflict", LocalDateTime.now()));
    }
}