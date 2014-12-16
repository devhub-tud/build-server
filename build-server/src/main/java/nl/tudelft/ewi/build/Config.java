package nl.tudelft.ewi.build;


public interface Config {

	int getHttpPort();
	
	int getMaximumConcurrentJobs();
	
	String getDockerHost();
	
	String getStagingDirectory();
	
	String getWorkingDirectory();
	
	String getClientId();
	
	String getClientSecret();

	String getCertificateDirectory();
	
}
