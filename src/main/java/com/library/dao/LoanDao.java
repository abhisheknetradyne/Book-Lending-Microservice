package com.library.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.library.entity.Loan;
import com.library.session.SqlSession;

@Repository
public class LoanDao {

	private final SqlSession sqlSession;

	public LoanDao(SqlSession sqlSession) {
		this.sqlSession = sqlSession;
	}

	@Transactional
	public Loan save(Loan loan) {
		return sqlSession.save(loan);
	}

	@Transactional(readOnly = true)
	public Optional<Loan> findOpenLoan(Long memberId, Long bookId) {
		return sqlSession.session().createQuery(
						"select l from Loan l join fetch l.member join fetch l.book where l.member.id = :memberId and l.book.id = :bookId and l.returnedAt is null",
						Loan.class)
				.setParameter("memberId", memberId)
				.setParameter("bookId", bookId)
				.setMaxResults(1)
				.getResultStream()
				.findFirst();
	}

	@Transactional(readOnly = true)
	public Optional<Loan> findLatestLoan(Long memberId, Long bookId) {
		return sqlSession.session().createQuery(
						"select l from Loan l join fetch l.member join fetch l.book where l.member.id = :memberId and l.book.id = :bookId order by l.id desc",
						Loan.class)
				.setParameter("memberId", memberId)
				.setParameter("bookId", bookId)
				.setMaxResults(1)
				.getResultStream()
				.findFirst();
	}

	@Transactional(readOnly = true)
	public Optional<Loan> findLoan(Long id) {
		return sqlSession.session().createQuery(
						"select l from Loan l join fetch l.member join fetch l.book where l.id = :id", Loan.class)
				.setParameter("id", id)
				.setMaxResults(1)
				.getResultStream()
				.findFirst();
	}

	@Transactional(readOnly = true)
	public List<Loan> findOpenLoans(Long memberId) {
		return sqlSession.session().createQuery(
						"select l from Loan l join fetch l.book where l.member.id = :memberId and l.returnedAt is null",
						Loan.class)
				.setParameter("memberId", memberId)
				.getResultList();
	}

	@Transactional(readOnly = true)
	public Page<Loan> findLoans(Long memberId, Pageable pageable) {
		String filter = memberId == null ? "" : " where l.member.id = :memberId";
		var session = sqlSession.session();
		var query = session.createQuery(
						"select l from Loan l join fetch l.member join fetch l.book" + filter + " order by l.id desc",
						Loan.class);
		var totalQuery = session.createQuery("select count(l) from Loan l" + filter, Long.class);
		if (memberId != null) {
			query.setParameter("memberId", memberId);
			totalQuery.setParameter("memberId", memberId);
		}
		List<Loan> content = query
				.setFirstResult((int) pageable.getOffset())
				.setMaxResults(pageable.getPageSize())
				.getResultList();
		return new PageImpl<>(content, pageable, totalQuery.getSingleResult());
	}

	@Transactional(readOnly = true)
	public long countOpenLoansByBook(Long bookId) {
		return sqlSession.session().createQuery(
						"select count(l) from Loan l where l.book.id = :bookId and l.returnedAt is null", Long.class)
				.setParameter("bookId", bookId)
				.getSingleResult();
	}

	@Transactional(readOnly = true)
	public long countOpenLoans() {
		return sqlSession.session().createQuery("select count(l) from Loan l where l.returnedAt is null", Long.class)
				.getSingleResult();
	}

}
