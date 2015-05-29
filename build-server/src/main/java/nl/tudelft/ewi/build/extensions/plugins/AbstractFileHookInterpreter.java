package nl.tudelft.ewi.build.extensions.plugins;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import nl.tudelft.ewi.build.Config;
import nl.tudelft.ewi.build.jaxrs.models.BuildInstruction;
import nl.tudelft.ewi.build.jaxrs.models.plugins.FileHookPlugin;
import org.jboss.resteasy.util.Base64;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import java.io.File;

/**
 * The {@code AbstractFileHookInterpreter} can be used for implementations of
 * {@link FileHookPlugin} that want to send a file from the build to a destination.
 *
 * @author Jan-Willem Gmelig Meyling
 */
@Slf4j
public abstract class AbstractFileHookInterpreter<T extends FileHookPlugin<V>, V extends BuildInstruction> implements PluginInterpreter<T, V> {

    private final Config config;

    @Inject
    public AbstractFileHookInterpreter(Config config) {
        this.config = config;
    }

    @Override
    public void after(T plugin, V buildInstruction, File stagingDirectory) {
        String userPass = config.getClientId() + ":"
                + config.getClientSecret();
        String authorization = "Basic "
                + Base64.encodeBytes(userPass.getBytes());
        File file = new File(stagingDirectory, plugin.getFilePath());

        if(file.exists()) {
            Client client = ClientBuilder.newClient();

            try {
                log.info("Sending {}", plugin);
                client.target(plugin.getCallbackUrl())
                    .request()
                    .header("Authorization", authorization)
                    .post(Entity.entity(file, plugin.getContentType()));
            }
            finally {
                client.close();
            }
        }
        else {
            log.warn("Requested file for {} did not exist", plugin);
        }
    }

}
