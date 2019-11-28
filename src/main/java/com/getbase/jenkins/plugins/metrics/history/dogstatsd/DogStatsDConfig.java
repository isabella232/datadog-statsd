package com.getbase.jenkins.plugins.metrics.history.dogstatsd;

import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class DogStatsDConfig extends GlobalConfiguration {
    private String agentHostNameEnvVar;
    private String agentPort;
    private String defaultPrefix;
    private String globalTags;

    public DogStatsDConfig() {
        load();
    }

    public static DogStatsDConfig get() {
        return GlobalConfiguration.all().get(DogStatsDConfig.class);
    }

    public String getAgentHostNameEnvVar() {
        return agentHostNameEnvVar;
    }

    public void setAgentHostNameEnvVar(String agentHostNameEnvVar) {
        this.agentHostNameEnvVar = agentHostNameEnvVar;
    }

    public String getAgentPort() {
        return agentPort;
    }

    public void setAgentPort(String agentPort) {
        this.agentPort = agentPort;
    }

    public String getDefaultPrefix() {
        return defaultPrefix;
    }

    public void setDefaultPrefix(String defaultPrefix) {
        this.defaultPrefix = defaultPrefix;
    }

    public String getGlobalTags() {
        return globalTags;
    }

    public void setGlobalTags(String globalTags) {
        this.globalTags = globalTags;
    }

    @Override
    public boolean configure(StaplerRequest request, JSONObject json) throws FormException {
        request.bindJSON(this, json);
        save();
        return true;
    }

    @Override
    public String toString() {
        return "[agentHostNameEnvVar=" + this.agentHostNameEnvVar
                + ", agentPort=" + this.agentPort
                + ", defaultPrefix=" + this.defaultPrefix
                + ", globalTags=" + this.globalTags + "]";
    }

    public FormValidation doCheckAgentHostNameEnvVar(@QueryParameter("agentHostNameEnvVar") final String agentPort) {
        if (StringUtils.isBlank(agentHostNameEnvVar)) {
            return FormValidation.error("Agent hostname must not be empty");
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckAgentPort(@QueryParameter("agentPort") final String agentPort) {
        if (StringUtils.isBlank(agentPort)) {
            return FormValidation.error("Port must not be empty");
        }
        if (!StringUtils.isNumeric(agentPort)) {
            return FormValidation.error("Port must be an integer");
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckGlobalTag(@QueryParameter("globalTags") final String globalTags) {
        String[] pairs = globalTags.split(",");
        for (String pair : pairs) {
            String[] keyPair = pair.trim().split(":");
            if (keyPair.length != 2) {
                return FormValidation.error("Every tag must be `key:value` pair");
            }
        }
        return FormValidation.ok();
    }
}
