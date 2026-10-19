package com.library.serviceimpl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.library.dto.BookRequest;
import com.library.dto.BookResponse;
import com.library.entity.Book;
import com.library.exception.ConflictException;
import com.library.exception.NotFoundException;
import com.library.repository.SqlSession;
import com.library.service.BookService;

@Service
public class BookServiceImpl implements BookService {

	private final SqlSession sqlSession;

	public BookServiceImpl(SqlSession sqlSession) {
		this.sqlSession = sqlSession;
	}

	@Override
	@Transactional
	public BookResponse create(BookRequest request) {
		if (sqlSession.isbnExists(request.isbn())) {
			throw new ConflictException("ISBN_ALREADY_EXISTS", "A book with this ISBN already exists");
		}
		Book book = Book.register(request.title(), request.author(), request.isbn(), request.totalCopies());
		return new BookResponse(sqlSession.save(book));
	}

	@Override
	@Transactional(readOnly = true)
	public BookResponse get(Long id) {
		return new BookResponse(find(id));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<BookResponse> list(int page, int size) {
		return sqlSession.findAll(Book.class, Pages.of(page, size)).map(BookResponse::new);
	}

	@Override
	@Transactional
	public BookResponse update(Long id, BookRequest request) {
		Book book = find(id);
		if (sqlSession.isbnExistsForOther(request.isbn(), id)) {
			throw new ConflictException("ISBN_ALREADY_EXISTS", "A book with this ISBN already exists");
		}
		try {
			book.resize(request.totalCopies());
		}
		catch (IllegalArgumentException exception) {
			throw new ConflictException("COPIES_BELOW_LOANS", exception.getMessage());
		}
		book.setTitle(request.title());
		book.setAuthor(request.author());
		book.setIsbn(request.isbn());
		return new BookResponse(sqlSession.save(book));
	}

	@Override
	@Transactional
	public void delete(Long id) {
		Book book = find(id);
		if (sqlSession.countOpenLoansByBook(id) > 0) {
			throw new ConflictException("BOOK_HAS_OPEN_LOANS", "Return every copy of this book before deleting it");
		}
		sqlSession.delete(book);
	}

	private Book find(Long id) {
		return sqlSession.find(Book.class, id).orElseThrow(() -> new NotFoundException("Book not found"));
	}

}
