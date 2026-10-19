package com.library.service;

import org.springframework.data.domain.Page;

import com.library.dto.MemberRequest;
import com.library.dto.MemberResponse;
import com.library.dto.MemberUpdateRequest;

public interface MemberService {

	MemberResponse create(MemberRequest request);

	MemberResponse get(Long id);

	Page<MemberResponse> list(int page, int size);

	MemberResponse update(Long id, MemberUpdateRequest request);

}
