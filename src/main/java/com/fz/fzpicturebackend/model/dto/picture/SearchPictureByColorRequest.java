package com.fz.fzpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author fang
 * @Date 2025/3/5 13:00
 * @注释
 */
@Data
public class SearchPictureByColorRequest implements Serializable {
    private static final long serialVersionUID = -5361356118209906310L;
    /**
     * 空间id
     */
    private Long spaceId;
    /**
     * 图片颜色
     */
    private String picColor;

}
