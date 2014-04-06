package nl.tudelft.ewi.build.docker;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class StatusCode {
	@JsonProperty(required = false)
	private Integer StatusCode;
}