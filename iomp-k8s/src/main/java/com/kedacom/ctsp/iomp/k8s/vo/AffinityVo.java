package com.kedacom.ctsp.iomp.k8s.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author hesen
 * @since 2021/7/28
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class AffinityVo implements Serializable {

    private String key;

    private String operator;

    private String values;
}
