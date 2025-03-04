package com.fz.fzpicturebackend.model.dto.space;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Author fang
 * @Date 2025/3/4 12:55
 * @注释
 */
@Data
@AllArgsConstructor
public class SpaceLevel {
    /**
     * 值
     */
    private int value;
    /**
     * 中文
     */
    private String text;
    /**
     * 最大数量
     */
    private long maxCount;
    /**
     * 最大容量
     */
    private long maxSize;
}
