package com.fz.fzpicturebackend.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fz.fzpicturebackend.annotation.AuthCheck;
import com.fz.fzpicturebackend.api.aliyunapi.AliYunAiApi;
import com.fz.fzpicturebackend.api.aliyunapi.model.CreateOutPaintingTaskRequest;
import com.fz.fzpicturebackend.api.aliyunapi.model.CreateOutPaintingTaskResponse;
import com.fz.fzpicturebackend.api.aliyunapi.model.GetOutPaintingTaskResponse;
import com.fz.fzpicturebackend.api.imagesearch.ImageSearchApiFacade;
import com.fz.fzpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.fz.fzpicturebackend.common.BaseResponse;
import com.fz.fzpicturebackend.common.DeleteRequest;
import com.fz.fzpicturebackend.common.ResultUtils;
import com.fz.fzpicturebackend.constant.UserConstant;
import com.fz.fzpicturebackend.exception.BusinessException;
import com.fz.fzpicturebackend.exception.ErrorCode;
import com.fz.fzpicturebackend.exception.ThrowUtils;
import com.fz.fzpicturebackend.manager.auth.SpaceUserAuthContext;
import com.fz.fzpicturebackend.manager.auth.SpaceUserAuthManager;
import com.fz.fzpicturebackend.manager.auth.StpKit;
import com.fz.fzpicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.fz.fzpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.fz.fzpicturebackend.model.dto.picture.*;
import com.fz.fzpicturebackend.model.entity.Picture;
import com.fz.fzpicturebackend.model.entity.Space;
import com.fz.fzpicturebackend.model.entity.User;
import com.fz.fzpicturebackend.model.enums.PictureReviewStatusEnum;
import com.fz.fzpicturebackend.model.vo.PictureTagCategory;
import com.fz.fzpicturebackend.model.vo.PictureVO;
import com.fz.fzpicturebackend.service.PictureService;
import com.fz.fzpicturebackend.service.SpaceService;
import com.fz.fzpicturebackend.service.UserService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.fz.fzpicturebackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @Author fang
 * @Date 2025/2/28 15:36
 * @注释
 */
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private UserService userService;
    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;
    @Resource
    private AliYunAiApi aliYunAiApi;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

//    本地缓存的配置信息
    private final Cache<String,String> LOCAL_CACHE = Caffeine.newBuilder()
//            初始容量
            .initialCapacity(1024)
//            最大数量
            .maximumSize(10_000L)
//            设置过期时间
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    /**
     * 根据本地文件上传图片
     * @param multipartFile
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart MultipartFile multipartFile
            , PictureUploadRequest pictureUploadRequest
            , HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 根据url上传图片
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest
            , HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        boolean result = pictureService.deletePicture(id,loginUser);
//        pictureService.clearPictureFile(oldPicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest
            ,HttpServletRequest request) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 数据校验
        pictureService.validPicture(picture);
        // 判断是否存在
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
//        pictureService.clearPictureFile(oldPicture);
        // 操作数据库
        User loginUser = userService.getLoginUser(request);
//        图片审核参数
        pictureService.fillReviewParams(picture,loginUser);
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取图片（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(picture);
    }

    /**
     * 根据 id 获取图片（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 空间的图片，需要校验权限
        Space space = null;
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            boolean result = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!result, ErrorCode.NO_AUTH_ERROR);
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 获取权限列表
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        PictureVO pictureVO = pictureService.getPictureVO(picture, request);
        pictureVO.setPermissionList(permissionList);
        // 获取封装类
        return ResultUtils.success(pictureVO);
    }


    /**
     * 分页获取图片列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        // 公开图库
        if (spaceId == null) {
            // 普通用户默认只能查看已过审的公开数据
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
        }
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }

    /**
     * 编辑图片（给用户使用）
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        // 操作数据库
        boolean result = pictureService.editPicture(pictureEditRequest,loginUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 前端标签展示
     * @return
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 图片审核
     * @param pictureReviewRequest
     * @param request
     * @return
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(
            @RequestBody PictureReviewRequest pictureReviewRequest
            , HttpServletRequest request){
        ThrowUtils.throwIf(pictureReviewRequest == null,ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest,loginUser);
        return ResultUtils.success(true);
    }

    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest
            , HttpServletRequest request){
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null,ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Integer uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }


    //包含redis操作

    /**
     * 分页获取图片列表（封装类）
     */
    @Deprecated
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//        普通用户默认只能看到已通过审核的图片
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        /**
         * 这里先查询redis，之后再查询数据库信息
         */
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
//        构建缓存的key
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
//        创建一个缓存的key
        String cacheKey = String.format("fzpicture:listPictureVOByPage:%s",hashKey);
        /**
         * 使用本地缓存
         */
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);

        if (cachedValue != null){
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }
        //        操作redis
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        //        获取都redis的数据
        cachedValue = opsForValue.get(cacheKey);
        if (cachedValue != null){
//            分布式缓存命中直接更新本地缓存
            LOCAL_CACHE.put(cacheKey,cachedValue);
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
//        获取到数据之后直接存储到redis中
        String cacheValue = JSONUtil.toJsonStr(picturePage);
//        设置缓存时间，5-10分钟，不是固定时间是为了防止缓存雪崩
        int cacheExpireTime = 300 + RandomUtil.randomInt(0,300);
//        存本地
        LOCAL_CACHE.put(cacheKey,cacheValue);
//        存redis
        opsForValue.set(cacheKey,cacheValue,cacheExpireTime, TimeUnit.SECONDS);
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }


    /**
     * 以图搜图
     * @param searchPictureByPictureRequest
     * @return
     */
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(
            @RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest){
        ThrowUtils.throwIf(searchPictureByPictureRequest == null,ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(searchPictureByPictureRequest.getPictureId() == null,ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(searchPictureByPictureRequest.getPictureId());
        ThrowUtils.throwIf(picture == null,ErrorCode.PARAMS_ERROR);
        List<ImageSearchResult> imageSearchResults = ImageSearchApiFacade.searchImage(picture.getUrl());
        return ResultUtils.success(imageSearchResults);
    }

    /**
     * 以颜色搜图
     * @param searchPictureByColorRequest
     * @return
     */
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(
            @RequestBody SearchPictureByColorRequest searchPictureByColorRequest,HttpServletRequest request){
        ThrowUtils.throwIf(searchPictureByColorRequest == null,ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByColorRequest.getSpaceId();
        String picColor = searchPictureByColorRequest.getPicColor();
        User loginUser = userService.getLoginUser(request);
        List<PictureVO> pictureVOList = pictureService.searchPictureByColor(pictureId, picColor, loginUser);
        return ResultUtils.success(pictureVOList);
    }

    /**
     * 批量更新图片
     * @param pictureEditByBatchRequest
     * @return
     */
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(
            @RequestBody PictureEditByBatchRequest pictureEditByBatchRequest,HttpServletRequest request){
        ThrowUtils.throwIf(pictureEditByBatchRequest == null,ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }
    /**
     * 创建 AI 扩图任务
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
            @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            HttpServletRequest request) {
        if (createPictureOutPaintingTaskRequest == null || createPictureOutPaintingTaskRequest.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        CreateOutPaintingTaskResponse response = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 查询 AI 扩图任务
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(task);
    }


}
