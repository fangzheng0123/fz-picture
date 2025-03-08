package com.fz.fzpicturebackend.model.enums;

import lombok.Getter;

/**
 * @Author fang
 * @Date 2025/3/8 15:41
 * @注释
 */
@Getter
public enum SpaceRoleEnum {

//    /**
//     * 空间角色：viewer/editor/admin
//     */
//    private String spaceRole;
    VIEWER("浏览者","viewer"),
    EDITOR("编辑者","editor"),
    ADMIN("管理员","admin");
    private final String text;
    private final String value;

    SpaceRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }
    public static SpaceRoleEnum getEnumByValue(String value) {
        for (SpaceRoleEnum roleEnum : values()) {
            if (roleEnum.getValue().equals(value)) {
                return roleEnum;
            }
        }
        return null;
    }

}
