package com.fz.fzpicturebackend.controller;

import com.fz.fzpicturebackend.common.BaseResponse;
import com.fz.fzpicturebackend.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author fang
 * @Date 2025/2/26 14:10
 * @注释
 */

@RestController
@RequestMapping("/")
public class MainController {

    @GetMapping("/health")
    public BaseResponse<String> health(){
        return ResultUtils.success("okk");
    }
}
