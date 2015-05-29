package nl.tudelft.ewi.build.extensions.instructions;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import nl.tudelft.ewi.build.extensions.plugins.MavenBuildPluginInterpreter;
import nl.tudelft.ewi.build.jaxrs.models.MavenBuildInstruction;
import nl.tudelft.ewi.build.jaxrs.models.plugins.MavenBuildPlugin;

import java.io.File;
import java.util.List;

@Slf4j
public class MavenBuildInstructionInterpreter implements BuildInstructionInterpreter<MavenBuildInstruction> {

	private final MavenBuildPluginInterpreter mavenBuildPluginInterpreter;

	@Inject
	public MavenBuildInstructionInterpreter(MavenBuildPluginInterpreter mavenBuildPluginInterpreter) {
		this.mavenBuildPluginInterpreter = mavenBuildPluginInterpreter;
	}

	@Override
	public String getImage(MavenBuildInstruction instruction) {
		return "java-maven";
	}

	@Override
	public String getCommand(MavenBuildInstruction instruction) {
		List<String> partials = Lists.newArrayList();
		if (instruction.isWithDisplay()) {
			partials.add("with-xvfb");
		}
		partials.add("mvn -B");
		for (String phase : instruction.getPhases()) {
			partials.add(phase);
		}
		return Joiner.on(" ").join(partials);
	}

	@Override
	public void runPluginBefores(MavenBuildInstruction instruction, File stagingDirectory) {
		for(MavenBuildPlugin mavenBuildPlugin : instruction.getPlugins()) {
			try {
				mavenBuildPluginInterpreter.before(mavenBuildPlugin, instruction, stagingDirectory);
			}
			catch (Exception e) {
				log.warn("Failed to execute build plugin {}", e, mavenBuildPlugin);
			}
		}
	}

	@Override
	public void runPluginAfters(MavenBuildInstruction instruction, File stagingDirectory) {
		for(MavenBuildPlugin mavenBuildPlugin : instruction.getPlugins()) {
			try {
				mavenBuildPluginInterpreter.after(mavenBuildPlugin, instruction, stagingDirectory);
			}
			catch (Exception e) {
				log.warn("Failed to execute build plugin {}", e, mavenBuildPlugin);
			}
		}
	}

}
