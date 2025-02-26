package com.fz.fzpicturebackend.exception;

/**
 * @Author fang
 * @Date 2025/2/26 13:41
 * @注释  异常处理封装类
 */
public class ThrowUtils {
    /**
     * 条件成立立即抛异常
     * @param condition 条件
     * @param runtimeException 异常
     */
    public static void throwIf(boolean condition,RuntimeException runtimeException){
        if (condition){
            throw runtimeException;
        }
    }
    /**
     * 条件成立立即抛异常
     * @param condition 条件
     * @param errorCode 错误码
     */
    public static void throwIf(boolean condition,ErrorCode errorCode){
        throwIf(condition,new BusinessException(errorCode));
    }

    /**
     * 条件成立立即抛异常
     * @param condition 条件
     * @param errorCode 错误码
     * @param message 错误描述信息
     */
    public static void throwIf(boolean condition,ErrorCode errorCode,String message){
        throwIf(condition,new BusinessException(errorCode,message));
    }

}
