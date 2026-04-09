package com.biaofan.util;

import java.util.UUID;

public class IdUtil {
    public static String generateTemplateId() {
        return "tpl_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
