package com.biaofan.util;

import java.util.UUID;

/**
 * ID生成工具类，提供各种唯一ID的生成功能。
 *
 * @author biaofan
 */
public class IdUtil {

    /**
     * 生成模板ID，格式为：tpl_ + 16位无横线UUID。
     * 例如：tpl_a1b2c3d4e5f6g7h8
     *
     * @return 生成的模板ID字符串
     */
    public static String generateTemplateId() {
        return "tpl_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
