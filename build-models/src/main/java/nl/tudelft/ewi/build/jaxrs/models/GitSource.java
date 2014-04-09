package nl.tudelft.ewi.build.jaxrs.models;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GitSource extends Source {
	
	public GitSource() {
		setType(Type.GIT);
	}

	private String repositoryUrl;
	
	private String branchName;
	
	private String commitId;
	
}
