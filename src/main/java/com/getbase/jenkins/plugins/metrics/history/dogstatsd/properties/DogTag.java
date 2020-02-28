package com.getbase.jenkins.plugins.metrics.history.dogstatsd.properties;

import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Set;

public class DogTag extends Step {
    private final @Nonnull
    String key;

    private final @Nonnull
    String value;

    @DataBoundConstructor
    public DogTag(@Nonnull String key, @Nonnull String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(key, value, context);
    }

    public static class DogTagAction implements Action {
        private String key;
        private String value;

        public DogTagAction(String key, String value) {
            this.key = key;
            this.value = value;
        }


        @CheckForNull
        @Override
        public String getIconFileName() {
            return "DogTagAction";
        }

        @CheckForNull
        @Override
        public String getDisplayName() {
            return "DogTagAction";
        }

        @CheckForNull
        @Override
        public String getUrlName() {
            return "DogTagAction";
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }

    public static class Execution extends SynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;
        private transient final String key;
        private transient final String value;

        Execution(String key, String value, StepContext context) {
            super(context);
            this.key = key;
            this.value = value;
        }

        @Override
        protected Void run() throws Exception {
            getContext().get(Run.class).addAction(new DogTagAction(key, value));
            return null;
        }

    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {
        @Override
        public String getFunctionName() {
            return "addDataDogTag";
        }

        @Override
        public String getDisplayName() {
            return "Adds DataDog tag to build";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class);
        }

    }

}