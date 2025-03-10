package com.fz.fzpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fz.fzpicturebackend.api.aliyunapi.AliYunAiApi;
import com.fz.fzpicturebackend.api.aliyunapi.model.CreateOutPaintingTaskRequest;
import com.fz.fzpicturebackend.api.aliyunapi.model.CreateOutPaintingTaskResponse;
import com.fz.fzpicturebackend.exception.BusinessException;
import com.fz.fzpicturebackend.exception.ErrorCode;
import com.fz.fzpicturebackend.exception.ThrowUtils;
import com.fz.fzpicturebackend.manager.CosManager;
import com.fz.fzpicturebackend.manager.upload.FilePictureUpload;
import com.fz.fzpicturebackend.manager.upload.PictureUploadTemplate;
import com.fz.fzpicturebackend.manager.upload.UrlPictureUpload;
import com.fz.fzpicturebackend.mapper.PictureMapper;
import com.fz.fzpicturebackend.model.dto.file.UploadPictureResult;
import com.fz.fzpicturebackend.model.dto.picture.*;
import com.fz.fzpicturebackend.model.entity.Picture;
import com.fz.fzpicturebackend.model.entity.Space;
import com.fz.fzpicturebackend.model.entity.User;
import com.fz.fzpicturebackend.model.enums.PictureReviewStatusEnum;
import com.fz.fzpicturebackend.model.enums.SpaceLevelEnum;
import com.fz.fzpicturebackend.model.vo.PictureVO;
import com.fz.fzpicturebackend.model.vo.UserVO;
import com.fz.fzpicturebackend.service.PictureService;
import com.fz.fzpicturebackend.service.SpaceService;
import com.fz.fzpicturebackend.service.UserService;
import com.fz.fzpicturebackend.utils.ColorSimilarUtils;
import com.fz.fzpicturebackend.utils.ColorTransformUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
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
    private SpaceService spaceService;
    @Resource
    private CosManager cosManager;

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Resource
    private TransactionTemplate transactionTemplate;
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

        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null){
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null,ErrorCode.PARAMS_ERROR);
//            检验是否有空间权限
            if (!loginUser.getId().equals(space.getUserId())){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            if (space.getTotalSize() >= space.getMaxSize()){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"存储空间不足");
            }
            if (space.getTotalCount() >= space.getMaxCount()){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"存储数量到达上限");
            }
        }
//        如果是更新，判断图片是否存在
        if (pictureId != null){
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null,ErrorCode.PARAMS_ERROR);
//            不是本人或者不是管理员就不能更新图片信息
            if (!oldPicture.getUserId().equals(loginUser.getId()) || !userService.isAdmin(loginUser)){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
//            校验空间是否一致  在更新操作中，如果没有传递新的spaceId就复用老的
            if (spaceId == null){
                if (oldPicture.getSpaceId() != null){
                    spaceId = oldPicture.getSpaceId();
                }
            }else {
                ThrowUtils.throwIf(ObjUtil.notEqual(spaceId,oldPicture.getSpaceId()),ErrorCode.NO_AUTH_ERROR);
            }
        }
//        上传图片，得到图片信息 区分公共图库和私有图库
        String uploadPathPrefix;
        if (spaceId == null){
            uploadPathPrefix = String.format("public/%s",loginUser.getId());
        }else {
            uploadPathPrefix = String.format("space/%s",spaceId);
        }
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
        picture.setSpaceId(spaceId);
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"操作数据库失败");
//            更新空间的使用额度
            if (Objects.nonNull(finalSpaceId)){
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update,ErrorCode.OPERATION_ERROR,"额度更新失败");
            }
            return picture;
        });
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
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
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
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
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
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime),"editTime",startEditTime);
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime),"editTime",endEditTime);
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
        picture.setPicColor(ColorTransformUtils.colorTransform(uploadPictureResult.getPicColor()));
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


    /**
     * 校验权限
     * @param picture
     * @param loginUser
     */
    @Override
    public void checkPictureAuth(Picture picture, User loginUser) {
        Long spaceId = picture.getSpaceId();
        Long userId = loginUser.getId();
//        如果spaceId为空就表示为公共图库
        if (spaceId == null){
//            仅管理员和本人能操作
            if (!picture.getUserId().equals(userId) && !userService.isAdmin(loginUser)){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }else {
            if (!picture.getUserId().equals(userId)){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    @Override
    public boolean deletePicture(long id, User loginUser) {
        ThrowUtils.throwIf(id<=0,ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null,ErrorCode.PARAMS_ERROR);
        // 判断是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
//        检验权限
//        更新为鉴权模式
//        this.checkPictureAuth(oldPicture,loginUser);
        Long pictureId = oldPicture.getSpaceId();
        transactionTemplate.execute(status -> {
            boolean result = this.removeById(id);
            ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"操作数据库失败");
//            更新空间的使用额度
            if (Objects.nonNull(pictureId)){
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, pictureId)
                        .setSql("totalSize = totalSize + " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update,ErrorCode.OPERATION_ERROR,"额度更新失败");
            }
            return oldPicture;
        });
        // 操作数据库
        return true;
    }

    @Override
    public boolean editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        picture.setIntroduction(pictureEditRequest.getIntroduction());
        this.validPicture(picture);
//        图片审核参数
        this.fillReviewParams(picture,loginUser);
        // 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
//        校验权限
//        更新为鉴权模式
//        this.checkPictureAuth(oldPicture,loginUser);
        this.saveOrUpdate(picture);
        return true;
    }


    /**
     *  根据颜色搜索图片
     * @param spaceId
     * @param color
     * @param userLogin
     * @return
     */
    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String color, User userLogin) {
//        1、校验参数
        ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userLogin == null, ErrorCode.PARAMS_ERROR);
//        2、校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!space.getUserId().equals(userLogin.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
//        3、查询空间下的所有图片（必须存在主色调）
        List<Picture> pictureList = this.lambdaQuery().eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
//        如果没有图片，那就返回空列表
        if (CollUtil.isEmpty(pictureList)) {
            return new ArrayList<>();
        }
//        将颜色字符串转变成主色调
        Color targetColor = Color.decode(color);
//        4、计算相似度排序
        List<Picture> sortedPictureList = pictureList.stream().sorted(
                Comparator.comparingDouble(picture -> {
                            String hexColor = picture.getPicColor();
                            if (hexColor == null) {
                                return Double.MAX_VALUE;
                            }
                            Color pictureColor = Color.decode(hexColor);
//                    计算相似度
//                    越大越相似
                            return ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);
                        }
                )
        ).limit(12).collect(Collectors.toList());
//        5、返回结果
        return sortedPictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
    }

    @Override
    public void editPictureBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
//        获取和校验参数
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        List<String> tags = pictureEditByBatchRequest.getTags();
        String category = pictureEditByBatchRequest.getCategory();
        if (CollUtil.isEmpty(pictureIdList) || spaceId == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (CollUtil.isEmpty(tags) && StringUtils.isBlank(category)){
            return;
        }
//        校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
//        if (!space.getUserId().equals(loginUser.getId())){
//            ThrowUtils.throwIf(true,ErrorCode.NO_AUTH_ERROR);
//        }
//        查询指定图片（仅选择需要的字段）
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId,Picture::getSpaceId)
                .in(Picture::getId,pictureIdList)
                .eq(Picture::getSpaceId,spaceId)
                .list();
        if (CollUtil.isEmpty(pictureList)){
            return;
        }
//        自定义一个锁来避免并发问题
        transactionTemplate.execute(stauts -> {
            //        更新分类和标签
            pictureList.forEach(picture -> {
                if (CollUtil.isNotEmpty(tags)) {
                    picture.setTags(JSONUtil.toJsonStr(tags));
                }
                if (StringUtils.isNotBlank(category)) {
                    picture.setCategory(category);
                }
            });
//            更新命名规则
            String nameRole = pictureEditByBatchRequest.getNameRule();
            fillPictureWithNameRole(pictureList,nameRole);
//        批量更新
            boolean result = this.updateBatchById(pictureList);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            return result;
        });
    }

    /**
     *
     * 格式 ：图片{序号}
     *
     * 根据命名规则更新图片名称
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRole(List<Picture> pictureList, String nameRule) {
        if (StringUtils.isBlank(nameRule) || CollUtil.isEmpty(pictureList)){
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            log.error("批量更新图片名称出错",e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"批量更新图片名称出错");
        }
    }


    /**
     * AI 扩图
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     */

    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        // 获取图片信息
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = Optional.ofNullable(this.getById(pictureId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR));
        // 权限校验
        //        更新为鉴权模式
//        checkPictureAuth(picture, loginUser);
        // 构造请求参数
        CreateOutPaintingTaskRequest taskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        taskRequest.setInput(input);
        BeanUtil.copyProperties(createPictureOutPaintingTaskRequest, taskRequest);
        // 创建任务
        return aliYunAiApi.createOutPaintingTask(taskRequest);
    }

}




