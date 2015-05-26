package nl.tudelft.ewi.build.jaxrs.models;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Jan-Willem Gmelig Meyling
 */
@Data
@EqualsAndHashCode
public class FileRequest {

    private String relativePath;

    private String callbackUrl;

}
