package nl.tudelft.ewi.build.extensions.instructions;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import nl.tudelft.ewi.build.jaxrs.models.BuildInstruction;
import nl.tudelft.ewi.build.jaxrs.models.MavenBuildInstruction;

import java.util.Map;

public class BuildInstructionInterpreterRegistry {

	private final Map<Class<? extends BuildInstruction>, BuildInstructionInterpreter<?>> registry;

	@Inject
	public BuildInstructionInterpreterRegistry(MavenBuildInstructionInterpreter mavenBuildInstructionInterpreter) {
		this.registry = ImmutableMap.<Class<? extends BuildInstruction>, BuildInstructionInterpreter<?>> builder()
				.put(MavenBuildInstruction.class, mavenBuildInstructionInterpreter)
				.build();
	}
	
	@SuppressWarnings("unchecked")
	public <T extends BuildInstruction> BuildInstructionInterpreter<T> getBuildDecorator(Class<T> instructionType) {
		return (BuildInstructionInterpreter<T>) registry.get(instructionType);
	}

}
