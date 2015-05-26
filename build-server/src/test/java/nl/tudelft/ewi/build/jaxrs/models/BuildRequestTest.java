package nl.tudelft.ewi.build.jaxrs.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.tudelft.ewi.build.jaxrs.json.MappingModule;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Jan-Willem Gmelig Meyling
 */
public class BuildRequestTest {

    private ObjectMapper objectMapper;

    @Before
    public void beforeTest() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new MappingModule());
    }

    @Test
    public void test() throws IOException {
        BuildRequest expected = new BuildRequest();

        MavenBuildInstruction buildInstruction = new MavenBuildInstruction();
        buildInstruction.setPhases(new String[] { "make", "fap" });
        buildInstruction.setWithDisplay(true);

        GitSource gitSource = new GitSource();
        gitSource.setRepositoryUrl("repo-url");
        gitSource.setCommitId("commitId");
        gitSource.setBranchName("branch-name");

        expected.setTimeout(100);
        expected.setCallbackUrl("callbackurl");
        expected.setSource(gitSource);
        expected.setInstruction(buildInstruction);

        try(InputStream in = BuildRequestTest.class.getResourceAsStream("/build-requests/java-maven-old.json")) {
            BuildRequest actual = objectMapper.readValue(in, BuildRequest.class);
            assertEquals(expected, actual);
        }
    }

}
