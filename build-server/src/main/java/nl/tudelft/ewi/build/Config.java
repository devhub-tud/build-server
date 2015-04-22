package nl.tudelft.ewi.build;


public interface Config {

	int getHttpPort();
	
	int getMaximumConcurrentJobs();
	
	String getStagingDirectory();

	String getClientId();
	
	String getClientSecret();

	String getDockerUser();

}
