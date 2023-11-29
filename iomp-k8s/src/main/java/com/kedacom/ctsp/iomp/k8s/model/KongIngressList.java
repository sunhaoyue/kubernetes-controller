package com.kedacom.ctsp.iomp.k8s.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.client.CustomResourceList;

import javax.annotation.Generated;

/**
 * KongIngress
 *
 * @author kedaom
 * @create 2019-03-08 15:28
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
public class KongIngressList extends CustomResourceList<KongIngress> {
}