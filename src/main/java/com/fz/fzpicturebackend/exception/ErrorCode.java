package com.fz.fzpicturebackend.exception;

import lombok.Getter;

/**
 * @Author fang
 * @Date 2025/2/26 13:29
 * @注释 错误编码枚举类
 */
@Getter
public enum ErrorCode {
    SUCCESS(0,"ok"),
    PARAMS_ERROR(40000,"请求参数错误"),
    NOT_FOUND_ERROR(40400,"请求数据不存在"),
    SYSTEM_ERROR(50000,"系统内部异常"),
    OPERATION_ERROR(50000,"操作失败"),
    NOT_LOGIN_ERROR(40100,"未登录"),
    NO_AUTH_ERROR(40101,"无权限"),
    FORBIDDEN_ERROR(40301,"禁止访问");

    private final int code;
    /**
     * 状态码信息
     */
    private final String message;


    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
