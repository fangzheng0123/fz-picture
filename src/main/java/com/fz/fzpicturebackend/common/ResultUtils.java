package com.fz.fzpicturebackend.common;

import com.fz.fzpicturebackend.exception.ErrorCode;

/**
 * @Author fang
 * @Date 2025/2/26 13:49
 * @注释  统一封装返回类
 */
public class ResultUtils {

    //    成功返回值
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "ok");
    }

    /**
     *
     * @param errorCode 错误信息
     * @param msg  错误消息
     * @return
     */
    public static BaseResponse error(ErrorCode errorCode,String msg) {
        return new BaseResponse<>(errorCode.getCode(),null,msg);
    }

    /**
     *
     * @param errorCode 错误信息
     * @return
     */
    public static BaseResponse error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     *
     * @param code 传递的code编码信息
     * @return
     */
    public static BaseResponse error(int code,String msg) {
        return new BaseResponse<>(code,null,msg);
    }
}

