package com.fz.fzpicturebackend.controller;

import com.fz.fzpicturebackend.common.BaseResponse;
import com.fz.fzpicturebackend.common.ResultUtils;
import com.fz.fzpicturebackend.exception.ErrorCode;
import com.fz.fzpicturebackend.exception.ThrowUtils;
import com.fz.fzpicturebackend.model.dto.UserLoginRequest;
import com.fz.fzpicturebackend.model.dto.UserRegisterRequest;
import com.fz.fzpicturebackend.model.vo.LoginUserVo;
import com.fz.fzpicturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @Author fang
 * @Date 2025/2/26 14:10
 * @注释
 */

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest){
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR,"请求数据为空");
        long result = userService.userRegister(userRegisterRequest);
        return ResultUtils.success(result);
    }

    @PostMapping("/login")
    public BaseResponse<LoginUserVo> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR,"请求数据为空");
        LoginUserVo loginUserVo = userService.userLogin(userLoginRequest, request);
        return ResultUtils.success(loginUserVo);
    }
}
