package com.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.jayway.jsonpath.JsonPath;
import com.library.entity.Member;
import com.library.repository.SqlSession;

import io.micrometer.core.instrument.MeterRegistry;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LibraryApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SqlSession sqlSession;

	@Autowired
	private MeterRegistry meterRegistry;

	@Test
	void healthChecksProcessDatabaseAndDisk() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components.ping.status").value("UP"))
				.andExpect(jsonPath("$.components.db.status").value("UP"))
				.andExpect(jsonPath("$.components.diskSpace.status").value("UP"));
	}

	@Test
	void librarianManagesCatalogAndMemberCannot() throws Exception {
		mockMvc.perform(get("/api/v1/books")).andExpect(status().isUnauthorized());

		long bookId = createBook(isbn(), 2);
		mockMvc.perform(get("/api/v1/books/" + bookId).with(member()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.availableCopies").value(2));

		mockMvc.perform(post("/api/v1/books").with(member())
						.contentType(MediaType.APPLICATION_JSON)
						.content(bookJson(isbn(), 1)))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/v1/books").with(librarian())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void validatesBodyPathAndQueryParameters() throws Exception {
		mockMvc.perform(get("/api/v1/books").with(librarian()).param("size", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("size"));

		mockMvc.perform(get("/api/v1/books/not-a-number").with(librarian()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MALFORMED_PARAMETER"));

		mockMvc.perform(post("/api/v1/loans").with(librarian())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"memberId\":-1,\"bookId\":0}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void librarianCanDeleteBooksAndViewBorrowHistory() throws Exception {
		long bookId = createBook(isbn(), 1);
		mockMvc.perform(delete("/api/v1/books/" + bookId).with(librarian()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/borrow-history").with(librarian()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray());
		mockMvc.perform(get("/api/v1/borrow-history").with(member()))
				.andExpect(status().isForbidden());
	}

	@Test
	void librarianCanManageMembersAndBorrowForThem() throws Exception {
		long memberId = createMember(email());
		long bookId = createBook(isbn(), 1);

		mockMvc.perform(get("/api/v1/members").with(librarian()))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/loans").with(librarian())
						.contentType(MediaType.APPLICATION_JSON)
						.content(loanJson(memberId, bookId)))
				.andExpect(status().isCreated());
	}

	@Test
	void memberBorrowsAndReturnsOnlyForSelf() throws Exception {
		Member seeded = sqlSession.findMemberByEmail("member@library.local").orElseThrow();
		long otherMemberId = createMember(email());
		long bookId = createBook(isbn(), 2);

		mockMvc.perform(post("/api/v1/loans").with(member())
						.contentType(MediaType.APPLICATION_JSON)
						.content(loanJson(otherMemberId, bookId)))
				.andExpect(status().isForbidden());

		MvcResult borrowed = mockMvc.perform(post("/api/v1/loans").with(member())
						.contentType(MediaType.APPLICATION_JSON)
						.content(loanJson(seeded.getId(), bookId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.returnedAt").isEmpty())
				.andReturn();
		Instant dueDate = Instant.parse(JsonPath.read(borrowed.getResponse().getContentAsString(), "$.dueDate"));
		assertThat(dueDate).isAfter(Instant.now().plus(13, ChronoUnit.DAYS));
		long loanId = idOf(borrowed);

		mockMvc.perform(get("/api/v1/books/" + bookId).with(member()))
				.andExpect(jsonPath("$.availableCopies").value(1));

		mockMvc.perform(post("/api/v1/loans/" + loanId + "/return").with(member()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.returnedAt").isNotEmpty());
		mockMvc.perform(post("/api/v1/loans/" + loanId + "/return").with(member()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.returnedAt").isNotEmpty());
		mockMvc.perform(get("/api/v1/books/" + bookId).with(member()))
				.andExpect(jsonPath("$.availableCopies").value(2));

		assertThat(meterRegistry.get("library.loans.borrowed").counter().count()).isPositive();
		assertThat(meterRegistry.get("library.loans.returned").counter().count()).isPositive();
		assertThat(meterRegistry.find("http.server.requests").tag("uri", "/api/v1/loans").timers()).isNotEmpty();
	}

	@Test
	void enforcesConfiguredLoanLimitCopiesAndDuplicate() throws Exception {
		long memberId = createMember(email());
		long holderId = createMember(email());
		long firstBook = createBook(isbn(), 2);
		long secondBook = createBook(isbn(), 1);
		long oneCopy = createBook(isbn(), 1);
		borrow(holderId, oneCopy);

		borrow(memberId, firstBook);
		mockMvc.perform(post("/api/v1/loans").with(librarian())
						.contentType(MediaType.APPLICATION_JSON)
						.content(loanJson(memberId, firstBook)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ALREADY_BORROWED"));

		for (int i = 0; i < 4; i++) {
			borrow(memberId, createBook(isbn(), 1));
		}
		mockMvc.perform(post("/api/v1/loans").with(librarian())
						.contentType(MediaType.APPLICATION_JSON)
						.content(loanJson(memberId, secondBook)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("LOAN_LIMIT_REACHED"));

		long thirdMember = createMember(email());
		mockMvc.perform(post("/api/v1/loans").with(librarian())
						.contentType(MediaType.APPLICATION_JSON)
						.content(loanJson(thirdMember, oneCopy)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("NO_COPIES_AVAILABLE"));

		assertThat(meterRegistry.get("library.loans.rejected").tag("code", "LOAN_LIMIT_REACHED").counter().count()).isPositive();
	}

	@Test
	void overdueLoanBlocksTheNextBorrow() throws Exception {
		long memberId = createMember(email());
		long firstBook = createBook(isbn(), 1);
		long secondBook = createBook(isbn(), 1);
		long loanId = borrow(memberId, firstBook);

		var loan = sqlSession.findLoan(loanId).orElseThrow();
		loan.setDueDate(Instant.now().minus(1, ChronoUnit.DAYS));
		sqlSession.save(loan);

		mockMvc.perform(post("/api/v1/loans").with(librarian())
						.contentType(MediaType.APPLICATION_JSON)
						.content(loanJson(memberId, secondBook)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("MEMBER_HAS_OVERDUE"));
	}

	@Test
	void openLoanBlocksDelete() throws Exception {
		long memberId = createMember(email());
		long bookId = createBook(isbn(), 1);
		borrow(memberId, bookId);
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/books/" + bookId).with(librarian()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("BOOK_HAS_OPEN_LOANS"));
	}

	@Test
	void memberCanReadRulesButNotAnotherMember() throws Exception {
		long otherMemberId = createMember(email());
		mockMvc.perform(get("/api/v1/members/" + otherMemberId).with(member()))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/borrowing-rules").with(member()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.maxActiveLoans").value(5))
				.andExpect(jsonPath("$.blockWhenOverdue").value(true))
				.andExpect(jsonPath("$.loanDurationDays").value(14));
	}

	private long createBook(String isbn, int copies) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/books").with(librarian())
						.contentType(MediaType.APPLICATION_JSON)
						.content(bookJson(isbn, copies)))
				.andExpect(status().isCreated())
				.andReturn();
		return idOf(result);
	}

	private long createMember(String email) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/members").with(librarian())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Test Member","email":"%s"}
								""".formatted(email)))
				.andExpect(status().isCreated())
				.andReturn();
		return idOf(result);
	}

	private long borrow(long memberId, long bookId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/loans").with(librarian())
						.contentType(MediaType.APPLICATION_JSON)
						.content(loanJson(memberId, bookId)))
				.andExpect(status().isCreated())
				.andReturn();
		return idOf(result);
	}

	private static long idOf(MvcResult result) throws Exception {
		return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
	}

	private static String bookJson(String isbn, int copies) {
		return """
				{"title":"Book %s","author":"Author","isbn":"%s","totalCopies":%d}
				""".formatted(isbn, isbn, copies);
	}

	private static String loanJson(long memberId, long bookId) {
		return """
				{"memberId":%d,"bookId":%d}
				""".formatted(memberId, bookId);
	}

	private static String isbn() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 13);
	}

	private static String email() {
		return "member-" + UUID.randomUUID() + "@library.local";
	}

	private static RequestPostProcessor librarian() {
		return httpBasic("librarian", "password");
	}

	private static RequestPostProcessor member() {
		return httpBasic("member", "password");
	}

}
