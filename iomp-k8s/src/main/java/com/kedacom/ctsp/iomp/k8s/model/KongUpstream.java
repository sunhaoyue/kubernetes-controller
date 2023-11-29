package com.kedacom.ctsp.iomp.k8s.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Generated;

/**
 * kong网关需要的Upstream类
 *
 * @author kedaom
 * @create 2019-03-08 16:57
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
public class KongUpstream {


    @JsonProperty("hash_on")
    private String hashOn = "cookie";

    @JsonProperty("hash_fallback")
    private String hashFallback = "none";

    @JsonProperty("hash_on_cookie")
    private String hashOnCookie = "route";

    @JsonProperty("healthchecks")
    private HealthChecks healthchecks;

    @JsonProperty("slots")
    private Integer slots=10000;

    public String getHashOnCookie() {
        return hashOnCookie;
    }

    public void setHashOnCookie(String hashOnCookie) {
        this.hashOnCookie = hashOnCookie;
    }

    public HealthChecks getHealthchecks() {
        return healthchecks;
    }

    public void setHealthchecks(HealthChecks healthchecks) {
        this.healthchecks = healthchecks;
    }

    public Integer getSlots() {
        return slots;
    }

    public void setSlots(Integer slots) {
        this.slots = slots;
    }

    public String getHashOn() {
        return hashOn;
    }



    public void setHashOn(String hashOn) {
        this.hashOn = hashOn;
    }

    public String getHashFallback() {
        return hashFallback;
    }

    public void setHashFallback(String hashFallback) {
        this.hashFallback = hashFallback;
    }

    public static class HealthChecks{

        @JsonProperty("active")
        private Active active;

        @JsonProperty("passive")
        private Passive passive;

        public Active getActive() {
            return active;
        }

        public void setActive(Active active) {
            this.active = active;
        }

        public Passive getPassive() {
            return passive;
        }

        public void setPassive(Passive passive) {
            this.passive = passive;
        }
    }

    public static class Active{

        @JsonProperty("concurrency")
        private Integer concurrency=10;

        @JsonProperty("http_path")
        private String httpPath="/";

        @JsonProperty("timeout")
        private Integer timeout=1;

        @JsonProperty("healthy")
        private Healthy healthy;

        @JsonProperty("unhealthy")
        private Unhealthy unhealthy;

        public Integer getConcurrency() {
            return concurrency;
        }

        public void setConcurrency(Integer concurrency) {
            this.concurrency = concurrency;
        }

        public String getHttpPath() {
            return httpPath;
        }

        public void setHttpPath(String httpPath) {
            this.httpPath = httpPath;
        }

        public Integer getTimeout() {
            return timeout;
        }

        public void setTimeout(Integer timeout) {
            this.timeout = timeout;
        }

        public Healthy getHealthy() {
            return healthy;
        }

        public void setHealthy(Healthy healthy) {
            this.healthy = healthy;
        }

        public Unhealthy getUnhealthy() {
            return unhealthy;
        }

        public void setUnhealthy(Unhealthy unhealthy) {
            this.unhealthy = unhealthy;
        }
    }

    public static class Passive{

        @JsonProperty("healthy")
        private Healthy healthy;

        @JsonProperty("unhealthy")
        private Unhealthy unhealthy;

        public Healthy getHealthy() {
            return healthy;
        }

        public void setHealthy(Healthy healthy) {
            this.healthy = healthy;
        }

        public Unhealthy getUnhealthy() {
            return unhealthy;
        }

        public void setUnhealthy(Unhealthy unhealthy) {
            this.unhealthy = unhealthy;
        }
    }

    public static class Healthy{

        @JsonProperty("http_statuses")
        private Integer[] httpStatuses;

        @JsonProperty("interval")
        private Integer interval=0;

        @JsonProperty("successes")
        private Integer successes=0;

        public Integer[] getHttpStatuses() {
            return httpStatuses;
        }

        public void setHttpStatuses(Integer[] httpStatuses) {
            this.httpStatuses = httpStatuses;
        }

        public Integer getInterval() {
            return interval;
        }

        public void setInterval(Integer interval) {
            this.interval = interval;
        }

        public Integer getSuccesses() {
            return successes;
        }

        public void setSuccesses(Integer successes) {
            this.successes = successes;
        }
    }

    public static class Unhealthy{

        @JsonProperty("http_statuses")
        private Integer[] httpStatuses;

        @JsonProperty("http_failures")
        private Integer httpFailures=0;

        @JsonProperty("tcp_failures")
        private Integer tcpFailures=0;

        @JsonProperty("timeouts")
        private Integer timeouts=0;

        public Integer[] getHttpStatuses() {
            return httpStatuses;
        }

        public void setHttpStatuses(Integer[] httpStatuses) {
            this.httpStatuses = httpStatuses;
        }

        public Integer getHttpFailures() {
            return httpFailures;
        }

        public void setHttpFailures(Integer httpFailures) {
            this.httpFailures = httpFailures;
        }

        public Integer getTcpFailures() {
            return tcpFailures;
        }

        public void setTcpFailures(Integer tcpFailures) {
            this.tcpFailures = tcpFailures;
        }

        public Integer getTimeouts() {
            return timeouts;
        }

        public void setTimeouts(Integer timeouts) {
            this.timeouts = timeouts;
        }
    }
}
