package com.fz.fzpicturebackend.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 给普通用户返回的脱敏用户信息
 * @TableName user
 */
@Data
public class UserVO {
    /**
     * id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;


    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 创建时间
     */
    private Date createTime;


}