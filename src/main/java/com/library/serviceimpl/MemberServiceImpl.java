package com.library.serviceimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.library.dao.MemberDao;
import com.library.dto.MemberRequest;
import com.library.dto.MemberResponse;
import com.library.dto.MemberUpdateRequest;
import com.library.entity.Member;
import com.library.exception.ConflictException;
import com.library.exception.NotFoundException;
import com.library.service.CurrentUser;
import com.library.service.MemberService;

@Service
public class MemberServiceImpl implements MemberService {

	private static final Logger log = LoggerFactory.getLogger(MemberServiceImpl.class);

	private final MemberDao members;
	private final CurrentUser currentUser;
	private final PasswordEncoder passwordEncoder;

	public MemberServiceImpl(MemberDao members, CurrentUser currentUser, PasswordEncoder passwordEncoder) {
		this.members = members;
		this.currentUser = currentUser;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public MemberResponse create(MemberRequest request) {
		log.info("create member email={}", request.email());
		if (members.memberEmailExists(request.email())) {
			log.warn("create member rejected email={} code=EMAIL_ALREADY_EXISTS", request.email());
			throw new ConflictException("EMAIL_ALREADY_EXISTS", "A member with this email already exists");
		}
		String encodedPassword = passwordEncoder.encode(request.password());
		Member member = members.save(Member.register(request.name(), request.email(), encodedPassword));
		log.info("create member succeeded memberId={}", member.getId());
		return new MemberResponse(member);
	}

	@Override
	@Transactional(readOnly = true)
	public MemberResponse get(Long id) {
		currentUser.assertCanActFor(id);
		return new MemberResponse(find(id));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<MemberResponse> list(int page, int size) {
		return members.findAll(Pages.of(page, size)).map(MemberResponse::new);
	}

	@Override
	@Transactional
	public MemberResponse update(Long id, MemberUpdateRequest request) {
		log.info("update member memberId={}", id);
		Member member = find(id);
		member.setName(request.name());
		member = members.save(member);
		log.info("update member succeeded memberId={}", member.getId());
		return new MemberResponse(member);
	}

	private Member find(Long id) {
		return members.findById(id).orElseThrow(() -> {
			log.warn("member not found memberId={}", id);
			return new NotFoundException("Member not found");
		});
	}

}
