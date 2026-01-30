package com.yu.yuapiclientsdk.exception;

import com.yu.yuapiclientsdk.common.BaseResponse;
import com.yu.yuapiclientsdk.common.ErrorCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自定义API异常类
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiException extends RuntimeException {

    private int code;

    public ApiException() {
        this(ErrorCode.OPERATION_ERROR);
    }

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public ApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ApiException(String message) {
        super(message);
        this.code = ErrorCode.OPERATION_ERROR.getCode();
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.code = ErrorCode.OPERATION_ERROR.getCode();
    }

    /**
     * 转换为BaseResponse对象
     */
    public <T> BaseResponse<T> toBaseResponse() {
        return new BaseResponse<>(this.code, null, this.getMessage());
    }
}