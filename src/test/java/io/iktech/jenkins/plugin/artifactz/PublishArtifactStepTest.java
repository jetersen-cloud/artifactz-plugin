package io.iktech.jenkins.plugin.artifactz;

import io.artifactz.client.ServiceClient;
import io.artifactz.client.ServiceClientBuilder;
import io.artifactz.client.exception.ClientException;
import io.artifactz.client.model.Stage;
import io.iktech.jenkins.plugins.artifactz.Configuration;
import io.iktech.jenkins.plugins.artifactz.PublishArtifactStep;
import io.jenkins.cli.shaded.org.apache.commons.io.FileUtils;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ServiceClientBuilder.class})
@PowerMockIgnore({"org.apache.http.conn.ssl.*", "javax.net.ssl.*" , "javax.crypto.*" })
public class PublishArtifactStepTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setUp() throws IOException {
        TestHelper.addCredential(j, "test");
        Configuration configuration = Configuration.get();
        configuration.setServerUrl("http://localhost:5002");
        configuration.doFillCredentialsIdItems(Jenkins.get(), null, "test");
        configuration.setCredentialsId("test");
    }

    @Test
    public void gettersAndSettersTest() throws Exception {
        PublishArtifactStep test = new PublishArtifactStep(null, null, null, null, null, null, null, null, null);
        test.setName("test-artifact");
        test.setDescription("Test Artifact");
        test.setStage("Development");
        test.setStageDescription("Development Stage");
        test.setType("JAR");
        test.setFlow("Standard");
        test.setGroupId("io.iktech.test");
        test.setArtifactId("test-artifact");
        test.setVersion("1.0.0");
        assertEquals("test-artifact", test.getName());
        assertEquals("Test Artifact", test.getDescription());
        assertEquals("Development", test.getStage());
        assertEquals("Development Stage", test.getStageDescription());
        assertEquals("JAR", test.getType());
        assertEquals("Standard", test.getFlow());
        assertEquals("io.iktech.test", test.getGroupId());
        assertEquals("test-artifact", test.getArtifactId());
        assertEquals("1.0.0", test.getVersion());
    }

    @Test
    public void publishArtifactSuccessTest() throws Exception {
        TestHelper.setupClient();

        WorkflowJob project = j.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition("" +
                "node {" +
                "  publishArtifact stage: 'Development', name: 'test-artifact', type: 'DockerImage', version: '1.0.0'\n" +
                "}", true));

        WorkflowRun build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");
        String s = FileUtils.readFileToString(build.getLogFile());
        assertThat(s, containsString("Successfully published artifact"));
    }

    @Test
    public void publishArtifactFailureTest() throws Exception {
        ServiceClient client = TestHelper.setupClient();

        Stage stage = new Stage();
        stage.setStage("Development");

        doThrow(new ClientException("test exception")).when(client).publishArtifact(eq("Development"), any(), eq("test-artifact"), any(), any(), eq("JAR"), any(), any(), eq("1.0.0"));

        WorkflowJob project = j.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition("" +
                "node {" +
                "  def version = publishArtifact stage: 'Development', name: 'test-artifact', type: 'JAR', flow: 'standard', stageDescription: 'Test stage', description: 'Test description', groupId: 'io.iktech', artifactId: 'test-artifact', version: '1.0.0'\n" +
                "}", true));

        WorkflowRun build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");
        String s = FileUtils.readFileToString(build.getLogFile());
        assertThat(s, containsString("Error while publishing artifact: test exception"));
    }
}
