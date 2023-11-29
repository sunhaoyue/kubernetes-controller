package com.kedacom.ctsp.iomp.k8s.common.util;

import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * joiner util
 */
public class JoinerUtil {

    public static String joincomma(List list) {
        //Joiner.on(",").join(list);
        return StringUtils.join(list, ",");
    }

    public static String joinList(List list) {
        return StringUtils.join(list, ",");
    }

    public static String joincomma(Set set) {
        return Joiner.on(",").join(set);
    }

    public static String joinSet(Set set) {
        return Joiner.on(",").join(set);
    }

}
