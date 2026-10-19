package com.library.dao;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.library.session.SqlSession;

@Repository
public class HealthDao {

	private final SqlSession sqlSession;

	public HealthDao(SqlSession sqlSession) {
		this.sqlSession = sqlSession;
	}

	@Transactional(readOnly = true)
	public int selectOne() {
		return sqlSession.session().createNativeQuery("SELECT 1", Integer.class).getSingleResult();
	}

}
