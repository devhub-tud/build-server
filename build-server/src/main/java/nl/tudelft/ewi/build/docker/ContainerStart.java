package nl.tudelft.ewi.build.docker;

import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Accessors(chain = true)
public class ContainerStart {
	
	@JsonProperty("Binds")
	private List<String> binds;
	
	@JsonProperty("LxcConf")
	private List<LxcConf> lxcConf;
	
}