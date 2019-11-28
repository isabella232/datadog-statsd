package com.getbase.jenkins.plugins.metrics.history.dogstatsd.listeners;

import com.getbase.jenkins.plugins.metrics.history.dogstatsd.DogStatsDConfig;
import com.getbase.jenkins.plugins.metrics.history.dogstatsd.metrics.BuildMetrics;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import org.apache.commons.lang.StringUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Send Jenkins build information to InfluxDB
 */
@Extension
public class DogStatsDPublisher extends RunListener<Run<?, ?>> {
    private static final String DD_AGENT_HOST_DEFAULT_ENV = "DD_AGENT_HOST";
    private static final String NUMBER_OF_BUILDS = "number_of_builds";
    private static final String BUILD_DURATION = "build_duration";
    private static final String QUEUEING_DURATION = "queueing_duration";
    private static final String TOTAL_DURATION = "total_duration";
    private static final String BUILD_RESULT = "build_result";
    private static final String BUILD_DONE = "build_done";

    private static final int DEFAULT_AGENT_PORT = 8125;
    private static final Logger logger = Logger.getLogger(DogStatsDPublisher.class.getName());

    @Extension
    public static final DogStatsDConfig ddConfig = new DogStatsDConfig();

    @Override
    public void onCompleted(Run<?, ?> build, TaskListener listener) {
        try {
            BuildMetrics metrics = BuildMetrics.fromBuild(build);
            StatsDClient client = getClient();
            writeToDogStatsD(client, metrics);
            client.close();
            listener.getLogger().println("[DogStatsD Plugin] Completed.");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not report to DogStatsD. Ignoring Exception.", e);
        }
    }

    private StatsDClient getClient() {
        String hostname = System.getenv(ddConfig.getAgentHostNameEnvVar());
        return new NonBlockingStatsDClient(
                StringUtils.isBlank(ddConfig.getDefaultPrefix()) ? "jenkins.dogstatsd" : ddConfig.getDefaultPrefix(),
                StringUtils.isBlank(hostname) ? DD_AGENT_HOST_DEFAULT_ENV : hostname,
                getAgentPort(),
                ddConfig.getGlobalTags().split(",")
        );
    }

    private int getAgentPort() {
        try {
            return Integer.parseInt(ddConfig.getAgentPort());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not parse agent port. Using default 8125.", e);
            return DEFAULT_AGENT_PORT;
        }
    }

    private void writeToDogStatsD(StatsDClient client, BuildMetrics metrics) {
        String[] buildTags = metrics.getTags();
        client.increment(NUMBER_OF_BUILDS, buildTags);
        client.time(BUILD_DURATION, metrics.buildDuration, buildTags);
        client.time(QUEUEING_DURATION, metrics.queueingDuration, buildTags);
        client.time(TOTAL_DURATION, metrics.totalDuration, buildTags);
        client.gauge(BUILD_RESULT, metrics.buildResultNum, buildTags);
        client.gauge(BUILD_DONE, 1, buildTags);
    }
}
