package com.fz.fzpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fz.fzpicturebackend.model.dto.picture.*;
import com.fz.fzpicturebackend.model.dto.user.UserQueryRequest;
import com.fz.fzpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fz.fzpicturebackend.model.entity.User;
import com.fz.fzpicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
* @author fang
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-02-27 16:28:55
*/
public interface PictureService extends IService<Picture> {


    /**
     * 上传图片
     * @param inputSource 文件源信息
     * @param pictureUploadRequest 文件请求
     * @param loginUser 上传的用户
     * @return
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 使用参数查询图片信息
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 封装picture的vo类
     * @param picture
     * @return
     */
    PictureVO getPictureVO(Picture picture,HttpServletRequest request);

    /**
     * 分页获取图片 VO 对象
     * @param picturePage  page 对象
     * @param request request 请求
     * @return 分页的 VO
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 校验参数
     * @param picture 需要校验的 picture 对象
     */
    void validPicture(Picture picture);


    /**
     * 审核上传图片信息
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest,User loginUser);

    /**
     * 图片审核校验
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);


    /**
     * 批量抓取图片
     * @param pictureUploadByBatchRequest
     * @param loginuser
     * @return
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest,User loginuser);

    /**
     * 删除cos中的图片信息
     */
    void clearPictureFile(Picture oldPicture);

    /**
     * 校验图片权限
     * @param picture
     * @param loginUser
     */
    void checkPictureAuth(Picture picture,User loginUser);

    /**
     * 删除图片
     */
    boolean deletePicture(long id,User loginUser);

    /**
     * 普通用户修改图片
     */
    boolean editPicture(PictureEditRequest pictureEditRequest,User loginUser);
}
