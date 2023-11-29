package com.kedacom.ctsp.iomp.k8s.common.util;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class K8sMapUtil {

    /**
     *
     * @param map
     * @param key
     * @param value
     * @param force 是否强制更新
     */
    public static void put(Map<String,String> map, String key, String value, Boolean force){
        if(BooleanUtils.isTrue(force)){
            map.put(key,value);
            return ;
        }
        if(StringUtils.isNotEmpty(value) && StringUtils.isNotEmpty(key)){
            map.put(key,value);
            return ;
        }
    }
}
