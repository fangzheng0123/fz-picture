package com.fz.fzpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fz.fzpicturebackend.exception.BusinessException;
import com.fz.fzpicturebackend.exception.ErrorCode;
import com.fz.fzpicturebackend.exception.ThrowUtils;
import com.fz.fzpicturebackend.manager.CosManager;
import com.fz.fzpicturebackend.manager.upload.FilePictureUpload;
import com.fz.fzpicturebackend.manager.upload.PictureUploadTemplate;
import com.fz.fzpicturebackend.manager.upload.UrlPictureUpload;
import com.fz.fzpicturebackend.mapper.PictureMapper;
import com.fz.fzpicturebackend.model.dto.file.UploadPictureResult;
import com.fz.fzpicturebackend.model.dto.picture.PictureQueryRequest;
import com.fz.fzpicturebackend.model.dto.picture.PictureReviewRequest;
import com.fz.fzpicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.fz.fzpicturebackend.model.dto.picture.PictureUploadRequest;
import com.fz.fzpicturebackend.model.entity.Picture;
import com.fz.fzpicturebackend.model.entity.User;
import com.fz.fzpicturebackend.model.enums.PictureReviewStatusEnum;
import com.fz.fzpicturebackend.model.vo.PictureVO;
import com.fz.fzpicturebackend.model.vo.UserVO;
import com.fz.fzpicturebackend.service.PictureService;
import com.fz.fzpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author fang
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-02-27 16:28:55
*/
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{
    @Resource
    private UserService userService;
    @Resource
    private FilePictureUpload filePictureUpload;
    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Resource
    private CosManager cosManager;
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
//        校验参数
        ThrowUtils.throwIf(inputSource == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null,ErrorCode.NOT_LOGIN_ERROR);
//        判断是新增还是删除
        Long pictureId = null;
        if (pictureUploadRequest != null){
            pictureId = pictureUploadRequest.getId();
        }
//        如果是更新，判断图片是否存在
        if (pictureId != null){
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null,ErrorCode.PARAMS_ERROR);
//            不是本人或者不是管理员就不能更新图片信息
            if (!oldPicture.getUserId().equals(loginUser.getId()) || !userService.isAdmin(loginUser)){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
//        上传图片，得到图片信息
        String uploadPathPrefix = String.format("public/%s",loginUser.getId());
//        根据传递的不同信息来决定使用不同的文件上传类型
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String){
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        String picName = null;
        if (pictureUploadRequest != null) {
            picName = uploadPictureResult.getPicName();
            if (inputSource instanceof String){
                picName = pictureUploadRequest.getPicName();
            }
        }
        if (picName == null){
            picName = "url上传文件";
        }
        uploadPictureResult.setPicName(picName);
        Picture picture = getPicture(loginUser, uploadPictureResult, pictureId);
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"操作数据库失败");
        return PictureVO.objToVo(picture);
    }

    /**
     * 使用参数查询图片信息
     * @param pictureQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "picFormat", reviewMessage);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId),"reviewerId",reviewerId);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus),"reviewStatus",reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField),sortOrder.equals("ascend"),sortField);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public PictureVO getPictureVO(Picture picture,HttpServletRequest request) {
        if (picture == null){
            return null;
        }
        PictureVO pictureVO = PictureVO.objToVo(picture);
        Long userId = pictureVO.getUserId();
        if (userId != null && userId>0){
            User user = userService.getById(userId);
            UserVO userVo = userService.getUserVo(user);
            pictureVO.setUser(userVo);
        }
        return pictureVO;
    }

    /**
     * 分页获取图片封装
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVo(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        ThrowUtils.throwIf(pictureReviewRequest == null,ErrorCode.PARAMS_ERROR);
        Long userId = loginUser.getId();
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum enumByValue = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || enumByValue == null || PictureReviewStatusEnum.REVIEWING.equals(enumByValue)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
//        判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null,ErrorCode.NOT_FOUND_ERROR);
//        检验审核状态是否重复
        if (oldPicture.getReviewStatus().equals(reviewStatus)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请勿重复审核");
        }
//        数据库操作
        Picture updatePicture = new Picture();
        BeanUtil.copyProperties(oldPicture,updatePicture);
//        ReviewerId是审核人信息，UserId是上传图片的人id
        updatePicture.setReviewerId(userId);
        updatePicture.setReviewStatus(reviewStatus);
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"数据库操作失败");
    }

    private Picture getPicture(User loginUser, UploadPictureResult uploadPictureResult, Long pictureId) {
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        String picName = uploadPictureResult.getPicName();
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
//        操作数据库
        if (pictureId != null){
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
//        补充审核参数
        this.fillReviewParams(picture,loginUser);
        return picture;
    }

    /**
     * 图片审核
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser){
        if (userService.isAdmin(loginUser)){
            picture.setReviewerId(loginUser.getId());
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewTime(new Date());
            picture.setReviewMessage("管理员自动审核通过");
        }else {
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    /**
     * 批量抓取图片
     * @param pictureUploadByBatchRequest
     * @param loginuser
     * @return
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginuser) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null,ErrorCode.PARAMS_ERROR);
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)){
            namePrefix = searchText;
        }
//        抓取数量不能超过30
        ThrowUtils.throwIf(count > 30,ErrorCode.PARAMS_ERROR,"抓取数量不能超过三十");
//        抓取内容
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
//            获取界面
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败",e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"获取页面失败");
        }
//        解析内容,获取元素失败
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isEmpty(div)){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
//        遍历元素，依次处理上传图片
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
//            获取到的url路径
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)){
                log.info("当前链接为空，已跳过，{}",fileUrl);
                continue;
            }
//            处理图片的地址，防止转义时和对象存储冲突
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1){
                fileUrl = fileUrl.substring(0,questionMarkIndex);
            }
//            上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            pictureUploadRequest.setPicName(namePrefix+(uploadCount+1));
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginuser);
                log.info("图片上传成功，id = {}",pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败",e);
                continue;
            }
            if (uploadCount >= count){
                break;
            }
        }
        return uploadCount;
    }

    @Override
    public void clearPictureFile(Picture oldPicture) {
//        判断该图片是否被多个用户使用
        String pictureUrl = oldPicture.getUrl();
        Long count = this.lambdaQuery().eq(Picture::getUrl, pictureUrl).count();
        if (count>1){
            return;
        }
        cosManager.deleteObject(pictureUrl);
//        删除缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        count = this.lambdaQuery().eq(Picture::getThumbnailUrl,thumbnailUrl).count();
        if (count>1){
            return;
        }
        cosManager.deleteObject(thumbnailUrl);
    }
}




