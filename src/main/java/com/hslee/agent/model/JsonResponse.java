package com.hslee.agent.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class JsonResponse {
	private List<PassengerModel> update;
	private List<PassengerModel> delete;
}
