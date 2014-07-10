package nl.tudelft.ewi.build.docker;

import lombok.Data;

@Data 
class Error {
	private String error;
	private Error.Detail errorDetail;
	
	@Data
	static class Detail {
		private int code;
		private String message;
	}
}