package com.fz.fzpicturebackend.aop;

import com.fz.fzpicturebackend.annotation.AuthCheck;
import com.fz.fzpicturebackend.exception.BusinessException;
import com.fz.fzpicturebackend.exception.ErrorCode;
import com.fz.fzpicturebackend.exception.ThrowUtils;
import com.fz.fzpicturebackend.model.entity.User;
import com.fz.fzpicturebackend.model.enums.UserRoleEnum;
import com.fz.fzpicturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @Author fang
 * @Date 2025/2/26 22:52
 * @注释  aop切面 判断用户是否是管理员
 */
@Aspect
@Component
public class AuthInterceptor {
    @Resource
    private UserService userService;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
//        这个信息就是注解中自己传递的角色信息
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
//        获取当前登录用户
        User loginUser = userService.getLoginUser(request);
//        获取传递过来的用户权限枚举信息  这个是自己定义需要的权限
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
//        如果没有传递数据就放行
        if (mustRoleEnum == null){
            return joinPoint.proceed();
        }
//        以下的代码：必须有权限才能运行
//        这个是当前登录用户的权限
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        ThrowUtils.throwIf(userRoleEnum == null, ErrorCode.NO_AUTH_ERROR);
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return joinPoint.proceed();
    }
}
