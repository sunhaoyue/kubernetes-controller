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
        "plugin",
        "config"
})
public class KongPlugin extends CustomResource {

    @JsonProperty("config")
    private KongPluginConfig config;

    @JsonProperty("plugin")
    private String plugin;

    public KongPluginConfig getConfig() {
        return config;
    }

    public void setConfig(KongPluginConfig config) {
        this.config = config;
    }

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }
}