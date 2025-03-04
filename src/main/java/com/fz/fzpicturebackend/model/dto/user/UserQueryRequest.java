package com.fz.fzpicturebackend.model.dto.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fz.fzpicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @Author fang
 * @Date 2025/2/26 20:57
 * @注释  用户查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {


    private static final long serialVersionUID = 877277546165141823L;
    /**
     * id
     *
     */
    private Long id;
    /**
     * 用户昵称
     */
    private String userName;
    /**
     * 用户账号
     */
    private String userAccount;
    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;


}
