package com.realdev.readle.global.exception;

import static com.realdev.readle.global.exception.GlobalErrorCode.INVALID_INPUT;
import static com.realdev.readle.global.exception.GlobalErrorCode.METHOD_NOT_SUPPORTED;
import static com.realdev.readle.global.exception.GlobalErrorCode.NOT_ACCEPTABLE;
import static com.realdev.readle.global.exception.GlobalErrorCode.NOT_FOUND;
import static com.realdev.readle.global.exception.GlobalErrorCode.SERVER_ERROR;
import static com.realdev.readle.global.exception.GlobalErrorCode.UNSUPPORTED_MEDIA_TYPE;

import com.realdev.readle.global.exception.dto.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  // ErrorCode의 HTTP status를 응답 status로 사용하고, body는 {code, message} 형식으로 반환한다.
  private ResponseEntity<Object> errorResponse(ErrorCode errorCode) {
    ErrorResponse response = ErrorResponse.error(errorCode);

    return ResponseEntity.status(response.status()).body(response);
  }

  // ErrorCode의 HTTP status를 유지하면서 기본 메시지만 전달받은 메시지로 대체한다.
  private ResponseEntity<Object> errorResponse(ErrorCode errorCode, String message) {
    ErrorResponse response = ErrorResponse.error(errorCode, message);

    return ResponseEntity.status(response.status()).body(response);
  }

  // ErrorCode 기반 {code, message} 응답을 만들고, Spring MVC가 전달한 HTTP headers를 보존한다.
  private ResponseEntity<Object> errorResponse(ErrorCode errorCode, HttpHeaders headers) {
    ErrorResponse response = ErrorResponse.error(errorCode);

    return ResponseEntity.status(response.status()).headers(headers).body(response);
  }

  // CustomException의 ErrorCode와 메시지를 그대로 사용해 {code, message} 형식으로 반환한다.
  private ResponseEntity<Object> errorResponse(CustomException exception) {
    ErrorResponse response = ErrorResponse.error(exception);

    return ResponseEntity.status(response.status()).body(response);
  }

  // createResponseEntity에서 Spring 기본 예외 status를 프로젝트 ErrorCode로 변환한다.
  private ErrorCode resolveErrorCode(HttpStatusCode statusCode) {
    return switch (statusCode.value()) {
      case 404 -> NOT_FOUND;
      case 405 -> METHOD_NOT_SUPPORTED;
      case 406 -> NOT_ACCEPTABLE;
      case 415 -> UNSUPPORTED_MEDIA_TYPE;
      default -> statusCode.is5xxServerError() ? SERVER_ERROR : INVALID_INPUT;
    };
  }

  // 비즈니스 예외를 ErrorResponse {code, message}로 반환한다. HTTP status는 ErrorCode를 따른다.
  @ExceptionHandler(CustomException.class)
  protected ResponseEntity<Object> handleCustomException(CustomException exception) {
    ErrorCode errorCode = exception.getErrorCode();

    if (exception.isServerError()) {
      log.error(
          "[SERVER_ERROR] code={}, message={}",
          errorCode.getCode(),
          exception.getMessage(),
          exception);
    } else {
      log.warn("[BUSINESS_ERROR] code={}, message={}", errorCode.getCode(), exception.getMessage());
    }

    return errorResponse(exception);
  }

  // 명시적으로 처리하지 못한 서버 예외를 SERVER_ERROR로 반환한다. HTTP status는 500이다.
  @ExceptionHandler(Exception.class)
  protected ResponseEntity<Object> handleException(Exception exception) {
    log.error("Unexpected error occurred", exception);

    return errorResponse(SERVER_ERROR);
  }

  // 지원하지 않는 HTTP method를 METHOD_NOT_SUPPORTED로 반환한다. HTTP status는 405이고 Allow 헤더를 보존한다.
  @Override
  protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
      HttpRequestMethodNotSupportedException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return errorResponse(METHOD_NOT_SUPPORTED, headers);
  }

  // malformed JSON 등 읽을 수 없는 요청 body를 INVALID_INPUT으로 반환한다. HTTP status는 400이다.
  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return errorResponse(INVALID_INPUT);
  }

  // 필수 query parameter 누락을 INVALID_INPUT으로 반환한다. HTTP status는 400이다.
  @Override
  protected ResponseEntity<Object> handleMissingServletRequestParameter(
      MissingServletRequestParameterException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return errorResponse(INVALID_INPUT);
  }

  // query/path 타입 변환 실패를 INVALID_INPUT으로 반환한다. HTTP status는 400이다.
  @Override
  protected ResponseEntity<Object> handleTypeMismatch(
      TypeMismatchException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    if (exception instanceof MethodArgumentTypeMismatchException methodException) {
      String message = methodException.getName() + " 파라미터 타입이 올바르지 않습니다.";
      return errorResponse(INVALID_INPUT, message);
    }

    return errorResponse(INVALID_INPUT);
  }

  // 지원하지 않는 Content-Type을 UNSUPPORTED_MEDIA_TYPE으로 반환한다. HTTP status는 415이고 headers를 보존한다.
  @Override
  protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return errorResponse(UNSUPPORTED_MEDIA_TYPE, headers);
  }

  // 지원하지 않는 Accept 요청을 NOT_ACCEPTABLE로 반환한다. HTTP status는 406이고 headers를 보존한다.
  @Override
  protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(
      HttpMediaTypeNotAcceptableException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return errorResponse(NOT_ACCEPTABLE, headers);
  }

  // 찾을 수 없는 정적 리소스를 NOT_FOUND로 반환한다. HTTP status는 404이다.
  @Override
  protected ResponseEntity<Object> handleNoResourceFoundException(
      NoResourceFoundException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return errorResponse(NOT_FOUND);
  }

  // 컨트롤러 method validation 실패를 처리한다. request 검증은 400, return value 검증은 500으로 반환한다.
  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    if (exception.getStatusCode().is5xxServerError()) {
      return errorResponse(SERVER_ERROR);
    }

    String message =
        exception.getAllErrors().stream()
            .map(MessageSourceResolvable::getDefaultMessage)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(", "));

    if (message.isBlank()) {
      message = INVALID_INPUT.getMessage();
    }

    return errorResponse(INVALID_INPUT, message);
  }

  // @RequestBody DTO 검증 실패를 INVALID_INPUT으로 반환한다. HTTP status는 400이고 field 메시지를 포함한다.
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    String message =
        exception.getBindingResult().getAllErrors().stream()
            .map(this::formatValidationError)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(", "));

    if (message.isBlank()) {
      message = INVALID_INPUT.getMessage();
    }

    return errorResponse(INVALID_INPUT, message);
  }

  // @Validated 기반 검증 실패를 INVALID_INPUT으로 반환한다. HTTP status는 400이다.
  @ExceptionHandler(ConstraintViolationException.class)
  protected ResponseEntity<Object> handleConstraintViolationException(
      ConstraintViolationException exception) {
    String message =
        exception.getConstraintViolations().stream()
            .map(violation -> violation.getMessage())
            .filter(Objects::nonNull)
            .collect(Collectors.joining(", "));

    if (message.isBlank()) {
      message = INVALID_INPUT.getMessage();
    }

    return errorResponse(INVALID_INPUT, message);
  }

  // Spring 기본 ProblemDetail 응답을 ErrorResponse {code, message}로 정규화한다. HTTP status는 매핑된 ErrorCode를
  // 따른다.
  @Override
  protected ResponseEntity<Object> createResponseEntity(
      Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
    if (body instanceof ErrorResponse) {
      return super.createResponseEntity(body, headers, statusCode, request);
    }

    ErrorCode errorCode = resolveErrorCode(statusCode);
    ErrorResponse response = ErrorResponse.error(errorCode);

    return super.createResponseEntity(response, headers, response.status(), request);
  }

  // validation 오류를 "field: message" 형태로 변환한다. 필드가 없는 오류는 기본 메시지만 반환한다.
  private String formatValidationError(ObjectError error) {
    String defaultMessage = error.getDefaultMessage();

    if (defaultMessage == null) {
      return null;
    }

    if (error instanceof FieldError fieldError) {
      return fieldError.getField() + ": " + defaultMessage;
    }

    return defaultMessage;
  }
}
