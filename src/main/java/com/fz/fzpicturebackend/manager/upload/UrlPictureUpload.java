package com.fz.fzpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.fz.fzpicturebackend.exception.BusinessException;
import com.fz.fzpicturebackend.exception.ErrorCode;
import com.fz.fzpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * @Author fang
 * @Date 2025/3/2 16:35
 * @注释
 */

@Service
public class UrlPictureUpload extends PictureUploadTemplate{

    /**
     * 处理输入源并生成本地文件
     * @param inputSource
     * @param file
     */
    @Override
    protected void processFile(Object inputSource, File file) {
        String fileUrl = (String) inputSource;
        HttpUtil.downloadFile(fileUrl,file);
    }

    /**
     * 获取输入源的原始信息
     * @param inputSource
     * @return
     */
    @Override
    protected String getOriginalFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        return FileUtil.mainName(fileUrl);
    }

    /**
     * 校验输入源（url）
     * @param inputSource
     */
    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl = (String) inputSource;
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR,"文件地址为隆");
//        校验url的有效性
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件地址格式不正确");
        }
//        校验url的协议
        ThrowUtils.throwIf(!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"),ErrorCode.PARAMS_ERROR,"仅支持 HTTP 或 HTTPS协议的文件地址");
//        发送HEAD请求验证文件是否存在
        HttpResponse httpResponse = null;
        try {
            httpResponse = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
//            未正常返回，无需执行其他判断
            if(httpResponse.getStatus() != HttpStatus.HTTP_OK){
                return;
            }
//            文件存在，检验文件类型
            String contentType = httpResponse.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)){
//                允许的图片类型
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "/image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),ErrorCode.PARAMS_ERROR,"文件类型错误");
            }
//            文件大小校验
            String contentLengthStr = httpResponse.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)){
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long ONE_M = 1024 * 1024;
                    ThrowUtils.throwIf(contentLength > 2 * ONE_M,ErrorCode.PARAMS_ERROR,"文件大小不能超过2M");
                } catch (NumberFormatException e) {
                    throw new RuntimeException(e);
                }
            }
        }finally {
            if (httpResponse != null){
                httpResponse.close();
            }
        }
    }
}
