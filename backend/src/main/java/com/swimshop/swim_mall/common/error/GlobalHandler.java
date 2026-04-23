package com.swimshop.swim_mall.common.error;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.swimshop.swim_mall.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j //로그변수
@RestControllerAdvice
public class GlobalHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException e){
        // BusinessException에서 ErrorCode 꺼내기
        ErrorCode errorCode = e.getErrorCode();
        
        // 예외 메시지가 ErrorCode의 기본 메시지와 다르면 예외 메시지 사용 (더 구체적인 메시지)
        String message = e.getMessage();
        if (message != null && !message.equals(errorCode.getMessage())) {
            // 커스텀 메시지가 있는 경우 ApiResponse.fail() 메서드 사용
            ApiResponse<?> response = ApiResponse.fail(errorCode, message);
            return ResponseEntity
                .status(errorCode.getStatus())
                .body(response);
        }
        
        // 기본 메시지 사용
        ApiResponse<?> response = ApiResponse.fail(errorCode);
        
        // HTTP 상태 코드 설정하고 반환
        return ResponseEntity
            .status(errorCode.getStatus())
            .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getMessage());
        
        // 검증 오류 메시지 수집
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());
        
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        return ResponseEntity
            .status(400)
            .body(ApiResponse.fail(errorCode, errors));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("Resource not found: {}", e.getResourcePath());
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        return ResponseEntity
            .status(404)
            .body(ApiResponse.fail(errorCode));
    }

    @ExceptionHandler(Exception.class)  // 모든 예외(예상치 못한)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
    log.error("Unexpected error: ", e);
    ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
    return ResponseEntity
        .status(500)
        .body(ApiResponse.fail(errorCode));
}
}
