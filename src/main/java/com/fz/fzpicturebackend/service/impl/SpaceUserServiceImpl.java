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
import com.fz.fzpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.fz.fzpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.fz.fzpicturebackend.model.entity.Space;
import com.fz.fzpicturebackend.model.entity.SpaceUser;
import com.fz.fzpicturebackend.model.entity.User;
import com.fz.fzpicturebackend.model.enums.SpaceLevelEnum;
import com.fz.fzpicturebackend.model.enums.SpaceRoleEnum;
import com.fz.fzpicturebackend.model.enums.SpaceTypeEnum;
import com.fz.fzpicturebackend.model.vo.SpaceUserVO;
import com.fz.fzpicturebackend.model.vo.SpaceVO;
import com.fz.fzpicturebackend.model.vo.UserVO;
import com.fz.fzpicturebackend.service.SpaceService;
import com.fz.fzpicturebackend.service.SpaceUserService;
import com.fz.fzpicturebackend.mapper.SpaceUserMapper;
import com.fz.fzpicturebackend.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author fang
* @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
* @createDate 2025-03-08 15:12:50
*/
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
    implements SpaceUserService{

    @Resource
    private UserService userService;
    @Resource
    @Lazy
    private SpaceService spaceService;

    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();

        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole);
        // 排序
        return queryWrapper;
    }

    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {
        if (spaceUser == null){
            return null;
        }
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        Long userId = spaceUserVO.getUserId();
        if (userId != null && userId>0){
            User user = userService.getById(userId);
            UserVO userVo = userService.getUserVo(user);
            spaceUserVO.setUser(userVo);
        }
        Long spaceId = spaceUserVO.getSpaceId();
        if (spaceId != null && spaceId > 0){
            Space space = spaceService.getById(spaceId);
            SpaceVO spaceVO = spaceService.getSpaceVO(space,request);
            spaceUserVO.setSpace(spaceVO);
        }
        return spaceUserVO;
    }

    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList, HttpServletRequest request) {
        if (CollUtil.isEmpty(spaceUserList)) {
            return new ArrayList<>();
        }
        // 对象列表 => 封装对象列表
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream().map(SpaceUserVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = spaceUserVOList.stream().map(SpaceUserVO::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIdSet= spaceUserVOList.stream().map(SpaceUserVO::getSpaceId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spaceIdSpaceListMap = spaceService.listByIds(spaceIdSet).stream().collect(Collectors.groupingBy(Space::getId));
        // 2. 填充信息
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            Long spaceId = spaceUserVO.getSpaceId();
            User user = null;
            Space space = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            if (spaceIdSpaceListMap.containsKey(spaceId)) {
                space = spaceIdSpaceListMap.get(spaceId).get(0);
            }
            spaceUserVO.setUser(userService.getUserVo(user));
            spaceUserVO.setSpace(SpaceVO.objToVo(space));
        });
        return spaceUserVOList;
    }

    @Override
    public void validSpaceUser(SpaceUser spaceUser, Boolean add) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        if (add){
            ThrowUtils.throwIf(spaceId == null,ErrorCode.PARAMS_ERROR,"空间不存在");
            ThrowUtils.throwIf(userId == null,ErrorCode.PARAMS_ERROR,"用户不存在");
            ThrowUtils.throwIf(userService.getById(userId) == null,ErrorCode.PARAMS_ERROR,"用户不存在");
            ThrowUtils.throwIf(spaceService.getById(spaceId) == null,ErrorCode.PARAMS_ERROR,"空间不存在");
        }
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum enumByValue = SpaceRoleEnum.getEnumByValue(spaceRole);
        ThrowUtils.throwIf(spaceRole != null && enumByValue == null,ErrorCode.PARAMS_ERROR,"空间角色不能为空");
    }

    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUserAddRequest == null,ErrorCode.PARAMS_ERROR);
        SpaceUser spaceUser = new SpaceUser();
        BeanUtil.copyProperties(spaceUserAddRequest,spaceUser);
        validSpaceUser(spaceUser,true);
//        进行数据库操作
        boolean save = this.save(spaceUser);
        ThrowUtils.throwIf(!save,ErrorCode.OPERATION_ERROR);
        return spaceUser.getId();
    }
}




