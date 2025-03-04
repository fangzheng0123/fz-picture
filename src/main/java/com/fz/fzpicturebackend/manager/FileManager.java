package com.fz.fzpicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.fz.fzpicturebackend.common.ResultUtils;
import com.fz.fzpicturebackend.config.CosClientConfig;
import com.fz.fzpicturebackend.exception.BusinessException;
import com.fz.fzpicturebackend.exception.ErrorCode;
import com.fz.fzpicturebackend.exception.ThrowUtils;
import com.fz.fzpicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 */
@Service
@Slf4j
@Deprecated   //这个注解表示这个类已经废弃
public class FileManager {
    /**
     * 1 兆
     */
    private static final long ONE_M = 1024 * 1024L;

    private static final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp");

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;


    /**
     * 通过本地上传文件
     * @param multipartFile
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
//        校验图片
        validPicture(multipartFile);
        // 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
//        拼接文件上传路径，不使用原始文件名，增强安全性
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        try {
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();

            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            return uploadPictureResult;

        } catch (Exception e) {
            log.error("FileManager#uploadPicture2 error {}", ExceptionUtils.getRootCauseMessage(e));
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败");
        } finally {
            deleteTempFile(file);
        }
    }


    /**
     * 通过url上传文件
     * @param fileUrl
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPicture(String fileUrl, String uploadPathPrefix) {
//        校验图片
        validPicture(fileUrl);
        // 图片上传地址
        String uuid = RandomUtil.randomString(16);
//        String originalFilename = multipartFile.getOriginalFilename();
        String originalFilename = FileUtil.mainName(fileUrl);
//        拼接文件上传路径，不使用原始文件名，增强安全性
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        try {
            file = File.createTempFile(uploadPath, null);
//            multipartFile.transferTo(file);
            HttpUtil.downloadFile(fileUrl,file);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();

            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            return uploadPictureResult;

        } catch (Exception e) {
            log.error("FileManager#uploadPicture2 error {}", ExceptionUtils.getRootCauseMessage(e));
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败");
        } finally {
            deleteTempFile(file);
        }
    }

    /**
     * 校验文件
     *
     * @param multipartFile multipart 文件
     */
    public void validPicture(MultipartFile multipartFile) {
 }


    /**
     * 对传递的url问价路径进行校验
     * @param fileUrl 需要校验的 picture 对象
     */
    public void validPicture(String fileUrl) {
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


    /**
     * 删除临时文件
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        // 删除临时文件
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }
}


