package com.fz.fzpicturebackend.api.aliyunapi;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.fz.fzpicturebackend.api.aliyunapi.model.CreateOutPaintingTaskRequest;
import com.fz.fzpicturebackend.api.aliyunapi.model.CreateOutPaintingTaskResponse;
import com.fz.fzpicturebackend.api.aliyunapi.model.GetOutPaintingTaskResponse;
import com.fz.fzpicturebackend.exception.BusinessException;
import com.fz.fzpicturebackend.exception.ErrorCode;
import com.fz.fzpicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @Author fang
 * @Date 2025/3/6 9:30
 * @注释
 */

@Slf4j
@Component

public class AliYunAPI {
    @Value("${aliyun.apiKey}")
    private String apiKey;
    // 创建任务  POST https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting
//    根据任务id查询结果 GET https://dashscope.aliyuncs.com/api/v1/tasks/{task_id}
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

//    创建任务
//    创建任务完成之后会有一个响应，响应信息就是CreateOutPaintingTaskResponse
//    创建任务的请求信息就是CreateOutPaintingTaskRequest请求类信息

    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest){
        // 创建请求
//        使用post请求访问接口
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
//                补充请求头
                .header("Authorization", "Bearer " + apiKey)
//                必须开启异步
                .header("X-DashScope-Async", "enable")
                .header("Content-Type", "application/json")
//                将对象转为json字符串传递给前端
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));

//        处理响应
//        获取到响应信息
        try (HttpResponse httpResponse = httpRequest.execute()){
//            如果响应信息不是ok 就抛出异常
            if (!httpResponse.isOk()){
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }
//            否则就将传回的数据转为对象在存储到AI扩图任务响应类中
            CreateOutPaintingTaskResponse createOutPaintingTaskResponse = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            if (createOutPaintingTaskResponse.getCode() != null){
                log.error("AI 扩图失败" + createOutPaintingTaskResponse.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败" + createOutPaintingTaskResponse.getMessage());
            }
//            将响应结果返回
            return createOutPaintingTaskResponse;
        }
    }

//    查询任务的响应结果
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId){
        ThrowUtils.throwIf(taskId == null, ErrorCode.PARAMS_ERROR, "任务id不能为空");
//        处理响应
        try (HttpResponse httpResponse = HttpRequest.get(String.format(GET_OUT_PAINTING_TASK_URL, taskId))
                .header("Authorization", "Bearer " + apiKey)
                .execute()){
            if (!httpResponse.isOk()){
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务结果失败");
            }
//            将查询到的信息存到获取任务详情请求中
            return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
        }
    }


}
