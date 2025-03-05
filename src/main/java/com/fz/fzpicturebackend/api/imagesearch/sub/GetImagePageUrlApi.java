package com.fz.fzpicturebackend.api.imagesearch.sub;

import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.fz.fzpicturebackend.exception.BusinessException;
import com.fz.fzpicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class GetImagePageUrlApi {

    /**
     * 获取图片页面地址
     *
     * @param imageUrl
     * @return
     */
    public static String getImagePageUrl(String imageUrl) throws BusinessException {

        // 1. 准备请求参数
        Map<String, Object> formData = new HashMap<>();
        formData.put("image", imageUrl);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");

        // 获取当前时间戳
        long uptime = System.currentTimeMillis();
        // 请求地址
        String url = "https://graph.baidu.com/upload?uptime=" + uptime;

        try {
            // 2. 发送 POST 请求到百度接口
            HttpResponse response = HttpRequest.post(url)
                    .form(formData)
                    .timeout(5000)
                    .header("Acs-Token","1741062668951_1741136590190_VTgF+gSn4nuxc3GlVlBR6I3Wb2jLCi98gQx2UE0fkVG3/oabYx0DRD8Jd1xALX3z9ZF8HEN7CvimFmxCzsobfTnM0PAsh3QuU/ONFU+hyth7mn06jHSeNkv7620qMqRS77wQtaEILsCV41AcV86p+s3OOEGd36EsmpE02KNixFB0TL8+NhIw5it1xJ+9MD07kAh94mX1Q2uERGl5lO4HM0fKn1JA4/m3fWisyh/h4OcPz74ul6d5f23fLlKLBMSWE6JHAh4jVmMDCOZMzUVwYtXCgf7aRTwLU9VPNJJss3uIB8MhXYMkAhJJa6+t7E+ImKiDn4+nFPZKcY9Bu6+QXPSW3ecuAVelcvx+1kRwhmZG4/nN0Khylyz4445yAzRE1aJOUiNc84S+Ie3r5MHWZFu+D3Pvyta5xl/yJZScRAYctXz7/XExuM31PLCtQ4S/t32cgc4J+ZI4cXYXRc0hGVHd1bxKlWxjjPI4iWIzyCM=")
                    .execute();
            // 判断响应状态
            if (HttpStatus.HTTP_OK != response.getStatus()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            // 解析响应
            String responseBody = response.body();
            Map<String, Object> result = JSONUtil.toBean(responseBody, Map.class);

            // 3. 处理响应结果
            if (result == null || !Integer.valueOf(0).equals(result.get("status"))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            String rawUrl = (String) data.get("url");
            // 对 URL 进行解码
            String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            // 如果 URL 为空
            if (searchResultUrl == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效结果");
            }
            return searchResultUrl;
        } catch (Exception e) {
            log.error("搜索失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }
    }

}

