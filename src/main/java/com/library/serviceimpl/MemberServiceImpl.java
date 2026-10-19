package com.library.serviceimpl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.library.dto.MemberRequest;
import com.library.dto.MemberResponse;
import com.library.dto.MemberUpdateRequest;
import com.library.entity.Member;
import com.library.exception.ConflictException;
import com.library.exception.NotFoundException;
import com.library.repository.SqlSession;
import com.library.service.CurrentUser;
import com.library.service.MemberService;

@Service
public class MemberServiceImpl implements MemberService {

	private final SqlSession sqlSession;
	private final CurrentUser currentUser;

	public MemberServiceImpl(SqlSession sqlSession, CurrentUser currentUser) {
		this.sqlSession = sqlSession;
		this.currentUser = currentUser;
	}

	@Override
	@Transactional
	public MemberResponse create(MemberRequest request) {
		if (sqlSession.memberEmailExists(request.email())) {
			throw new ConflictException("EMAIL_ALREADY_EXISTS", "A member with this email already exists");
		}
		Member member = Member.register(request.name(), request.email());
		return new MemberResponse(sqlSession.save(member));
	}

	@Override
	@Transactional(readOnly = true)
	public MemberResponse get(Long id) {
		currentUser.assertCanReadMember(id);
		return new MemberResponse(find(id));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<MemberResponse> list(int page, int size) {
		return sqlSession.findAll(Member.class, Pages.of(page, size)).map(MemberResponse::new);
	}

	@Override
	@Transactional
	public MemberResponse update(Long id, MemberUpdateRequest request) {
		Member member = find(id);
		member.setName(request.name());
		return new MemberResponse(sqlSession.save(member));
	}

	private Member find(Long id) {
		return sqlSession.find(Member.class, id).orElseThrow(() -> new NotFoundException("Member not found"));
	}

}
