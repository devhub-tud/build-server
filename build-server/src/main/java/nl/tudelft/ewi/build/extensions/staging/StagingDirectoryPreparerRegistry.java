package nl.tudelft.ewi.build.extensions.staging;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import nl.tudelft.ewi.build.jaxrs.models.GitSource;
import nl.tudelft.ewi.build.jaxrs.models.Source;

import java.util.Map;

public class StagingDirectoryPreparerRegistry {

	private final Map<Class<? extends Source>, StagingDirectoryPreparer<?>> registry;

	@Inject
	public StagingDirectoryPreparerRegistry(GitStagingDirectoryPreparer gitStagingDirectoryPreparer) {
		this.registry = ImmutableMap.<Class<? extends Source>, StagingDirectoryPreparer<?>> builder()
				.put(GitSource.class, gitStagingDirectoryPreparer)
				.build();
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Source> StagingDirectoryPreparer<T> getStagingDirectoryPreparer(Class<T> sourceType) {
		return (StagingDirectoryPreparer<T>) registry.get(sourceType);
	}

}
