package com.library.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.library.dto.BookRequest;
import com.library.dto.BookResponse;
import com.library.dto.PageResponse;
import com.library.service.BookService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Books", description = "Catalog of books")
@SecurityRequirement(name = "basicAuth")
public class BookController {

	private final BookService books;

	public BookController(BookService books) {
		this.books = books;
	}

	@GetMapping
	@Operation(summary = "List books")
	public PageResponse<BookResponse> list(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return PageResponse.from(books.list(page, size));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get one book")
	public BookResponse get(@PathVariable @Positive Long id) {
		return books.get(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Add a book", description = "Librarian only")
	public BookResponse create(@Valid @RequestBody BookRequest request) {
		return books.create(request);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a book", description = "Librarian only. Total copies cannot drop below the number currently on loan.")
	public BookResponse update(@PathVariable @Positive Long id, @Valid @RequestBody BookRequest request) {
		return books.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a book", description = "Librarian only. Refused while a copy is on loan.")
	public void delete(@PathVariable @Positive Long id) {
		books.delete(id);
	}

}
