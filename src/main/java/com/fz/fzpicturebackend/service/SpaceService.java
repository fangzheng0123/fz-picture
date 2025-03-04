package com.fz.fzpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fz.fzpicturebackend.model.dto.space.SpaceAddRequest;
import com.fz.fzpicturebackend.model.dto.space.SpaceQueryRequest;
import com.fz.fzpicturebackend.model.entity.Space;
import com.fz.fzpicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fz.fzpicturebackend.model.entity.User;
import com.fz.fzpicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author fang
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-03-04 09:04:03
*/
public interface SpaceService extends IService<Space> {
    /**
     * 使用参数查询空间信息
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 封装space的vo类
     * @param space
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 分页获取空间 VO 对象
     * @param spacePage  page 对象
     * @param request request 请求
     * @return 分页的 VO
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 校验参数
     * @param space 需要校验的 space 对象
     * @param add 判断是不是新建
     */
    void validSpace(Space space,Boolean add);


    /**
     * 新增空间(只有管理员才能操作)
     */
    long addSpace(SpaceAddRequest spaceAddRequest,User loginUser);

    /**
     * 根据传递的用户确定用户的最大容量和最大数量
     * @param space
     */
    void fillSpaceBySpaseLevel(Space space);

}
