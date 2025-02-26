package com.fz.fzpicturebackend.common;

import lombok.Data;

/**
 * @Author fang
 * @Date 2025/2/26 13:58
 * @注释  通用分页请求类
 */

@Data
public class PageRequest {

    /**
     * 当前页数
     */
    private int current = 1;
    /**
     * 页面大小
     */
    private int pageSize = 10;
    /**
     * 排序字段
     */
    private String sortField;
    /**
     * 排序顺序（默认升序）
     */
    private String sortOrder = "descend";

}
