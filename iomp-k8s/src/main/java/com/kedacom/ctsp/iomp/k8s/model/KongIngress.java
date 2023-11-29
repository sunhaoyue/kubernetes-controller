package com.kedacom.ctsp.iomp.k8s.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.fabric8.kubernetes.client.CustomResource;

import javax.annotation.Generated;

/**
 * KongIngress
 *
 * @author kedaom
 * @create 2019-03-08 15:28
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "apiVersion",
        "kind",
        "metadata",
        "upstream",
        "proxy",
        "route"
})
public class KongIngress extends CustomResource {

    @JsonProperty("upstream")
    private KongUpstream upstream;

    @JsonProperty("proxy")
    private KongProxy proxy;

    @JsonProperty("route")
    private KongRoute route;

    /**
     * No args constructor for use in serialization
     */
    public KongIngress() {
    }

    public KongUpstream getUpstream() {
        return upstream;
    }

    public void setUpstream(KongUpstream upstream) {
        this.upstream = upstream;
    }

    public KongProxy getProxy() {
        return proxy;
    }

    public void setProxy(KongProxy proxy) {
        this.proxy = proxy;
    }

    public KongRoute getRoute() {
        return route;
    }

    public void setRoute(KongRoute route) {
        this.route = route;
    }
}