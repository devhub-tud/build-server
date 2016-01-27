package nl.tudelft.ewi.build.jaxrs.models.plugins;

import lombok.Data;
import lombok.EqualsAndHashCode;
import nl.tudelft.ewi.build.jaxrs.models.MavenBuildInstruction;

/**
 * @author Jan-Willem Gmelig Meyling
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MavenBuildPlugin extends FileHookPlugin<MavenBuildInstruction> {

    public MavenBuildPlugin() {
        super(Type.MAVEN);
    }

    private String[] phases;

}
