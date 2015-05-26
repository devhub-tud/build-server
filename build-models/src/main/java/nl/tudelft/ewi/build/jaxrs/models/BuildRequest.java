package nl.tudelft.ewi.build.jaxrs.models;

import lombok.Data;

import java.util.List;

@Data
public class BuildRequest {

	private Source source;

	private BuildInstruction instruction;

	private String callbackUrl;

	private Integer timeout;

	private List<FileRequest> fileRequests;

}
