package com.fz.fzpicturebackend.exception;

import lombok.Getter;

/**
 * @Author fang
 * @Date 2025/2/26 13:34
 * @注释
 */

@Getter
public class BusinessException extends RuntimeException{
    /**
     * 错误码
     */
    private final int code;

    public BusinessException(ErrorCode errorCode,String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
}