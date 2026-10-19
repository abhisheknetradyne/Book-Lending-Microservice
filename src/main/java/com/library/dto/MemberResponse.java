package com.library.dto;

import com.library.entity.Member;

public record MemberResponse(Long id, String name, String email) {

	public MemberResponse(Member member) {
		this(member.getId(), member.getName(), member.getEmail());
	}

}
