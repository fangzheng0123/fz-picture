package com.fz.fzpicturebackend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author fang
 * @Date 2025/2/26 14:01
 * @注释 通用删除请求类
 */


@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = -2470999418652168150L;
    /**
     * id
     */
    private Long id;


}
