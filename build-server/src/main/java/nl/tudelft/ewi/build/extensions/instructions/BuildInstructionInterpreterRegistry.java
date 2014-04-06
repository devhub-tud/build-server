package nl.tudelft.ewi.build.extensions.instructions;

import java.util.Map;

import nl.tudelft.ewi.build.jaxrs.models.BuildInstruction;
import nl.tudelft.ewi.build.jaxrs.models.MavenBuildInstruction;

import com.google.common.collect.ImmutableMap;

public class BuildInstructionInterpreterRegistry {

	private final Map<Class<? extends BuildInstruction>, BuildInstructionInterpreter<?>> registry;

	public BuildInstructionInterpreterRegistry() {
		this.registry = ImmutableMap.<Class<? extends BuildInstruction>, BuildInstructionInterpreter<?>> builder()
				.put(MavenBuildInstruction.class, new MavenBuildInstructionInterpreter())
				.build();
	}
	
	@SuppressWarnings("unchecked")
	public <T extends BuildInstruction> BuildInstructionInterpreter<T> getBuildDecorator(Class<T> instructionType) {
		return (BuildInstructionInterpreter<T>) registry.get(instructionType);
	}

}
