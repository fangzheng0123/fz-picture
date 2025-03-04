package com.fz.fzpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fz.fzpicturebackend.model.dto.user.UserLoginRequest;
import com.fz.fzpicturebackend.model.dto.user.UserQueryRequest;
import com.fz.fzpicturebackend.model.dto.user.UserRegisterRequest;
import com.fz.fzpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fz.fzpicturebackend.model.vo.LoginUserVo;
import com.fz.fzpicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author fang
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-02-26 19:35:05
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param userRegisterRequest 用户注册信息
     * @return 用户的id
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 密码加密
     * @param userPassword 原始密码
     * @return
     */
    String getEncryptPassword(String userPassword);

    /**
     * 用户登录
     * @param userLoginRequest 用户登录信息
     * @param request 可以获取登录态信息
     * @return 用户脱敏信息
     */
    LoginUserVo userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

    /**
     * 根据原始用户信息获取返回前端的用户信息
     * @param user
     * @return
     */
    LoginUserVo getLoginUserVo(User user);

    /**
     * 给普通用户返回用户信息
     * @param user
     * @return
     */
    UserVO getUserVo(User user);

    /**
     * 给普通用户返回用户信息列表
     * @param userList 用户信息列表
     * @return
     */
    List<UserVO> getUserListVo(List<User> userList);

    /**
     * 获取当前登录用户信息，该结果只在后端传输
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销退出登录
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 使用参数查询用户信息
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 判断是否是管理员
     * @param user
     * @return
     */
    boolean isAdmin(User user);
}
