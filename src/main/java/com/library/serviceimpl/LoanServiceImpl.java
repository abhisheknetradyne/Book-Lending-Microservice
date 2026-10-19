package com.library.serviceimpl;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.library.config.BorrowingProperties;
import com.library.domain.rules.BorrowingContext;
import com.library.domain.rules.BorrowingPolicy;
import com.library.domain.rules.BorrowingRuleRegistry;
import com.library.domain.rules.LoanDurationPolicy;
import com.library.domain.rules.RuleResult;
import com.library.dto.LoanRequest;
import com.library.dto.LoanResponse;
import com.library.entity.Book;
import com.library.entity.Loan;
import com.library.entity.Member;
import com.library.exception.BorrowingRejectedException;
import com.library.exception.ForbiddenException;
import com.library.exception.NotFoundException;
import com.library.repository.SqlSession;
import com.library.service.CurrentUser;
import com.library.service.LoanService;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class LoanServiceImpl implements LoanService {

	private static final Logger log = LoggerFactory.getLogger(LoanServiceImpl.class);

	private final SqlSession sqlSession;
	private final BorrowingProperties borrowingProperties;
	private final BorrowingRuleRegistry registry;
	private final LoanDurationPolicy durationPolicy;
	private final CurrentUser currentUser;
	private final Clock clock;
	private final MeterRegistry meterRegistry;

	public LoanServiceImpl(SqlSession sqlSession, BorrowingProperties borrowingProperties, BorrowingRuleRegistry registry,
			LoanDurationPolicy durationPolicy, CurrentUser currentUser, Clock clock, MeterRegistry meterRegistry) {
		this.sqlSession = sqlSession;
		this.borrowingProperties = borrowingProperties;
		this.registry = registry;
		this.durationPolicy = durationPolicy;
		this.currentUser = currentUser;
		this.clock = clock;
		this.meterRegistry = meterRegistry;
	}

	@Override
	@Transactional
	public LoanResponse borrow(LoanRequest request) {
		Timer.Sample sample = Timer.start(meterRegistry);
		try {
			currentUser.assertCanActFor(request.memberId());
			Member member = sqlSession.find(Member.class, request.memberId())
					.orElseThrow(() -> new NotFoundException("Member not found"));
			Book book = sqlSession.findForUpdate(Book.class, request.bookId())
					.orElseThrow(() -> new NotFoundException("Book not found"));
			BorrowingPolicy currentRules = borrowingPolicy();
			Instant now = clock.instant();
			BorrowingContext context = new BorrowingContext(member, book, sqlSession.findOpenLoans(member.getId()), now, currentRules);
			RuleResult result = registry.evaluate(context);
			if (!result.allowed()) {
				meterRegistry.counter("library.loans.rejected", "code", result.code()).increment();
				log.info("borrow rejected memberId={} bookId={} code={}", member.getId(), book.getId(), result.code());
				throw new BorrowingRejectedException(result.code(), result.message());
			}
			Instant dueDate = durationPolicy.dueDate(now, currentRules);
			book.borrowOne();
			sqlSession.save(book);
			Loan loan = sqlSession.save(Loan.open(member, book, now, dueDate));
			meterRegistry.counter("library.loans.borrowed").increment();
			log.info("borrow allowed memberId={} bookId={} loanId={} dueDate={}", member.getId(), book.getId(), loan.getId(), dueDate);
			return new LoanResponse(loan);
		}
		finally {
			sample.stop(meterRegistry.timer("library.loans.borrow"));
		}
	}

	@Override
	@Transactional
	public LoanResponse returnBook(Long loanId) {
		Loan loan = sqlSession.findLoan(loanId).orElseThrow(() -> new NotFoundException("Loan not found"));
		currentUser.assertCanActFor(loan.getMemberId());
		Instant now = clock.instant();
		if (!loan.isOpen()) {
			return new LoanResponse(loan);
		}
		Book book = sqlSession.findForUpdate(Book.class, loan.getBookId())
				.orElseThrow(() -> new NotFoundException("Book not found"));
		loan.markReturned(now);
		book.returnOne();
		sqlSession.save(book);
		sqlSession.save(loan);
		meterRegistry.counter("library.loans.returned").increment();
		log.info("return loanId={} memberId={} bookId={}", loan.getId(), loan.getMemberId(), book.getId());
		return new LoanResponse(loan);
	}

	@Override
	@Transactional(readOnly = true)
	public LoanResponse get(Long loanId) {
		Loan loan = sqlSession.findLoan(loanId).orElseThrow(() -> new NotFoundException("Loan not found"));
		currentUser.assertCanActFor(loan.getMemberId());
		return new LoanResponse(loan);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<LoanResponse> list(Long memberId, int page, int size) {
		Long scopedMemberId = memberId;
		if (!currentUser.librarian()) {
			Member self = currentUser.requireMemberAccount();
			if (memberId != null && !memberId.equals(self.getId())) {
				throw new ForbiddenException("Members can only list their own loans");
			}
			scopedMemberId = self.getId();
		}
		Pageable pageable = Pages.of(page, size);
		return sqlSession.findLoans(scopedMemberId, pageable).map(LoanResponse::new);
	}

	private BorrowingPolicy borrowingPolicy() {
		return new BorrowingPolicy(
				borrowingProperties.maxActiveLoans(),
				borrowingProperties.blockWhenOverdue(),
				borrowingProperties.loanDurationDays());
	}

}
