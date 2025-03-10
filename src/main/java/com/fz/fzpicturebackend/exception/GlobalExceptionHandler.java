package com.fz.fzpicturebackend.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.fz.fzpicturebackend.common.BaseResponse;
import com.fz.fzpicturebackend.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @Author fang
 * @Date 2025/2/26 13:55
 * @注释   全局异常处理类
 */
@SuppressWarnings({"all"})
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * @ExceptionHandler(BusinessException.class) 注解的作用就是只捕获BusinessException
     * 中出现的异常信息
     * @param e 捕获的异常信息
     * @return 异常返回结果
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse businessException(BusinessException e){
        log.error("businessException"+e.getMessage(),e);
        return ResultUtils.error(e.getCode(),e.getMessage());
    }


    /**
     *  系统运行全局异常
     * @param e
     * @return
     */
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse runtimeException(RuntimeException e){
        log.error("runtimeException",e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR,"系统内部错误");
    }

    @ExceptionHandler(NotLoginException.class)
    public BaseResponse<?> notLoginException(NotLoginException e) {
        log.error("NotLoginException", e);
        return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR, e.getMessage());
    }

    @ExceptionHandler(NotPermissionException.class)
    public BaseResponse<?> notPermissionExceptionHandler(NotPermissionException e) {
        log.error("NotPermissionException", e);
        return ResultUtils.error(ErrorCode.NO_AUTH_ERROR, e.getMessage());
    }

}
