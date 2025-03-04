package com.fz.fzpicturebackend.model.vo;

import lombok.Data;

import java.util.List;

/**
 * @Author fang
 * @Date 2025/2/28 16:20
 * @注释  图片标签分类列表视图
 */

@Data
public class PictureTagCategory {
    /**
     * 标签列表
     */
    private List<String> tagList;
    /**
     * 分页列表
     */
    private List<String> categoryList;
}
