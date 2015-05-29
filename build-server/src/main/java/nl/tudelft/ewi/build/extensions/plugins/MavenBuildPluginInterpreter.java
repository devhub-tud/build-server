package nl.tudelft.ewi.build.extensions.plugins;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import nl.tudelft.ewi.build.Config;
import nl.tudelft.ewi.build.jaxrs.models.MavenBuildInstruction;
import nl.tudelft.ewi.build.jaxrs.models.plugins.MavenBuildPlugin;

import java.io.File;
import java.util.List;

/**
 * The {@code MavenBuildPluginInterpreter} is a {@link AbstractFileHookInterpreter} that
 * generates the sent files through maven execution phases.
 *
 * @author Jan-Willem Gmelig Meyling
 */
@Slf4j
public class MavenBuildPluginInterpreter extends AbstractFileHookInterpreter<MavenBuildPlugin, MavenBuildInstruction> {

    @Inject
    public MavenBuildPluginInterpreter(Config config) {
        super(config);
    }

    @Override
    public void before(MavenBuildPlugin plugin, MavenBuildInstruction buildInstruction, File stagingDirectory) {
        if(plugin.getPhases() != null) {
            copyPhasesToInstruction(plugin, buildInstruction);
        }
    }

    protected void copyPhasesToInstruction(MavenBuildPlugin plugin, MavenBuildInstruction buildInstruction) {
        List<String> phases = Lists.newArrayList(buildInstruction.getPhases());

        for(String phase : plugin.getPhases()) {
            phases.add(phase);
        }

        String[] phasesArr = new String[phases.size()];
        phases.toArray(phasesArr);
        buildInstruction.setPhases(phasesArr);
    }

}
