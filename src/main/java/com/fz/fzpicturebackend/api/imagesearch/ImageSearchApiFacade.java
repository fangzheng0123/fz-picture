package com.fz.fzpicturebackend.api.imagesearch;

import com.fz.fzpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.fz.fzpicturebackend.api.imagesearch.sub.GetImageFirstUrlApi;
import com.fz.fzpicturebackend.api.imagesearch.sub.GetImageListApi;
import com.fz.fzpicturebackend.api.imagesearch.sub.GetImagePageUrlApi;

import java.util.List;

/**
 * @Author fang
 * @Date 2025/3/5 12:54
 * @注释 图片搜索API 门面
 */
public class ImageSearchApiFacade {

    /**
     * 图片搜索 返回图片列表
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }
}
