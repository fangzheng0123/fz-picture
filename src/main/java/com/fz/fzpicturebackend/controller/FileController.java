package com.fz.fzpicturebackend.controller;

import com.fz.fzpicturebackend.annotation.AuthCheck;
import com.fz.fzpicturebackend.common.BaseResponse;
import com.fz.fzpicturebackend.common.ResultUtils;
import com.fz.fzpicturebackend.constant.UserConstant;
import com.fz.fzpicturebackend.exception.BusinessException;
import com.fz.fzpicturebackend.exception.ErrorCode;
import com.fz.fzpicturebackend.manager.CosManager;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * @Author fang
 * @Date 2025/2/27 15:42
 * @注释
 */
@RestController
@Slf4j
@RequestMapping("/file")
public class FileController {

    @Resource
    private CosManager cosManager;

    /**
     * 测试文件上传
     *
     * @param multipartFile
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
//        获取上传的文件名称
        String filename = multipartFile.getOriginalFilename();
//        上传文件的存储位置
        String filepath = String.format("/test/%s", filename);
        File file = null;
        try {
//            将multipartFile转变成临时文件并赋值给file，此处是个空文件
            file = File.createTempFile(filepath, null);
//            把前端传递的文件传输到本地的临时文件中
            multipartFile.transferTo(file);
//            最后上传文件
            cosManager.putObject(filepath, file);
            return ResultUtils.success(filepath);
        } catch (Exception e) {
            log.error("file upload error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
//                删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error,filepath = {}", filepath);
                }
            }
        }
    }
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInput = null;
        try {
            COSObject object = cosManager.getObject(filepath);
            cosObjectInput = object.getObjectContent();
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            response.setContentType("application/octet-stream;charset-UTF-8");
            response.setHeader("Content-Disposition","attachment; filename="+filepath);
//            写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }finally {
            if (cosObjectInput != null){
                cosObjectInput.close();
            }
        }
    }


}
