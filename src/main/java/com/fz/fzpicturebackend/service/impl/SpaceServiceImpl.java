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
import com.fz.fzpicturebackend.model.dto.picture.CreatePictureOutPaintingTaskRequest;
import com.fz.fzpicturebackend.model.dto.space.SpaceAddRequest;
import com.fz.fzpicturebackend.model.dto.space.SpaceQueryRequest;
import com.fz.fzpicturebackend.model.entity.*;
import com.fz.fzpicturebackend.model.entity.Space;
import com.fz.fzpicturebackend.model.entity.Space;
import com.fz.fzpicturebackend.model.enums.SpaceLevelEnum;
import com.fz.fzpicturebackend.model.vo.SpaceVO;
import com.fz.fzpicturebackend.model.vo.SpaceVO;
import com.fz.fzpicturebackend.model.vo.SpaceVO;
import com.fz.fzpicturebackend.model.vo.UserVO;
import com.fz.fzpicturebackend.service.PictureService;
import com.fz.fzpicturebackend.service.SpaceService;
import com.fz.fzpicturebackend.mapper.SpaceMapper;
import com.fz.fzpicturebackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author fang
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-03-04 09:04:03
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{
    
    
    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private PictureService pictureService;

    /**
     * 通用查询
     * @param spaceQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();

        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel),"spaceLevel",spaceLevel);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 获取空间信息传递给前端
     * @param space
     * @param request
     * @return
     */
    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        if (space == null){
            return null;
        }
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        Long userId = spaceVO.getUserId();
        if (userId != null && userId>0){
            User user = userService.getById(userId);
            UserVO userVo = userService.getUserVo(user);
            spaceVO.setUser(userVo);
        }
        return spaceVO;
    }

    /**
     * 获取分页的信息，传递给前端
     * @param spacePage  page 对象
     * @param request request 请求
     * @return
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVo(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    /**
     * 校验传递的数据信息
     * @param space 需要校验的 space 对象
     * @param add 判断是不是新建
     */
    @Override
    public void validSpace(Space space,Boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);

        if (add){
            ThrowUtils.throwIf(spaceLevel == null,ErrorCode.PARAMS_ERROR,"空间等级不能为空");
            ThrowUtils.throwIf(StrUtil.isBlank(spaceName),ErrorCode.PARAMS_ERROR,"空间名称不能为空");
        }

        // 修改数据时，id 不能为空，有参数则校验
        if (StrUtil.isNotBlank(spaceName)) {
            ThrowUtils.throwIf(spaceName.length() > 30, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间级别不存在");
        }
    }

    /**
     * 新增空间
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        ThrowUtils.throwIf(spaceAddRequest == null,ErrorCode.PARAMS_ERROR);
        Space space = new Space();
        BeanUtil.copyProperties(spaceAddRequest,space);
        String spaceName = spaceAddRequest.getSpaceName();
        if (StrUtil.isBlank(spaceName)){
            space.setSpaceName("默认空间");
        }
        Integer spaceLevel = spaceAddRequest.getSpaceLevel();
        if (ObjUtil.isEmpty(spaceLevel)){
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        fillSpaceBySpaseLevel(space);
        validSpace(space,true);
//        校验权限，非管理员只能创建普通级别的空间
        Long userId = loginUser.getId();
        space.setUserId(userId);
        if (SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel() && !userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"无权限创建指定级别的空间");
        }
//        控制同一个用户只能创建一个空间
        String lock = String.valueOf(userId).intern();
//        每个用户一把锁
        synchronized (lock){
//            代替@Transactional注解，注解在方法上表示完成整个方法
            Long execute = transactionTemplate.execute(status -> {
//                判断该用户是否有空间
                boolean exists = this.lambdaQuery().eq(Space::getUserId, userId).exists();
                ThrowUtils.throwIf(exists,ErrorCode.PARAMS_ERROR,"每个用户仅能有一个空间");
//                创建
                boolean save = this.save(space);
                ThrowUtils.throwIf(!save,ErrorCode.PARAMS_ERROR,"创建空间失败");
                return space.getId();
            });
            return Optional.ofNullable(execute).orElse(-1L);
        }
    }

    @Override
    public void fillSpaceBySpaseLevel(Space space) {
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        if (spaceLevelEnum != null){
            long maxSize = spaceLevelEnum.getMaxSize();
//            如果传递过来的size是空，那就传值，不为空就以管理员规定的为主
            if (space.getMaxSize() == null){
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null){
                space.setMaxCount(maxCount);
            }
        }
    }


}




