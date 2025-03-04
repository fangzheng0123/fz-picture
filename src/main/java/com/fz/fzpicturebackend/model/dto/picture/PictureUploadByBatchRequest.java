package com.fz.fzpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author fang
 * @Date 2025/3/2 18:30
 * @注释  批量抓取请求数据
 */
@Data
public class PictureUploadByBatchRequest implements Serializable {
    private static final long serialVersionUID = 1560595547285029621L;
    /**
     * 搜索关键字
     */
    private String searchText;
    /**
     * 搜索数量
     */
    private Integer count = 10;
    /**
     * 搜索之后文件命名
     */
    private String namePrefix;
}
