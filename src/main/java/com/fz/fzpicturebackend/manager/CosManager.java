package com.fz.fzpicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import com.fz.fzpicturebackend.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author fang
 * @Date 2025/2/27 15:30
 * @注释  文件管理类
 */
@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;
    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     * @param key 唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file){
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     * @param key 唯一键
     */
    public COSObject getObject(String key){
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }


    /**
     * 上传对象（附带图片信息）
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        // 对图片进行处理（获取基本信息也被视作为一种图片的处理）
        PicOperations picOperations = new PicOperations();
        // 0 不返回原图信息，1返回原图信息，默认为0
        picOperations.setIsPicInfo(1);
//        图片处理规则
//        压缩规则
        List<PicOperations.Rule> rules = new ArrayList<>();
        String webpKey = FileUtil.mainName(key)+".jpg";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setRule("imageMogr2/format/jpg");
        compressRule.setFileId(webpKey);
        compressRule.setBucket(cosClientConfig.getBucket());
        rules.add(compressRule);
        if (file.length()> 2 * 1024){
            //        添加缩略图规则
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            String thumbnailKey = FileUtil.mainName(key)+"_thumbnail"+FileUtil.getSuffix(key);
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s",255,255));
            thumbnailRule.setFileId(thumbnailKey);
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            rules.add(thumbnailRule);
        }

        // 构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }


    /**
     * 删除对象
     * @param key 唯一键
     */
    public void deleteObject(String key){
        new DeleteObjectRequest(cosClientConfig.getBucket(), key);
    }
}
