package com.getbase.jenkins.plugins.metrics.history.dogstatsd.metrics;

import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Result;
import hudson.model.Run;
import jenkins.metrics.impl.TimeInQueueAction;
import org.apache.commons.lang.ArrayUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildMetrics {
    private static final String PARAMETER = "parameters";
    private static final String JOB_NAME = "job_name";
    private static final String BUILD_NUMBER = "build_number";
    private static final String BUILD_RESULT = "build_result";
    private static final String BUILD_URL = "build_url";
    private static final String JOB_URL = "job_url";

    public String jobName;
    public Integer buildNumber;
    public String buildResult;
    public Integer buildResultNum;
    public String buildUrl;
    public String jobUrl;
    public Map<String, String> parameters;

    public long buildDuration;
    public long queueingDuration;
    public long totalDuration;

    private String getTag(String key, String value) {
        return key + ":" + value;
    }

    public String[] getTags() {
        String[] tags = new String[]{
                getTag(JOB_NAME, jobName),
                getTag(BUILD_NUMBER, buildNumber.toString()),
                getTag(BUILD_RESULT, buildResult),
                getTag(BUILD_URL, buildUrl),
                getTag(JOB_URL, jobUrl),
        };
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            ArrayUtils.add(tags, getTag(PARAMETER + "." + entry.getKey(), entry.getValue()));
        }
        return tags;
    }

    public static BuildMetrics fromBuild(Run<?, ?> build) {
        BuildMetrics bm = new BuildMetrics();
        bm.buildDuration = getBuildDuration(build);
        bm.queueingDuration = getQueueingDuration(build);
        bm.totalDuration = bm.buildDuration + bm.queueingDuration;
        bm.buildResult = getResult(build);
        bm.buildResultNum = bm.buildResult.equals("SUCCESS") ? 1 : 0;
        bm.buildNumber = build.getNumber();
        bm.jobName = build.getParent().getFullName();
        bm.jobUrl = build.getParent().getAbsoluteUrl();
        bm.buildUrl = bm.jobUrl + build.getNumber();
        bm.parameters = getBuildParameters(build);
        return bm;
    }

    private static long getQueueingDuration(Run<?, ?> build) {
        TimeInQueueAction action = build.getAction(TimeInQueueAction.class);
        return action.getQueuingDurationMillis();
    }

    private static String getResult(Run<?, ?> build) {
        final Result result = build.getResult();
        return result != null ? result.toString() : "UNKNOWN";
    }

    private static long getBuildDuration(Run<?, ?> build) {
        long startTime = build.getTimeInMillis();
        long currTime = System.currentTimeMillis();
        long dt = currTime - startTime;
        return build.getDuration() == 0 ? dt : build.getDuration();
    }

    private static Map<String, String> getBuildParameters(Run build) {
        List<ParametersAction> actions = build.getActions(ParametersAction.class);
        if (actions != null) {
            Map<String, String> parametersMap = new HashMap<>();
            for (ParametersAction action : actions) {
                List<ParameterValue> parameters = action.getParameters();
                if (parameters != null) {
                    for (ParameterValue parameter : parameters) {
                        String name = parameter.getName();
                        Object value = parameter.getValue();
                        parametersMap.put(name, value != null ? value.toString() : "");
                    }
                }
            }
            return parametersMap;
        }
        return null;
    }
}