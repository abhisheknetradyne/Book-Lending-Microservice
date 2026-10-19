package com.library.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.library.dao.BookDao;
import com.library.dao.MemberDao;
import com.library.entity.Book;
import com.library.entity.Member;

@Component
public class DataSeeder implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
	private static final String SEED_MEMBER_EMAIL = "member@library.local";

	private final MemberDao members;
	private final BookDao books;
	private final PasswordEncoder passwordEncoder;
	private final String seedMemberPassword;

	public DataSeeder(MemberDao members, BookDao books, PasswordEncoder passwordEncoder,
			@Value("${library.seed.member-password}") String seedMemberPassword) {
		this.members = members;
		this.books = books;
		this.passwordEncoder = passwordEncoder;
		this.seedMemberPassword = seedMemberPassword;
	}

	@Override
	@Transactional
	public void run(String... args) {
		members.findMemberByEmail(SEED_MEMBER_EMAIL).ifPresentOrElse(this::ensureSeedMemberPassword, this::seedSampleMember);
		if (!books.isbnExists("9780132350884")) {
			books.save(Book.register("Clean Code", "Robert C. Martin", "9780132350884", 3));
		}
		if (!books.isbnExists("9780134685991")) {
			books.save(Book.register("Effective Java", "Joshua Bloch", "9780134685991", 2));
		}
	}

	private void seedSampleMember() {
		members.save(Member.register("Ada Lovelace", SEED_MEMBER_EMAIL, passwordEncoder.encode(seedMemberPassword)));
		log.info("Seeded sample member {}", SEED_MEMBER_EMAIL);
	}

	private void ensureSeedMemberPassword(Member member) {
		if (!member.missingLoginPassword()) {
			return;
		}
		member.setPasswordHash(passwordEncoder.encode(seedMemberPassword));
		members.save(member);
		log.info("Set login password for seeded member {}", SEED_MEMBER_EMAIL);
	}

}
