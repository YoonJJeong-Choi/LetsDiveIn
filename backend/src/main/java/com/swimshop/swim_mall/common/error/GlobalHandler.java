package com.swimshop.swim_mall.common.error;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.swimshop.swim_mall.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
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

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("HTTP method not supported: {} {}", e.getMethod(), e.getMessage());
        String[] supported = e.getSupportedMethods();
        String allowed = supported != null ? String.join(", ", supported) : "POST";
        return ResponseEntity
                .status(405)
                .body(ApiResponse.fail(ErrorCode.INVALID_REQUEST,
                        "지원하지 않는 HTTP 메서드입니다. " + e.getMethod() + " (허용: " + allowed + ")"));
    }

    /**
     * 브라우저/클라이언트(bots, OpenAI fetch, 미리보기 등)가 이미지/파일을 끝까지 받기 전에 연결을 끊을 때
     * 발생하며, 서비스 결함이라기보다 흔한 케이다. 응답이 이미 바이트 스트리밍 중이면 JSON 에러 바디를
     * 쓰지 않는다(그렇지 않으면 Content-Type이 image/*인 채로 ApiResponse 직렬화에 실패).
     */
    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbortException(ClientAbortException e) {
        if (log.isDebugEnabled()) {
            log.debug("Client aborted connection: {}", e.getMessage());
        }
    }

    @ExceptionHandler(Exception.class)  // 모든 예외(예상치 못한)
    public Object handleException(Exception e, HttpServletResponse httpResponse) {
        if (httpResponse != null && httpResponse.isCommitted()) {
            if (e instanceof ClientAbortException || e.getCause() instanceof ClientAbortException) {
                if (log.isDebugEnabled()) {
                    log.debug("Client abort after response committed, {}", e.getMessage());
                }
            } else {
                log.warn("Error after response committed, skipping JSON error body: {} — {}",
                        e.getClass().getSimpleName(), e.getMessage());
            }
            return null;
        }
        if (e instanceof ClientAbortException || e.getCause() instanceof ClientAbortException) {
            if (log.isDebugEnabled()) {
                log.debug("Client aborted connection: {}", e.getMessage());
            }
            return null;
        }
        log.error("Unexpected error: ", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(500)
                .body(ApiResponse.fail(errorCode));
    }
}
