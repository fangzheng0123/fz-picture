package com.fz.fzpicturebackend.utils;

/**
 * @Author fang
 * @Date 2025/3/5 16:13
 * @注释
 */

/**
 * 颜色转换工具类
 */
public class ColorTransformUtils {
    /**
     * 获取标准颜色
     * @param color
     * @return
     */
    public static String colorTransform(String color) {
//        每种rgb色值都可能只有一个0，要转换成00
//        如果是六位，不用转换，如果是五位，要给第三位后面加个0
//        示例：
//        0x080e0 =>0x0800e0
        if (color.length() == 7){
            color = color.substring(0, 4) + "0" + color.substring(4, 7);
        }
        return color;

    }
}
