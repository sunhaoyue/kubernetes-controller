package com.kedacom.ctsp.iomp.k8s.vo;

import com.kedacom.ctsp.iomp.k8s.constant.K8sConstants;
import lombok.Data;

@Data
public class NsAttributeVo {

    private String terminal = K8sConstants.ENABLED;

    private String download = K8sConstants.ENABLED;

    /**
     * 兼容以前方法，因为vo对象中的默认值去除了
     * @param terminal
     * @param download
     * @return
     */
    public static NsAttributeVo instance(String terminal, String download) {
        NsAttributeVo vo = new NsAttributeVo();
        vo.setTerminal(terminal);
        if(terminal == null){
            vo.setTerminal(K8sConstants.ENABLED);
        }
        vo.setDownload(download);
        if(download == null){
            vo.setDownload(K8sConstants.ENABLED);
        }
        return vo;
    }

    public static NsAttributeVo instanceCustom(String terminal, String download) {
        NsAttributeVo vo = new NsAttributeVo();
        vo.setTerminal(terminal);
        vo.setDownload(download);
        return vo;
    }
}
