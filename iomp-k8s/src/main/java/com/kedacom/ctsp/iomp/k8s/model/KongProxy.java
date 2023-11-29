package com.kedacom.ctsp.iomp.k8s.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Generated;

/**
 * kong 代理
 *
 * @author kedaom
 * @create 2019-03-08 17:04
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
public class KongProxy {

    @JsonProperty("protocol")
    private String protocol;

    @JsonProperty("path")
    private String path;

    @JsonProperty("connect_timeout")
    private String connectTimeout;

    @JsonProperty("retries")
    private String retries;

    @JsonProperty("read_timeout")
    private String readTimeout;

    @JsonProperty("write_timeout")
    private String writeTimeout;

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(String connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public String getRetries() {
        return retries;
    }

    public void setRetries(String retries) {
        this.retries = retries;
    }

    public String getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(String readTimeout) {
        this.readTimeout = readTimeout;
    }

    public String getWriteTimeout() {
        return writeTimeout;
    }

    public void setWriteTimeout(String writeTimeout) {
        this.writeTimeout = writeTimeout;
    }
}
