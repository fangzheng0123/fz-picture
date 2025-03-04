package com.fz.fzpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author fang
 * @Date 2025/2/27 8:50
 * @注释 用户修改请求
 */
@Data
public class UserUpdateRequest implements Serializable {

    private static final long serialVersionUID = 778151260831713581L;
    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

}


