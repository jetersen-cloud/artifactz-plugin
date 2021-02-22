package io.iktech.jenkins.plugins.artifactor;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.google.common.collect.ImmutableSet;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.artifactz.client.ServiceClient;
import io.artifactz.client.exception.ClientException;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Objects;
import java.util.Set;

public class PublishArtifactStep extends Step {
    private static final Logger logger = LoggerFactory.getLogger(PublishArtifactStep.class);
    private String name;
    private String description;
    private String type;
    private String groupId;
    private String artifactId;
    private String stage;
    private String flow;
    private String stageDescription;
    private String version;

    @DataBoundConstructor
    public PublishArtifactStep(String name, String description, String type, String flow, String stage, String stageDescription, String groupId, String artifactId, String version) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.flow = flow;
        this.stage = stage;
        this.stageDescription = stageDescription;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public String getStage() {
        return stage;
    }

    @DataBoundSetter
    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getName() {
        return name;
    }

    @DataBoundSetter
    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    @DataBoundSetter
    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    @DataBoundSetter
    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    @DataBoundSetter
    public void setType(String type) {
        this.type = type;
    }

    public String getGroupId() {
        return groupId;
    }

    @DataBoundSetter
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    @DataBoundSetter
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getFlow() {
        return flow;
    }

    @DataBoundSetter
    public void setFlow(String flow) {
        this.flow = flow;
    }

    public String getStageDescription() {
        return stageDescription;
    }

    @DataBoundSetter
    public void setStageDescription(String stageDescription) {
        this.stageDescription = stageDescription;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new PublishArtifactStep.Execution(this.name, this.description, this.type, this.flow, this.stage, this.stageDescription, this.groupId, this.artifactId, this.version, context);
    }

    private static final class Execution extends SynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 4829381492818317576L;

        private String name;
        private String description;
        private String type;
        private String groupId;
        private String artifactId;
        private String stage;
        private String flow;
        private String stageDescription;
        private String version;

        Execution(String name, String description, String type, String flow, String stage, String stageDescription, String groupId, String artifactId, String version, StepContext context) {
            super(context);
            this.name = name;
            this.description = description;
            this.type = type;
            this.flow = flow;
            this.stage = stage;
            this.stageDescription = stageDescription;
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        @Override protected Void run() throws Exception {
            Run<?, ?> run = getContext().get(Run.class);
            TaskListener taskListener = getContext().get(TaskListener.class);

            PrintStream l = taskListener.getLogger();
            l.println("Pushing artifact '" + this.name + "' at the stage '" + this.stage + "'");

            StringCredentials token = CredentialsProvider.findCredentialById(Configuration.get().getCredentialsId(), StringCredentials.class, run);
            try {
                ServiceClient client = ServiceHelper.getClient(taskListener, token.getSecret().getPlainText());
                client.publishArtifact(this.stage, this.stageDescription, this.name, this.description, this.flow, this.type, this.groupId, this.artifactId, this.version);
                taskListener.getLogger().println("Successfully published artifact");
            } catch (ClientException e) {
                logger.error("Error while publishing artifact", e);
                String errorMessage = "Error while publishing artifact: " + e.getMessage();
                taskListener.fatalError(errorMessage);
                Objects.requireNonNull(run.getExecutor()).interrupt(Result.FAILURE);
                throw new AbortException(errorMessage);
            }

            return Void.TYPE.newInstance();
        }
    }

    @Extension
    public static final class DescriptorImpl extends StepDescriptor {
        @Override
        public String getFunctionName() {
            return "publishArtifact";
        }

        @Override
        public String getDisplayName() {
            return "Publish Artifact Version";
        }

        @Override
        public boolean isMetaStep() {
            return false;
        }

        @Override public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, FilePath.class, Launcher.class, TaskListener.class);
        }
    }
}
