package com.fz.fzpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author fang
 * @Date 2025/3/5 17:28
 * @注释 图片批量修改请求
 */

@Data
public class PictureEditByBatchRequest implements Serializable {
    private static final long serialVersionUID = 7076741931968585352L;
    /**
     * 修改图片id的集合
     */
    private List<Long> pictureIdList;
    /**
     * 空间Id
     */
    private Long spaceId;
    /**
     * 标签
     */
    private List<String> tags;
    /**
     * 图片描述
     */
    private String introduction;
    /**
     * 图片分类
     */
    private String category;
    /**
     * 图片名称
     */
    private String EditName;

}
