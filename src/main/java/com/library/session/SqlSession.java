package com.library.session;

import java.util.Optional;

import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

@Component
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

	public Session session() {
		return entityManager.unwrap(Session.class);
	}

	private <T> T persistIfNew(Session session, T entity) {
		if (identifier(entity) == null) {
			session.persist(entity);
		}
		return entity;
	}

	private Object identifier(Object entity) {
		return entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
	}

}
