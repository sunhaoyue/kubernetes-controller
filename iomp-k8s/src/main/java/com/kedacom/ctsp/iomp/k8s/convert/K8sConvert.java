package com.kedacom.ctsp.iomp.k8s.convert;

/**
 * @author tangjixing
 * @date 2020/4/20
 */
public interface K8sConvert<F, T> {

    /**
     * 对象转换
     * @param from
     * @return
     */
    T convert (F from);

    /**
     * 把 target 合并覆盖到  source中，
     * @return
     */
    F merge (T source, F target);
}
