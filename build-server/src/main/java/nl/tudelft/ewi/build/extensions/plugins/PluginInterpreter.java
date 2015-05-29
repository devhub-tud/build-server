package nl.tudelft.ewi.build.extensions.plugins;

import nl.tudelft.ewi.build.jaxrs.models.BuildInstruction;
import nl.tudelft.ewi.build.jaxrs.models.plugins.BuildPlugin;

import java.io.File;

/**
 * Plugins add functionality to the build and can interact with the
 * staging directory of the container before and after the build.
 *
 * @author Jan-Willem Gmelig Meyling
 */
public interface PluginInterpreter<T extends BuildPlugin<V>, V extends BuildInstruction> {

    /**
     * Interact with the staging directory before the build.
     *
     * @param plugin plugin to execute
     * @param buildInstruction instruction that will be executed
     * @param stagingDirectory staging directory
     */
    void before(T plugin, V buildInstruction, File stagingDirectory);

    /**
     * Interact with the staging directory after the build.
     *
     * @param plugin plugin to execute
     * @param buildInstruction instruction that was executed
     * @param stagingDirectory staging directory
     */
    void after(T plugin, V buildInstruction, File stagingDirectory);

}
