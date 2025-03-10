package com.fz.fzpicturebackend.model.enums;

import lombok.Getter;

/**
 * @Author fang
 * @Date 2025/3/8 14:58
 * @注释
 */
@Getter
public enum SpaceTypeEnum {
    PRIVATE("私有空间",0),
    TEAM("团队空间",1);

    private final Integer value;
    private final String text;

    SpaceTypeEnum(String text, Integer value) {
        this.value = value;
        this.text = text;
    }
    public static SpaceTypeEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (SpaceTypeEnum anEnum : SpaceTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
