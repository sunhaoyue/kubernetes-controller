package com.kedacom.ctsp.iomp.k8s.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class NsParamVo {

    private String department;

    private String departmentName;
}
