package com.library.dto;

import com.library.entity.Book;

public record BookResponse(
		Long id,
		String title,
		String author,
		String isbn,
		int totalCopies,
		int availableCopies) {

	public BookResponse(Book book) {
		this(book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn(), book.getTotalCopies(), book.getAvailableCopies());
	}

}
