package com.fz.fzpicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fz.fzpicturebackend.exception.BusinessException;
import com.fz.fzpicturebackend.exception.ErrorCode;
import com.fz.fzpicturebackend.exception.ThrowUtils;
import com.fz.fzpicturebackend.mapper.SpaceMapper;
import com.fz.fzpicturebackend.model.dto.space.analyze.*;
import com.fz.fzpicturebackend.model.entity.Picture;
import com.fz.fzpicturebackend.model.entity.Space;
import com.fz.fzpicturebackend.model.entity.User;
import com.fz.fzpicturebackend.model.vo.space.*;
import com.fz.fzpicturebackend.service.PictureService;
import com.fz.fzpicturebackend.service.SpaceAnalyzeService;
import com.fz.fzpicturebackend.service.SpaceService;
import com.fz.fzpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author fang
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-03-04 09:04:03
 */
@Service
@Slf4j
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceAnalyzeService {


    @Resource
    private UserService userService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private PictureService pictureService;

    /**
     * 校验空间分析权限
     *
     * @param spaceAnalyzeRequest
     * @param userLogin
     */
    @Override
    public void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User userLogin) {
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
//        如果是查询所有的空间或者查询公有空间，那就只能管理员能查询
        if (queryAll || queryPublic) {
            boolean admin = userService.isAdmin(userLogin);
            ThrowUtils.throwIf(!admin, ErrorCode.NO_AUTH_ERROR);
        } else {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
//         查询私有空间，只能自己或者本人才能查看
            spaceService.checkSpaceAuth(space, userLogin);
        }
    }

    /**
     * 填充查询条件
     *
     * @param spaceAnalyzeRequest
     * @param queryWrapper
     */
    @Override
    public void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
//        如果是要查询全部
        if (queryAll) {
            return;
        }
        if (queryPublic) {
            queryWrapper.isNull("spaceId");
            return;
        }
//        只查询私有空间
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }

    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User userLogin) {
//        数据校验
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        boolean queryPublic = spaceUsageAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceUsageAnalyzeRequest.isQueryAll();
        SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = null;
//        全空间查询或公共图库查询 从picture查
        if (queryAll || queryPublic) {
//            校验用户是否为管理员
            boolean admin = userService.isAdmin(userLogin);
            ThrowUtils.throwIf(!admin, ErrorCode.NO_AUTH_ERROR);
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
//            补充查询条件
            fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest, queryWrapper);
//            查询指定信息
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
//            将获取的信息转换为Long类型并相加
            long usedCount = pictureObjList.stream().mapToLong(obj -> (Long) obj).sum();
            long usedSize = pictureObjList.size();
//            封装返回结果
            spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
        } else {
            //        特定空间查询
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
            //        权限校验
            this.checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, userLogin);
//            封装返回结果
            spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(space.getTotalSize());
            spaceUsageAnalyzeResponse.setUsedCount(space.getTotalCount());
            spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
            double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
            spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());
            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
        }
        return spaceUsageAnalyzeResponse;
    }

    /**
     * 获取图片分类分析包括图片分类，图片数量，分类图片总大小
     *
     * @param spaceCategoryAnalyzeRequest
     * @param userLogin
     * @return
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User userLogin) {
//        校验数据
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
//        检查权限
        this.checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, userLogin);
//        构造条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);
//        使用mybatis分组查询
        queryWrapper.select("category", "count(*) as count", "sum(picSize) as totalSize")
                .groupBy("category");
//        查询
        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(result -> {
                    String category = (String) result.get("category");
                    Long count = ((Number) result.get("count")).longValue();
                    Long totalSize = ((Number) result.get("totalSize")).longValue();
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                })
                .collect(Collectors.toList());
    }


    /**
     * 获取图片标签分析
     *
     * @param spaceTagAnalyzeRequest
     * @param userLogin
     * @return
     */
    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User userLogin) {
//        数据校验
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
//          权限校验
        this.checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, userLogin);
//        构建查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);
//        只查询标签
        queryWrapper.select("tags");
//        添加查询条件
        List<String> tagsJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                .collect(Collectors.toList());
// 合并所有标签并统计使用次数
        // 这个 flatMap 就是把 ["a", "b"] ["c", "d"] 转换为 ["a", "b", "c", "d"] 类似把他拍扁的感觉
        Map<String, Long> tagCountMap = tagsJsonList.stream()
                .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        // 转换为响应对象，按使用次数降序排序
        return tagCountMap.entrySet().stream()
                // 降序排列
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 获取空间大小分析
     *
     * @param spaceSizeAnalyzeRequest SpaceSizeAnalyzeRequests
     * @param loginUser               当前登录用户
     * @return List<SpaceSizeAnalyzeResponse>
     */
    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 检查权限
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);

        // 查询所有符合条件的图片大小
        queryWrapper.select("picSize");
        // 定义分段范围，注意使用有序 Map
        Map<String, Long> sizeRanges = new LinkedHashMap<>();
        pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .map(size -> ((Number) size).longValue())
                .forEach(picSize -> {
                    if (picSize < 100 * 1024) {
                        sizeRanges.put("<100KB", sizeRanges.getOrDefault("<100KB", 0L) + 1);
                    } else if (picSize < 500 * 1024) {
                        sizeRanges.put("100KB-500KB", sizeRanges.getOrDefault("100KB-500KB", 0L) + 1);
                    } else if (picSize < 1024 * 1024) {
                        sizeRanges.put("500KB-1MB", sizeRanges.getOrDefault("500KB-1MB", 0L) + 1);
                    } else {
                        sizeRanges.put(">1MB", sizeRanges.getOrDefault(">1MB", 0L) + 1);
                    }
                });
        // 转换为响应对象
        return sizeRanges.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 用户上传时间分析
     *
     * @param spaceUserAnalyzeRequest SpaceUserAnalyzeRequest
     * @param loginUser               当前登录用户
     * @return List<SpaceUserAnalyzeResponse>
     */
    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 检查权限
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);
        Long userId = spaceUserAnalyzeRequest.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime,'%Y-%m-%d') as period", "count(*) as count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) as period", "count(*) as count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime,'%Y-%m') as period", "count(*) as count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "时间维度错误");
        }
//        分组，升序排序
        queryWrapper.groupBy("period").orderByAsc("period");
//        查询并封装结果
        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(result -> {
                    String period = result.get("period").toString();
                    Long count = Long.parseLong(result.get("count").toString());
                    return new SpaceUserAnalyzeResponse(period, count);
                }).collect(Collectors.toList());
    }

    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
//        检查权限。仅管理员查看
        if (!userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
//       构造条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "spaceName", "userId", "totalSize")
                .orderByDesc("totalSize")
                .last("limit " + spaceRankAnalyzeRequest.getTopN());
//        返回查询结果
        return spaceService.list(queryWrapper);
    }


}




