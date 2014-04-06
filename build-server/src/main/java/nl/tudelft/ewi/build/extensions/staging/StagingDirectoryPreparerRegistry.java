package nl.tudelft.ewi.build.extensions.staging;

import java.util.Map;

import nl.tudelft.ewi.build.jaxrs.models.GitSource;
import nl.tudelft.ewi.build.jaxrs.models.Source;

import com.google.common.collect.ImmutableMap;

public class StagingDirectoryPreparerRegistry {

	private final Map<Class<? extends Source>, StagingDirectoryPreparer<?>> registry;

	public StagingDirectoryPreparerRegistry() {
		this.registry = ImmutableMap.<Class<? extends Source>, StagingDirectoryPreparer<?>> builder()
				.put(GitSource.class, new GitStagingDirectoryPreparer())
				.build();
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Source> StagingDirectoryPreparer<T> getStagingDirectoryPreparer(Class<T> sourceType) {
		return (StagingDirectoryPreparer<T>) registry.get(sourceType);
	}

}
