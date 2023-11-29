package com.kedacom.ctsp.iomp.k8s.dto;

import lombok.Data;

/**
 * @author tangjixing
 * @date 2020/4/20
 */
@Data
public class VolumeDto {

    public static final String TYPE_PVC = "pvc";

    public static final String TYPE_SECRET = "secret";

    public static final String TYPE_HOSTPATH = "hostpath";

    private String name;

    private String type;

    private String mountPath;

    private String mountHardcorePath;

    private String claimName;

    private String secretName;

/*

    private VolumeMount volumeMount;
    private Volume volume;
*/

}
