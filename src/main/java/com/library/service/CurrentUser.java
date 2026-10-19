package com.library.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.library.config.LibraryUser;
import com.library.entity.Member;
import com.library.exception.ForbiddenException;
import com.library.exception.NotFoundException;
import com.library.dao.MemberDao;

@Component
public class CurrentUser {

	private final MemberDao members;

	public CurrentUser(MemberDao members) {
		this.members = members;
	}

	public boolean librarian() {
		Authentication authentication = authentication();
		return authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals("ROLE_LIBRARIAN"));
	}

	public Member requireMemberAccount() {
		LibraryUser user = principal();
		if (user.memberEmail() == null || user.memberEmail().isBlank()) {
			throw new ForbiddenException("This account is not linked to a library member");
		}
		return members.findMemberByEmail(user.memberEmail())
				.orElseThrow(() -> new NotFoundException("No library member matches this login"));
	}

	public void assertCanActFor(Long memberId) {
		if (librarian()) {
			return;
		}
		Member self = requireMemberAccount();
		if (!self.getId().equals(memberId)) {
			throw new ForbiddenException("Members can only act on their own account");
		}
	}
	private LibraryUser principal() {
		Object principal = authentication().getPrincipal();
		if (principal instanceof LibraryUser user) {
			return user;
		}
		throw new ForbiddenException("Authentication is required");
	}

	private Authentication authentication() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new ForbiddenException("Authentication is required");
		}
		return authentication;
	}

}
