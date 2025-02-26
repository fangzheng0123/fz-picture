package com.fz.fzpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fz.fzpicturebackend.constant.UserConstant;
import com.fz.fzpicturebackend.exception.ErrorCode;
import com.fz.fzpicturebackend.exception.ThrowUtils;
import com.fz.fzpicturebackend.model.dto.UserLoginRequest;
import com.fz.fzpicturebackend.model.dto.UserRegisterRequest;
import com.fz.fzpicturebackend.model.entity.User;
import com.fz.fzpicturebackend.model.enums.UserRoleEnum;
import com.fz.fzpicturebackend.model.vo.LoginUserVo;
import com.fz.fzpicturebackend.service.UserService;
import com.fz.fzpicturebackend.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;

/**
* @author fang
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-02-26 19:35:05
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    /**
     * 用户注册
     * @param userRegisterRequest 用户注册信息
     * @return
     */
    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        ThrowUtils.throwIf(ObjUtil.hasEmpty(userAccount,userPassword,checkPassword), ErrorCode.NOT_FOUND_ERROR,"请求数据存在空值");
        ThrowUtils.throwIf((userAccount.length()<4 || userPassword.length()<8 || checkPassword.length()<8),ErrorCode.PARAMS_ERROR,"账号长度不能小于4且密码长度不能小于8");
        ThrowUtils.throwIf(!userPassword.equals(checkPassword),ErrorCode.PARAMS_ERROR,"二次输入的密码不一致");

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        Long result = this.baseMapper.selectCount(queryWrapper);
        ThrowUtils.throwIf(result>0,ErrorCode.PARAMS_ERROR,"用户已存在");
//        加密用户密码
        String encryptPassword = getEncryptPassword(userPassword);
//        创建用户并存入数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean save = this.save(user);
        ThrowUtils.throwIf(!save,ErrorCode.SYSTEM_ERROR,"数据库存储错误");
        return user.getId();
    }

    /**
     * 用户登录
     * @param userLoginRequest 用户登录信息
     * @param request 可以获取登录态信息
     * @return
     */
    @Override
    public LoginUserVo userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount,userPassword),ErrorCode.PARAMS_ERROR,"请求数据存在空数据");
        ThrowUtils.throwIf((userAccount.length()<4 || userPassword.length()<8),ErrorCode.PARAMS_ERROR,"账号长度不能小于4且密码长度不能小于8");

        String encryptPassword = getEncryptPassword(userPassword);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        queryWrapper.eq("userPassword",encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        ThrowUtils.throwIf(user == null,ErrorCode.NOT_FOUND_ERROR,"账号名或密码不正确");
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE,user);

        return getLoginUserVo(user);
    }



    /**
     * 密码加密
     * @param userPassword 原始密码
     * @return
     */
    @Override
    public String getEncryptPassword(String userPassword){
//        加盐
        final String SALT = "fz";
        return DigestUtils.md5DigestAsHex((SALT+userPassword).getBytes());
    }


    /**
     * 获取返回前端的用户信息
     * @param user 完整的用户信息
     * @return
     */
    @Override
    public LoginUserVo getLoginUserVo(User user) {
        if (user == null){
            return null;
        }
        LoginUserVo loginUserVo = new LoginUserVo();
        BeanUtil.copyProperties(user,loginUserVo);
        return loginUserVo;
    }
}




