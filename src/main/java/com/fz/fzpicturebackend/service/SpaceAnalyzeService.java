package com.fz.fzpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fz.fzpicturebackend.model.dto.space.analyze.*;
import com.fz.fzpicturebackend.model.entity.Picture;
import com.fz.fzpicturebackend.model.entity.Space;
import com.fz.fzpicturebackend.model.entity.User;
import com.fz.fzpicturebackend.model.vo.space.*;

import java.util.List;

/**
* @author fang
* @createDate 2025-03-04 09:04:03
*/
public interface SpaceAnalyzeService extends IService<Space> {

    /**
     * 校验空间分析权限
     */
    void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User userLogin);

    /**
     * 补充查询条件
     */
    void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper);

    /**
     * 获取空间分析数据
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User userLogin);
    /**
     * 获取空间图片分类
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User userLogin);

    /**
     * 获取图片标签统计信息
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User userLogin);

    /**
     * 获取空间大小分析
     *
     * @param spaceSizeAnalyzeRequest SpaceSizeAnalyzeRequests
     * @param loginUser               当前登录用户
     * @return List<SpaceSizeAnalyzeResponse>
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

    /**
     * 用户上传时间分析
     *
     * @param spaceUserAnalyzeRequest  spaceUserAnalyzeRequest
     * @param loginUser 当前登录的用户
     * @return List<SpaceUserAnalyzeResponse>
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    /**
     * 空间使用排行 仅管理员可用
     *
     * @param spaceRankAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);







}