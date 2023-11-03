package com.hslee.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hslee.agent.repository.domain.Passenger;
import lombok.*;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PassengerModel {
	@JsonProperty("USER_ID")
	private String USER_ID;

	@JsonProperty("USER_NM")
	private String USER_NM;

	@JsonProperty("COMP_NM")
	private String COMP_NM;

	@JsonProperty("TEAM_NM")
	private String TEAM_NM;

	public Passenger toEntity() {
		return new Passenger(USER_ID, USER_NM, COMP_NM, TEAM_NM);
	}
}
