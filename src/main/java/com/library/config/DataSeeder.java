package com.library.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.library.entity.Book;
import com.library.entity.Member;
import com.library.repository.SqlSession;

@Component
public class DataSeeder implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

	private final SqlSession sqlSession;

	public DataSeeder(SqlSession sqlSession) {
		this.sqlSession = sqlSession;
	}

	@Override
	@Transactional
	public void run(String... args) {
		if (sqlSession.findMemberByEmail("member@library.local").isEmpty()) {
			sqlSession.save(Member.register("Ada Lovelace", "member@library.local"));
			log.info("Seeded sample member member@library.local");
		}
		if (!sqlSession.isbnExists("9780132350884")) {
			sqlSession.save(Book.register("Clean Code", "Robert C. Martin", "9780132350884", 3));
		}
		if (!sqlSession.isbnExists("9780134685991")) {
			sqlSession.save(Book.register("Effective Java", "Joshua Bloch", "9780134685991", 2));
		}
	}

}
