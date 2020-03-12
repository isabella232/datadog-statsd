package com.getbase.jenkins.plugins.metrics.history.dogstatsd.metrics;

import com.getbase.jenkins.plugins.metrics.history.dogstatsd.properties.DogTag;
import hudson.model.*;
import jenkins.metrics.impl.TimeInQueueAction;
import jenkins.model.Jenkins;
import org.apache.commons.lang.ArrayUtils;

import java.io.File;
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
    private static final String USERNAME = "username";
    private static final String AUTOMATED_RUN = "automated_run";
    private static final String TOP_LEVEL_DIRECTORY = "top_level_dir";
    private static final String BUILT_UNDER_SIX_MIN = "build_under_six_min";
    private static final String JOB_DIRECTORY = "job_directory";
    private static final String JENKINS_URL = "jenkins_url";

    public String jobName;
    public Integer buildNumber;
    public String buildResult;
    public Integer buildResultNum;
    public String buildUrl;
    public String jobUrl;
    public Map<String, String> parameters;
    public String startingUser;

    public long buildDuration;
    public long queueingDuration;
    public long totalDuration;
    private String topLevelDir;
    private boolean builtUnderSixMin;
    private String jobDirectory;
    private String jenkinsUrl;
    public Map<String, String> customTags;

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
                getTag(USERNAME, startingUser),
                getTag(TOP_LEVEL_DIRECTORY, topLevelDir),
                getTag(BUILT_UNDER_SIX_MIN, builtUnderSixMin ? "true" : "false"),
                getTag(AUTOMATED_RUN, startingUser != null ? "false" : "true"),
                getTag(JOB_DIRECTORY, jobDirectory),
                getTag(JENKINS_URL, jenkinsUrl),
        };
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            tags = (String[]) ArrayUtils.add(tags, getTag(PARAMETER + "." + entry.getKey(), entry.getValue()));
        }
        for (Map.Entry<String, String> entry : customTags.entrySet()) {
            tags = (String[]) ArrayUtils.add(tags, getTag(entry.getKey(), entry.getValue()));
        }
        return tags;
    }

    public static BuildMetrics fromBuild(Run<?, ?> build) {
        BuildMetrics bm = new BuildMetrics();
        bm.buildDuration = getBuildDuration(build);
        bm.queueingDuration = getQueueingDuration(build);
        bm.totalDuration = bm.buildDuration + bm.queueingDuration;
        bm.buildResult = getResult(build);
        bm.buildResultNum = bm.buildResult.equals("SUCCESS") || bm.buildResult.equals("UNSTABLE") ? 1 : 0;
        bm.buildNumber = build.getNumber();
        bm.jobName = build.getParent().getFullName();
        bm.jobUrl = build.getParent().getAbsoluteUrl();
        bm.buildUrl = bm.jobUrl + build.getNumber();
        bm.parameters = getBuildParameters(build);
        bm.customTags = getCustomTags(build);
        bm.startingUser = getUser(build);
        bm.topLevelDir = getTopLevelDir(build);
        bm.builtUnderSixMin = bm.totalDuration / 1000 / 60 < 6;
        bm.jobDirectory = getJobDir(build);
        bm.jenkinsUrl = getJenkinsUrl();
        return bm;
    }

    private static String getJobDir(Run<?, ?> build) {
        return new File(build.getParent().getFullName()).getParent();
    }

    private static String getJenkinsUrl() {
        Jenkins instance = Jenkins.getInstanceOrNull();
        if (instance != null) {
            return instance.getRootUrl();
        }
        return "";
    }

    private static String getTopLevelDir(Run<?, ?> build) {
        String fullName = build.getParent().getFullName();
        if (fullName.contains("/")) {
            String[] split = fullName.split("/");
            if (split.length > 0) {
                return split[0];
            }
        }
        return fullName;
    }

    private static String getUser(Run<?, ?> build) {
        Cause.UserIdCause cause = build.getCause(Cause.UserIdCause.class);
        if (cause != null) {
            return cause.getUserName();
        }
        return null;
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

    private static Map<String, String> getCustomTags(Run<?, ?> build) {
        List<DogTag.DogTagAction> actions = build.getActions(DogTag.DogTagAction.class);
        Map<String, String> tagsMap = new HashMap<>();
        for (DogTag.DogTagAction action : actions) {
            String key = action.getKey();
            String value = action.getValue();
            if (key != null && value != null) {
                tagsMap.put(key, value);
            }
        }
        return tagsMap;
    }
}
