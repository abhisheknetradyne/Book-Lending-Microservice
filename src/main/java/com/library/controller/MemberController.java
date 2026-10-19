package com.library.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.library.dto.MemberRequest;
import com.library.dto.MemberResponse;
import com.library.dto.MemberUpdateRequest;
import com.library.dto.PageResponse;
import com.library.service.MemberService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/v1/members")
@Tag(name = "Members", description = "Library members")
@SecurityRequirement(name = "basicAuth")
public class MemberController {

	private final MemberService members;

	public MemberController(MemberService members) {
		this.members = members;
	}

	@GetMapping
	@Operation(summary = "List members", description = "Librarian only")
	public PageResponse<MemberResponse> list(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return PageResponse.from(members.list(page, size));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get one member", description = "A librarian can read any member. A member can read only their own record.")
	public MemberResponse get(@PathVariable @Positive Long id) {
		return members.get(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register a member", description = "Librarian only")
	public MemberResponse create(@Valid @RequestBody MemberRequest request) {
		return members.create(request);
	}

	@PatchMapping("/{id}")
	@Operation(summary = "Update a member name", description = "Librarian only")
	public MemberResponse update(@PathVariable @Positive Long id, @Valid @RequestBody MemberUpdateRequest request) {
		return members.update(id, request);
	}

}
