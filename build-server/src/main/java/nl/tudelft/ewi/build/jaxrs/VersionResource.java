package nl.tudelft.ewi.build.jaxrs;

import nl.tudelft.ewi.build.jaxrs.models.Version;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author Jan-Willem Gmelig Meyling
 */
@Path("api/version")
@Produces(MediaType.APPLICATION_JSON)
public class VersionResource {

    /**
     * @return the GitServer version
     */
    @GET
    public Version getVersion() {
        Package buildServerPackage = VersionResource.class.getPackage();
        Version version = new Version();
        version.setVersion(buildServerPackage.getImplementationVersion());
        return version;
    }

}
