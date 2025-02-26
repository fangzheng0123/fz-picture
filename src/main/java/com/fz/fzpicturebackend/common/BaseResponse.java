package com.fz.fzpicturebackend.common;

import com.fz.fzpicturebackend.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author fang
 * @Date 2025/2/26 13:46
 * @注释  通用的返回类
 */
@Data
public class BaseResponse<T> implements Serializable {
    private static final long serialVersionUID = -5516655821654757600L;
    private int code;
    private T data;
    private String msg;
    public BaseResponse(int code, T data, String msg) {
        this.code = code;
        this.data = data;
        this.msg = msg;
    }

    public BaseResponse(int code, T data) {
        this(code,data,"");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(),null,errorCode.getMessage());
    }

}
