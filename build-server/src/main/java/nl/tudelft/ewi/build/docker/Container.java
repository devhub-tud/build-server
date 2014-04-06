package nl.tudelft.ewi.build.docker;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Container {
	
	@JsonProperty("Hostname")
	private String hostname = "";
	
	@JsonProperty("User")
	private String user = "";
	
	@JsonProperty("Memory")
	private Integer memory = 0;
	
	@JsonProperty("MemorySwap")
	private Integer memorySwap = 0;
	
	@JsonProperty("AttachStdin")
	private Boolean attachStdin = false;
	
	@JsonProperty("AttachStdout")
	private Boolean attachStdout = false;
	
	@JsonProperty("AttachStderr")
	private Boolean attachStderr = false;
	
	@JsonProperty("PortSpecs")
	private Object portSpecs = null;
	
	@JsonProperty("Tty")
	private Boolean tty = false;
	
	@JsonProperty("OpenStdin")
	private Boolean openStdin = false;
	
	@JsonProperty("StdinOnce")
	private Boolean stdinOnce = false;
	
	@JsonProperty("Env")
	private Object env = null;
	
	@JsonProperty("Cmd")
	private List<String> cmd;
	
	@JsonProperty("Dns")
	private Object dns = null;
	
	@JsonProperty("Image")
	private String image;
	
	@JsonProperty("Volumes")
	private Object volumes = null;
	
	@JsonProperty("VolumesFrom")
	private String volumesFrom = "";
	
	@JsonProperty("WorkingDir")
	private String workingDir = "";
	
	@JsonProperty("ExposedPorts")
	private Object exposedPorts = null;
	
}