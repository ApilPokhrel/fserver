package com.fileserver.app.config;

import javax.validation.ConstraintViolationException;

import com.fileserver.app.entity.ErrorResponse;
import com.fileserver.app.exception.AWSUploadException;
import com.fileserver.app.exception.FileNotDownloadedException;
import com.fileserver.app.exception.FileStorageException;
import com.fileserver.app.exception.NotFoundException;
import com.fileserver.app.exception.NotSupportedException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ExceptionHandlerImpl extends ResponseEntityExceptionHandler {

  @ExceptionHandler(ConstraintViolationException.class)
  public final ResponseEntity<Object> handleConstraintViolationExceptions(ConstraintViolationException ex) {
    String string = "%s\n";
    String exceptionResponse = String.format(string, ex.getMessage());
    return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(NotFoundException.class)
  public final ResponseEntity<Object> handleNotFoundExceptions(NotFoundException ex) {
    String string = "%s\n";
    String exceptionResponse = String.format(string, ex.getMessage());
    return new ResponseEntity<>(exceptionResponse, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(FileStorageException.class)
  public final ResponseEntity<Object> handleFileStorageExceptions(FileStorageException ex) {
    String string = "%s\n";
    String exceptionResponse = String.format(string, ex.getMessage());
    return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(AWSUploadException.class)
  public final ResponseEntity<Object> handleAWSUploadException(AWSUploadException ex) {
    String string = "AWS %s \n";
    String exceptionResponse = String.format(string, ex.getMessage());
    return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(NotSupportedException.class)
  public final ResponseEntity<Object> handleNotSupportedException(NotSupportedException ex) {
    String string = "%s \n";
    String exceptionResponse = String.format(string, ex.getMessage());
    return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(FileNotDownloadedException.class)
  public final ErrorResponse handleNotSupportedException(FileNotDownloadedException ex) {
    return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), ex.getStackTrace().toString());
  }
}