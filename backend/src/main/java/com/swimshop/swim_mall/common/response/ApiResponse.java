package com.swimshop.swim_mall.common.response;

import com.swimshop.swim_mall.common.error.ErrorCode;
import lombok.Getter;
import java.util.List;

//응답 구조(성공/실패)
@Getter
public class ApiResponse<T> {
    
    private boolean success;
    private String code;        // 실패 시 에러 코드
    private String message;
    private T data;            // 성공 시 데이터
    private List<String> errors; // 실패 시 상세 에러 목록 (선택적)
    
    // 생성자 (같은 패키지에서 접근 가능하도록 package-private)
    ApiResponse(boolean success, String code, String message, T data, List<String> errors) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
        this.errors = errors;
    }
    
    // 성공 응답 (데이터 o)
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, "성공", data, null);
    }
    //return ApiResponse.success(customerDto);
    // 결과: { "success": true, "code": null, "message": "성공", "data": {...}, "errors": null }
    
    // 성공 응답 (데이터 x, 커스텀 메시지)
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, null, message, null, null);
    }
    //return ApiResponse.success("고객이 등록되었습니다");
    // 결과: { "success": true, "code": null, "message": "고객이 등록되었습니다", "data": null, "errors": null }
    
    // 실패 응답 (ErrorCode 사용)
    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>(
            false, 
            errorCode.name(), 
            errorCode.getMessage(), 
            null, 
            null
        );
    }
    //return ApiResponse.fail(ErrorCode.CUSTOMER_NOT_FOUND);
    // 결과: { "success": false, "code": "CUSTOMER_NOT_FOUND", "message": "고객을 찾을 수 없습니다", "data": null, "errors": null }
    
    // 실패 응답 (ErrorCode + 상세 에러 목록)
    public static <T> ApiResponse<T> fail(ErrorCode errorCode, List<String> errors) {
        return new ApiResponse<>(
            false, 
            errorCode.name(), 
            errorCode.getMessage(), 
            null, 
            errors
        );
    }
    //List<String> errors = Arrays.asList("이메일 형식이 올바르지 않습니다", "비밀번호는 8자 이상이어야 합니다");
    //return ApiResponse.fail(ErrorCode.INVALID_INPUT, errors);
    // 결과: { "success": false, "code": "INVALID_INPUT", "message": "...", "data": null, "errors": [...] }

    // 실패 응답 (ErrorCode + 커스텀 메시지)
    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String customMessage) {
        return new ApiResponse<>(
            false,
            errorCode.name(),
            customMessage,
            null,
            null
        );
    }
    //return ApiResponse.fail(ErrorCode.INVALID_REQUEST, "운영 중인 파트너만 휴업 신청이 가능합니다.");
    // 결과: { "success": false, "code": "INVALID_REQUEST", "message": "운영 중인 파트너만 휴업 신청이 가능합니다.", "data": null, "errors": null }

}

