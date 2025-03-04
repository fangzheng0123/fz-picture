package com.fz.fzpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author fang
 * @Date 2025/2/27 8:50
 * @注释  用户添加请求
 */
@Data
public class UserAddRequest implements Serializable {

    private static final long serialVersionUID = 7505153615784327464L;
    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色: user, admin
     */
    private String userRole;
    
}

