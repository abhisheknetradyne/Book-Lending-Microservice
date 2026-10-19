package com.library.service;

import org.springframework.data.domain.Page;

import com.library.dto.BookRequest;
import com.library.dto.BookResponse;

public interface BookService {

	BookResponse create(BookRequest request);

	BookResponse get(Long id);

	Page<BookResponse> list(int page, int size);

	BookResponse update(Long id, BookRequest request);

	void delete(Long id);

}
