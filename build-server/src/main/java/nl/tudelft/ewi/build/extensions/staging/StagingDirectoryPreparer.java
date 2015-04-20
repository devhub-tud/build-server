package nl.tudelft.ewi.build.extensions.staging;

import java.io.File;
import java.io.IOException;

import nl.tudelft.ewi.build.builds.Logger;
import nl.tudelft.ewi.build.jaxrs.models.Source;

public interface StagingDirectoryPreparer<T extends Source> {
	
	void prepareStagingDirectory(T source, Logger logger, File stagingDirectory) throws IOException;
	
}
