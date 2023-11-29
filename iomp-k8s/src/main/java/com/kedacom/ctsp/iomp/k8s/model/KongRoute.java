package com.kedacom.ctsp.iomp.k8s.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Generated;
import java.util.List;

/**
 * kong的route
 *
 * @author kedaom
 * @create 2019-03-08 17:10
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
public class KongRoute {

    @JsonProperty("methods")
    private List<String> methods;

    @JsonProperty("regex_priority")
    private Integer regexPriority;


    @JsonProperty("strip_path")
    private Boolean stripPath;

    @JsonProperty("preserve_host")
    private Boolean preserveHost=Boolean.TRUE;


    @JsonProperty("protocols")
    private List<String> protocols;

    public List<String> getMethods() {
        return methods;
    }

    public void setMethods(List<String> methods) {
        this.methods = methods;
    }

    public Integer getRegexPriority() {
        return regexPriority;
    }

    public void setRegexPriority(Integer regexPriority) {
        this.regexPriority = regexPriority;
    }

    public Boolean getStripPath() {
        return stripPath;
    }

    public void setStripPath(Boolean stripPath) {
        this.stripPath = stripPath;
    }

    public Boolean getPreserveHost() {
        return preserveHost;
    }

    public void setPreserveHost(Boolean preserveHost) {
        this.preserveHost = preserveHost;
    }

    public List<String> getProtocols() {
        return protocols;
    }

    public void setProtocols(List<String> protocols) {
        this.protocols = protocols;
    }
}
