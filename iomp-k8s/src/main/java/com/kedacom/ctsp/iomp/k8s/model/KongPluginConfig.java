package com.kedacom.ctsp.iomp.k8s.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import javax.annotation.Generated;
import java.util.List;

/**
 * KongIngress
 *
 * @author kedaom
 * @create 2019-03-08 15:28
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
public class KongPluginConfig{

    List<String> blacklist;

    List<String> whilelist;

    public List<String> getBlacklist() {
        return blacklist;
    }

    public void setBlacklist(List<String> blacklist) {
        this.blacklist = blacklist;
    }

    public List<String> getWhilelist() {
        return whilelist;
    }

    public void setWhilelist(List<String> whilelist) {
        this.whilelist = whilelist;
    }
}