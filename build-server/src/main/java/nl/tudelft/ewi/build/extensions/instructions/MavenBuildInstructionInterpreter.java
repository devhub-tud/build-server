package nl.tudelft.ewi.build.extensions.instructions;

import java.util.List;

import nl.tudelft.ewi.build.jaxrs.models.MavenBuildInstruction;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class MavenBuildInstructionInterpreter implements BuildInstructionInterpreter<MavenBuildInstruction> {

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

}
