package com.fz.fzpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author fang
 * @Date 2025/3/5 13:00
 * @注释
 */
@Data
public class SearchPictureByPictureRequest implements Serializable {
    private static final long serialVersionUID = -5361356118209906310L;
    /**
     * 图片id
     */
    private Long pictureId;

}
