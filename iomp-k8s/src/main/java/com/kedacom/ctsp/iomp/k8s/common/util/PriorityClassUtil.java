package com.kedacom.ctsp.iomp.k8s.common.util;

import com.kedacom.ctsp.iomp.k8s.enmu.K8sPriorityClassEnum;
import com.kedacom.ctsp.lang.EnumUtil;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

public class PriorityClassUtil {

    /**
     * level为空返回null
     * level未找到enum，返回null
     *
     * @param level
     * @return
     */
    public static String getByLevel(String level) {
        if (StringUtils.isEmpty(level)) {
            return null;
        }
        K8sPriorityClassEnum priorityClassEnum = EnumUtil.parse(level, K8sPriorityClassEnum.class);
        if (priorityClassEnum == null) {
            return null;
        }
        return priorityClassEnum.getName();
    }
}
