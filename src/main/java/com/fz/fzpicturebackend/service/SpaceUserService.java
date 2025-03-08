package com.fz.fzpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fz.fzpicturebackend.model.dto.space.SpaceAddRequest;
import com.fz.fzpicturebackend.model.dto.space.SpaceQueryRequest;
import com.fz.fzpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.fz.fzpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.fz.fzpicturebackend.model.entity.Space;
import com.fz.fzpicturebackend.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fz.fzpicturebackend.model.entity.User;
import com.fz.fzpicturebackend.model.vo.SpaceUserVO;
import com.fz.fzpicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author fang
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-03-08 15:12:50
*/
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 使用参数查询空间信息
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 封装space的vo类
     * @param spaceUser
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 获取空间 VOList 对象
     * @param spaceUserList  page 对象
     * @param request request 请求
     * @return 分页的 VO
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList, HttpServletRequest request);

    /**
     * 校验参数
     * @param spaceUser 需要校验的 space 对象
     * @param add 判断是不是新建
     */
    void validSpaceUser(SpaceUser spaceUser,Boolean add);


    /**
     * 新增空间(只有管理员才能操作)
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest, User loginUser);


}
