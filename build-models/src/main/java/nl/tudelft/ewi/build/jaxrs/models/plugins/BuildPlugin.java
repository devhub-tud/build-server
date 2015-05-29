package nl.tudelft.ewi.build.jaxrs.models.plugins;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import nl.tudelft.ewi.build.jaxrs.models.BuildInstruction;

/**
 * @author Jan-Willem Gmelig Meyling
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public abstract class BuildPlugin<T extends BuildInstruction> {

    enum Type {
        MAVEN;
    }

    private Type type;

}
