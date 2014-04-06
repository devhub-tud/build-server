package nl.tudelft.ewi.build.extensions.instructions;

import java.io.File;
import java.util.List;

import nl.tudelft.ewi.build.docker.DefaultLogger;
import nl.tudelft.ewi.build.jaxrs.models.MavenBuildResult;
import nl.tudelft.ewi.build.jaxrs.models.MavenBuildInstruction;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class MavenBuildInstructionInterpreter extends DefaultBuildInstructionInterpreter<MavenBuildInstruction> {

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
		partials.add("mvn");
		for (String phase : instruction.getPhases()) {
			partials.add(phase);
		}
		
		return Joiner.on(" ").join(partials);
	}

	@Override
	public MavenBuildResult createResult(MavenBuildInstruction instruction, DefaultLogger logger, File stagingDirectory) {
		MavenBuildResult result = new MavenBuildResult();
		setLogAndStatus(logger, result);
		return result;
	}
	
}
