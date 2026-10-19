package com.library.serviceimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.library.dao.BookDao;
import com.library.dao.LoanDao;
import com.library.dto.BookRequest;
import com.library.dto.BookResponse;
import com.library.entity.Book;
import com.library.exception.ConflictException;
import com.library.exception.NotFoundException;
import com.library.service.BookService;

@Service
public class BookServiceImpl implements BookService {

	private static final Logger log = LoggerFactory.getLogger(BookServiceImpl.class);

	private final BookDao books;
	private final LoanDao loans;

	public BookServiceImpl(BookDao books, LoanDao loans) {
		this.books = books;
		this.loans = loans;
	}

	@Override
	@Transactional
	public BookResponse create(BookRequest request) {
		log.info("create book isbn={}", request.isbn());
		if (books.isbnExists(request.isbn())) {
			log.warn("create book rejected isbn={} code=ISBN_ALREADY_EXISTS", request.isbn());
			throw new ConflictException("ISBN_ALREADY_EXISTS", "A book with this ISBN already exists");
		}
		Book book = Book.register(request.title(), request.author(), request.isbn(), request.totalCopies());
		book = books.save(book);
		log.info("create book succeeded bookId={}", book.getId());
		return new BookResponse(book);
	}

	@Override
	@Transactional(readOnly = true)
	public BookResponse get(Long id) {
		return new BookResponse(find(id));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<BookResponse> list(int page, int size) {
		return books.findAll(Pages.of(page, size)).map(BookResponse::new);
	}

	@Override
	@Transactional
	public BookResponse update(Long id, BookRequest request) {
		log.info("update book bookId={}", id);
		Book book = find(id);
		if (books.isbnExistsForOther(request.isbn(), id)) {
			log.warn("update book rejected bookId={} code=ISBN_ALREADY_EXISTS", id);
			throw new ConflictException("ISBN_ALREADY_EXISTS", "A book with this ISBN already exists");
		}
		try {
			book.resize(request.totalCopies());
		}
		catch (IllegalArgumentException exception) {
			log.warn("update book rejected bookId={} code=COPIES_BELOW_LOANS message={}", id, exception.getMessage());
			throw new ConflictException("COPIES_BELOW_LOANS", exception.getMessage());
		}
		book.setTitle(request.title());
		book.setAuthor(request.author());
		book.setIsbn(request.isbn());
		book = books.save(book);
		log.info("update book succeeded bookId={}", book.getId());
		return new BookResponse(book);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		log.info("delete book bookId={}", id);
		Book book = find(id);
		if (loans.countOpenLoansByBook(id) > 0) {
			log.warn("delete book rejected bookId={} code=BOOK_HAS_OPEN_LOANS", id);
			throw new ConflictException("BOOK_HAS_OPEN_LOANS", "Return every copy of this book before deleting it");
		}
		books.delete(book);
		log.info("delete book succeeded bookId={}", id);
	}

	private Book find(Long id) {
		return books.findById(id).orElseThrow(() -> {
			log.warn("book not found bookId={}", id);
			return new NotFoundException("Book not found");
		});
	}

}
