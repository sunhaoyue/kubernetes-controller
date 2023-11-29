package com.kedacom.ctsp.iomp.k8s.common.util;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

import static com.kedacom.ctsp.iomp.k8s.enmu.ServiceAccountEnum.*;

public class ServiceAccountUtil {

    private static final List<String> accountList = Lists.newArrayList(DEFAULT.value(), NS_READ.value(), NS_ADMIN.value(), CLUSTER_READ.value(), ADMIN.value());

    /**
     * 空间资源这里，如果空间绑定admin用户，则服务上课使用admin、cluster-read、ns-admin、ns-read、default权限；
     * <p>
     * 如果空间绑定了cluster-read，则服务可使用cluster-read、ns-admin、ns-read、default权限；
     * <p>
     * 如果空间绑定了ns-admin，则服务可使用ns-admin、ns-read、default权限；
     * <p>
     * 如果空间绑定了ns-read，则服务可使用ns-read、default权限；
     * <p>
     * 如果空间绑定了default，则服务可使用default权限；
     *
     * @param serviceAccount
     * @return
     */
    public static String complete(String serviceAccount) {
        if (StringUtils.isEmpty(serviceAccount)) {
            return DEFAULT.value();
        }
        List<String> accounts = Arrays.asList(StringUtils.split(serviceAccount, ","));
        if (accounts.contains(ADMIN.value())) {
            return JoinerUtil.joinList(Lists.newArrayList(ADMIN.value(), CLUSTER_READ.value(), NS_ADMIN.value(), NS_READ.value(), DEFAULT.value()));
        }
        if (accounts.contains(CLUSTER_READ.value())) {
            return JoinerUtil.joinList(Lists.newArrayList(CLUSTER_READ.value(), NS_ADMIN.value(), NS_READ.value(), DEFAULT.value()));
        }
        if (accounts.contains(NS_ADMIN.value())) {
            return JoinerUtil.joinList(Lists.newArrayList(NS_ADMIN.value(), NS_READ.value(), DEFAULT.value()));
        }
        if (accounts.contains(NS_READ.value())) {
            return JoinerUtil.joinList(Lists.newArrayList(NS_READ.value(), DEFAULT.value()));
        }
        return JoinerUtil.joinList(Lists.newArrayList(DEFAULT.value()));
    }
}
