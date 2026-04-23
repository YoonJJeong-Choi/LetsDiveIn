package com.swimshop.swim_mall.common.error;

import lombok.Getter;

//에러 코드를 담는 예외 클래스
//서비스에서 throw할 예외
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] args;  // String.format용 파라미터 (동적 메시지)

    // ErrorCode만 받기 (고정된 메시지만 필요)
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.args = null;
    }

    // ErrorCode + 동적 파라미터 받기 (동적으로 값을 넣어야 할 때)
    public BusinessException(ErrorCode errorCode, Object... args) {
        super(formatMessage(errorCode.getMessage(), args));
        this.errorCode = errorCode;
        this.args = args;
    }
    
    /**
     * 메시지 포맷팅 처리
     * - 포맷 문자열(%s 등)이 있으면 String.format 사용
     * - 포맷 문자열이 없고 args가 하나의 문자열이면 그 문자열을 직접 사용
     * - 그 외의 경우는 기본 메시지 사용
     */
    private static String formatMessage(String baseMessage, Object... args) {
        if (args == null || args.length == 0) {
            return baseMessage;
        }
        
        // 포맷 문자열이 있는지 확인 (%s, %d 등)
        boolean hasFormatSpecifier = baseMessage.contains("%");
        
        if (hasFormatSpecifier) {
            // 포맷 문자열이 있으면 String.format 사용
            try {
                return String.format(baseMessage, args);
            } catch (Exception e) {
                // 포맷 오류 시 기본 메시지 사용
                return baseMessage;
            }
        } else {
            // 포맷 문자열이 없고 args가 하나의 문자열이면 그 문자열을 직접 사용
            if (args.length == 1 && args[0] instanceof String) {
                return (String) args[0];
            }
            // 그 외의 경우는 기본 메시지 사용
            return baseMessage;
        }
    }
}
