package nl.tudelft.ewi.build.docker;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LxcConf {
	
	@JsonProperty("Key")
	private String key;
	
	@JsonProperty("Value")
	private String value;
	
}