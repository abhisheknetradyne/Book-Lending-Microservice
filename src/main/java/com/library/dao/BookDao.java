package com.library.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.library.entity.Book;
import com.library.session.SqlSession;

@Repository
public class BookDao {

	private final SqlSession sqlSession;

	public BookDao(SqlSession sqlSession) {
		this.sqlSession = sqlSession;
	}

	@Transactional(readOnly = true)
	public Optional<Book> findById(Long id) {
		return sqlSession.find(Book.class, id);
	}

	@Transactional
	public Optional<Book> findByIdForUpdate(Long id) {
		return sqlSession.findForUpdate(Book.class, id);
	}

	@Transactional
	public Book save(Book book) {
		return sqlSession.save(book);
	}

	@Transactional
	public void delete(Book book) {
		sqlSession.delete(book);
	}

	@Transactional(readOnly = true)
	public Page<Book> findAll(Pageable pageable) {
		var session = sqlSession.session();
		List<Book> content = session.createQuery("select b from Book b order by b.id desc", Book.class)
				.setFirstResult((int) pageable.getOffset())
				.setMaxResults(pageable.getPageSize())
				.getResultList();
		long total = session.createQuery("select count(b) from Book b", Long.class).getSingleResult();
		return new PageImpl<>(content, pageable, total);
	}

	@Transactional(readOnly = true)
	public boolean isbnExists(String isbn) {
		Long count = sqlSession.session().createQuery(
						"select count(b) from Book b where lower(b.isbn) = lower(:isbn)", Long.class)
				.setParameter("isbn", isbn)
				.getSingleResult();
		return count > 0;
	}

	@Transactional(readOnly = true)
	public boolean isbnExistsForOther(String isbn, Long id) {
		Long matches = sqlSession.session().createQuery(
						"select count(b) from Book b where lower(b.isbn) = lower(:isbn) and b.id <> :id", Long.class)
				.setParameter("isbn", isbn)
				.setParameter("id", id)
				.getSingleResult();
		return matches > 0;
	}

}
