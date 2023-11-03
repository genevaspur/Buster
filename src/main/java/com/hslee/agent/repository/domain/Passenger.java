package com.hslee.agent.repository.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@RequiredArgsConstructor
@NoArgsConstructor
@Table("passenger")
public class Passenger implements Persistable<String> {

	@Id
	@Column("USER_ID")
	@NonNull
	private String userId;

	@Column("USER_NM")
	@NonNull
	private String userNm;

	@Column("COMP_NM")
	@NonNull
	private String compNm;

	@Column("TEAM_NM")
	@NonNull
	private String teamNm;

	// insert, update 시 isNew 값에 따라 insert, update 쿼리를 날린다.
	@Transient
	private boolean isNew;

	public Passenger copyFrom(Passenger source) {
		return new Passenger(source.getUserId(), source.getUserNm(), source.getCompNm(), source.getTeamNm());
	}

	@Override
	public String getId() {
		return this.userId;
	}

	@Override
	public boolean isNew() {
		return isNew;
	}
}
