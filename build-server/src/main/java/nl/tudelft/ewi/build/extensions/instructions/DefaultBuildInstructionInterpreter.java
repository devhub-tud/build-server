package nl.tudelft.ewi.build.extensions.instructions;

import java.io.File;

import nl.tudelft.ewi.build.docker.DefaultLogger;
import nl.tudelft.ewi.build.jaxrs.models.BuildInstruction;
import nl.tudelft.ewi.build.jaxrs.models.BuildResult;
import nl.tudelft.ewi.build.jaxrs.models.BuildResult.Status;

public abstract class DefaultBuildInstructionInterpreter<T extends BuildInstruction> implements BuildInstructionInterpreter<T> {

	@Override
	public BuildResult createResult(T instruction, DefaultLogger logger, File stagingDirectory) {
		BuildResult result = new BuildResult();
		setLogAndStatus(logger, result);
		return result;
	}
	
	protected void setLogAndStatus(DefaultLogger logger, BuildResult result) {
		int exitCode = logger.getExitCode().get();
		Status status = Status.SUCCEEDED;
		if (exitCode != 0) {
			status = Status.FAILED;
		}
		
		result.setStatus(status);
		result.setLogLines(logger.getLogLines());
	}

}
