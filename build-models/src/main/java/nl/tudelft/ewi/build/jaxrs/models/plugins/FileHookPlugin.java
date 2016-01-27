package nl.tudelft.ewi.build.jaxrs.models.plugins;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import nl.tudelft.ewi.build.jaxrs.models.BuildInstruction;

/**
 * @author Jan-Willem Gmelig Meyling
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public abstract class FileHookPlugin<T extends BuildInstruction> extends BuildPlugin<T> {

    protected FileHookPlugin(Type type) {
        super(type);
    }

    private String callbackUrl;

    private String filePath;

    private String contentType;

}
