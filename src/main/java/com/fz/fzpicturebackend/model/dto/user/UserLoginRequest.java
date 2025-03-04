package com.fz.fzpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author fang
 * @Date 2025/2/26 20:57
 * @注释  用户注册请求
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = -3846876727412210575L;
    /**
     * 用户账号
     */
    private String userAccount;
    /**
     * 用户密码
     */
    private String userPassword;

}
