package com.kedacom.ctsp.iomp.k8s.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sunhaoyue
 * @date Created in 2023/11/8 20:40
 */
@Data
@Version("v1")
@Group("apps")
public class ClusterAndNode {

    @JsonProperty("clusters")
    List<ClusterVO> clusters;
}


