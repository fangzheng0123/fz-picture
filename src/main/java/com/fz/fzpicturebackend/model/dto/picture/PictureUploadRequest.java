package com.fz.fzpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data  
public class PictureUploadRequest implements Serializable {

    private static final long serialVersionUID = -5829198876484397821L;
    /**  
     * 图片 id（用于修改）  
     */  
    private Long id;
    /**
     * 图片的url地址信息
     */
    private String fileUrl;
    /**
     * 图片名称
     */
    private String picName;
    /**
     * 空间 id （为空表示公共空间）
     */
    private Long spaceId;

}

