package com.fz.fzpicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.fz.fzpicturebackend.config.CosClientConfig;
import com.fz.fzpicturebackend.exception.BusinessException;
import com.fz.fzpicturebackend.exception.ErrorCode;
import com.fz.fzpicturebackend.exception.ThrowUtils;
import com.fz.fzpicturebackend.manager.CosManager;
import com.fz.fzpicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 */
@Service
@Slf4j
public abstract class PictureUploadTemplate {
    /**
     * 1 兆
     */
    protected static final long ONE_M = 1024 * 1024L;

    protected static final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp");

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;


    /**
     * 通过本地上传文件
     * @param inputSource 传递的文件信息
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
//        1.校验图片
        validPicture(inputSource);
        // 2.图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = getOriginalFilename(inputSource);
//        拼接文件上传路径，不使用原始文件名，增强安全性
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        try {
//            3、创建临时文件，获取文件到服务器
            file = File.createTempFile(uploadPath, null);
//            处理文件来源
            processFile(inputSource,file);
//            上传文件到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
//            获取图片信息对象，封装返回结果
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
//            获取图片处理结果
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)){
                CIObject compressedCiObject = objectList.get(0);
                CIObject thumbnailciObject = compressedCiObject;
                if (objectList.size() > 1){
                    thumbnailciObject  = objectList.get(1);
                }
                return buildResult(originalFilename, compressedCiObject,thumbnailciObject);
            }
            return buildResult(imageInfo, uploadPath, originalFilename, file);
        } catch (Exception e) {
            log.error("FileManager#uploadPicture2 error {}", ExceptionUtils.getRootCauseMessage(e));
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败");
        } finally {
//            临时文件处理
            deleteTempFile(file);
        }
    }

    /**
     * 构建返回结果
     * @param compressedCiObject
     * @param originalFilename
     * @return
     */
    private UploadPictureResult buildResult(String originalFilename, CIObject compressedCiObject,CIObject thumbnailciObject) {
        int picWidth = compressedCiObject.getWidth();
        int picHeight = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();

        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" +thumbnailciObject.getKey());
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
        return uploadPictureResult;
    }

    /**
     * 构建返回结果
     * @param imageInfo
     * @param uploadPath
     * @param originalFilename
     * @param file
     * @return
     */
    private UploadPictureResult buildResult(ImageInfo imageInfo, String uploadPath, String originalFilename, File file) {
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
    }

    /**
     * 处理输入源并生成本地文件
     * @param inputSource
     */
    protected abstract void processFile(Object inputSource,File file);

    /**
     * 获取输入源的原始信息
     * @param inputSource
     * @return
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * 校验输入源（本地文件或url）
     * @param inputSource
     */
    protected abstract void validPicture(Object inputSource);

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


