package nl.tudelft.ewi.build.docker;

public interface ImageBuildObserver {
	
	void onMessage(String message);
	
	void onError(String error);
	
	void onCompleted();

}
