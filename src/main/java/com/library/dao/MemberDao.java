package com.library.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.library.entity.Member;
import com.library.session.SqlSession;

@Repository
public class MemberDao {

	private final SqlSession sqlSession;

	public MemberDao(SqlSession sqlSession) {
		this.sqlSession = sqlSession;
	}

	@Transactional(readOnly = true)
	public Optional<Member> findById(Long id) {
		return sqlSession.find(Member.class, id);
	}

	@Transactional
	public Member save(Member member) {
		return sqlSession.save(member);
	}

	@Transactional(readOnly = true)
	public Page<Member> findAll(Pageable pageable) {
		var session = sqlSession.session();
		List<Member> content = session.createQuery("select m from Member m order by m.id desc", Member.class)
				.setFirstResult((int) pageable.getOffset())
				.setMaxResults(pageable.getPageSize())
				.getResultList();
		long total = session.createQuery("select count(m) from Member m", Long.class).getSingleResult();
		return new PageImpl<>(content, pageable, total);
	}

	@Transactional(readOnly = true)
	public boolean memberEmailExists(String email) {
		Long count = sqlSession.session().createQuery(
						"select count(m) from Member m where lower(m.email) = lower(:email)", Long.class)
				.setParameter("email", email)
				.getSingleResult();
		return count > 0;
	}

	@Transactional(readOnly = true)
	public Optional<Member> findMemberByEmail(String email) {
		return sqlSession.session().createQuery("select m from Member m where lower(m.email) = lower(:email)", Member.class)
				.setParameter("email", email)
				.setMaxResults(1)
				.getResultStream()
				.findFirst();
	}

}
