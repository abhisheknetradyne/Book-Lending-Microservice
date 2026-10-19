package com.library.repository;

import java.util.List;
import java.util.Optional;

import org.hibernate.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.library.entity.Loan;
import com.library.entity.Member;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

@Repository
public class SqlSession {

	@PersistenceContext
	private EntityManager entityManager;

	@Transactional
	public <T> T save(T entity) {
		Session session = session();
		T stored = identifier(entity) == null || session.contains(entity) ? persistIfNew(session, entity) : session.merge(entity);
		session.flush();
		return stored;
	}

	@Transactional
	public void delete(Object entity) {
		Session session = session();
		session.remove(session.contains(entity) ? entity : session.merge(entity));
		session.flush();
	}

	@Transactional(readOnly = true)
	public <T> Optional<T> find(Class<T> type, Long id) {
		return Optional.ofNullable(session().find(type, id));
	}

	@Transactional
	public <T> Optional<T> findForUpdate(Class<T> type, Long id) {
		return Optional.ofNullable(session().find(type, id, LockModeType.PESSIMISTIC_WRITE));
	}

	@Transactional(readOnly = true)
	public <T> Page<T> findAll(Class<T> type, Pageable pageable) {
		String entity = type.getSimpleName();
		return page(
				"select e from " + entity + " e order by e.id desc",
				"select count(e) from " + entity + " e",
				type,
				pageable);
	}

	@Transactional(readOnly = true)
	public boolean isbnExists(String isbn) {
		return count("select count(b) from Book b where lower(b.isbn) = lower(:isbn)", "isbn", isbn) > 0;
	}

	@Transactional(readOnly = true)
	public boolean isbnExistsForOther(String isbn, Long id) {
		Long matches = session().createQuery(
						"select count(b) from Book b where lower(b.isbn) = lower(:isbn) and b.id <> :id", Long.class)
				.setParameter("isbn", isbn)
				.setParameter("id", id)
				.getSingleResult();
		return matches > 0;
	}

	@Transactional(readOnly = true)
	public boolean memberEmailExists(String email) {
		return count("select count(m) from Member m where lower(m.email) = lower(:email)", "email", email) > 0;
	}

	@Transactional(readOnly = true)
	public Optional<Member> findMemberByEmail(String email) {
		return session().createQuery("select m from Member m where lower(m.email) = lower(:email)", Member.class)
				.setParameter("email", email)
				.setMaxResults(1)
				.getResultStream()
				.findFirst();
	}

	@Transactional(readOnly = true)
	public Optional<Loan> findLoan(Long id) {
		return session().createQuery(
						"select l from Loan l join fetch l.member join fetch l.book where l.id = :id", Loan.class)
				.setParameter("id", id)
				.setMaxResults(1)
				.getResultStream()
				.findFirst();
	}

	@Transactional(readOnly = true)
	public List<Loan> findOpenLoans(Long memberId) {
		return session().createQuery(
						"select l from Loan l join fetch l.book where l.member.id = :memberId and l.returnedAt is null",
						Loan.class)
				.setParameter("memberId", memberId)
				.getResultList();
	}

	@Transactional(readOnly = true)
	public Page<Loan> findLoans(Long memberId, Pageable pageable) {
		Session session = session();
		String filter = memberId == null ? "" : " where l.member.id = :memberId";
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
		return session().createQuery(
						"select count(l) from Loan l where l.book.id = :bookId and l.returnedAt is null", Long.class)
				.setParameter("bookId", bookId)
				.getSingleResult();
	}

	@Transactional(readOnly = true)
	public long countOpenLoans() {
		return session().createQuery("select count(l) from Loan l where l.returnedAt is null", Long.class)
				.getSingleResult();
	}

	private <T> T persistIfNew(Session session, T entity) {
		if (identifier(entity) == null) {
			session.persist(entity);
		}
		return entity;
	}

	private <T> Page<T> page(String select, String count, Class<T> type, Pageable pageable) {
		Session session = session();
		List<T> content = session.createQuery(select, type)
				.setFirstResult((int) pageable.getOffset())
				.setMaxResults(pageable.getPageSize())
				.getResultList();
		long total = session.createQuery(count, Long.class).getSingleResult();
		return new PageImpl<>(content, pageable, total);
	}

	private long count(String jpql, String parameter, String value) {
		return session().createQuery(jpql, Long.class).setParameter(parameter, value).getSingleResult();
	}

	private Object identifier(Object entity) {
		return entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
	}

	private Session session() {
		return entityManager.unwrap(Session.class);
	}

}
