package com.library.serviceimpl;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.library.dao.BookDao;
import com.library.dao.LoanDao;
import com.library.dao.MemberDao;
import com.library.rules.BorrowingContext;
import com.library.rules.BorrowingRules;
import com.library.rules.RuleResult;
import com.library.dto.LoanRequest;
import com.library.dto.LoanResponse;
import com.library.entity.Book;
import com.library.entity.Loan;
import com.library.entity.Member;
import com.library.exception.BadRequestException;
import com.library.exception.ConflictException;
import com.library.exception.ForbiddenException;
import com.library.exception.NotFoundException;
import com.library.config.BorrowingProperties;
import com.library.service.CurrentUser;
import com.library.service.LoanService;
import com.library.service.MetricsService;

@Service
public class LoanServiceImpl implements LoanService {

	private static final Logger log = LoggerFactory.getLogger(LoanServiceImpl.class);

	private final MemberDao members;
	private final BookDao books;
	private final LoanDao loans;
	private final BorrowingProperties borrowingProperties;
	private final BorrowingRules borrowingRules;
	private final CurrentUser currentUser;
	private final Clock clock;
	private final MetricsService metrics;

	public LoanServiceImpl(MemberDao members, BookDao books, LoanDao loans, BorrowingProperties borrowingProperties,
			BorrowingRules borrowingRules, CurrentUser currentUser, Clock clock, MetricsService metrics) {
		this.members = members;
		this.books = books;
		this.loans = loans;
		this.borrowingProperties = borrowingProperties;
		this.borrowingRules = borrowingRules;
		this.currentUser = currentUser;
		this.clock = clock;
		this.metrics = metrics;
	}

	@Override
	@Transactional
	public LoanResponse borrow(LoanRequest request) {
		long started = System.nanoTime();
		log.info("borrow started memberId={} bookId={}", request.memberId(), request.bookId());
		currentUser.assertCanActFor(request.memberId());
		Member member = members.findById(request.memberId())
				.orElseThrow(() -> {
					log.warn("borrow failed memberId={} bookId={} reason=member not found", request.memberId(), request.bookId());
					return new NotFoundException("Member not found");
				});
		Book book = books.findByIdForUpdate(request.bookId())
				.orElseThrow(() -> {
					log.warn("borrow failed memberId={} bookId={} reason=book not found", request.memberId(), request.bookId());
					return new NotFoundException("Book not found");
				});
		Instant now = clock.instant();
		var rules = borrowingProperties;
		BorrowingContext context = new BorrowingContext(member, book, loans.findOpenLoans(member.getId()), now, rules);
		RuleResult result = borrowingRules.evaluate(context);
		if (!result.allowed()) {
			metrics.recordRejected(result.code());
			log.warn("borrow rejected memberId={} bookId={} code={}", member.getId(), book.getId(), result.code());
			throw new ConflictException(result.code(), result.message());
		}
		Instant dueDate = borrowingRules.dueDate(now, rules);
		book.borrowOne();
		books.save(book);
		Loan loan = loans.save(Loan.open(member, book, now, dueDate));
		long durationMs = (System.nanoTime() - started) / 1_000_000;
		metrics.recordBorrowed(durationMs);
		log.info("borrow allowed memberId={} bookId={} loanId={} dueDate={}", member.getId(), book.getId(), loan.getId(), dueDate);
		return new LoanResponse(loan);
	}

	@Override
	@Transactional
	public LoanResponse returnBook(LoanRequest request) {
		log.info("return started memberId={} bookId={}", request.memberId(), request.bookId());
		currentUser.assertCanActFor(request.memberId());
		members.findById(request.memberId()).orElseThrow(() -> {
			log.warn("return failed memberId={} bookId={} reason=member not found", request.memberId(), request.bookId());
			return new NotFoundException("Member not found");
		});
		books.findById(request.bookId()).orElseThrow(() -> {
			log.warn("return failed memberId={} bookId={} reason=book not found", request.memberId(), request.bookId());
			return new NotFoundException("Book not found");
		});
		Loan open = loans.findOpenLoan(request.memberId(), request.bookId()).orElse(null);
		if (open == null) {
			Loan previous = loans.findLatestLoan(request.memberId(), request.bookId())
					.filter(loan -> !loan.isOpen())
					.orElseThrow(() -> {
						log.warn("return failed memberId={} bookId={} reason=loan not found", request.memberId(),
								request.bookId());
						return new NotFoundException("Loan not found");
					});
			log.info("return already completed memberId={} bookId={} loanId={}", request.memberId(), request.bookId(),
					previous.getId());
			return new LoanResponse(previous);
		}
		Instant now = clock.instant();
		Book book = books.findByIdForUpdate(open.getBookId())
				.orElseThrow(() -> new NotFoundException("Book not found"));
		open.markReturned(now);
		book.returnOne();
		books.save(book);
		loans.save(open);
		metrics.recordReturned();
		log.info("return loanId={} memberId={} bookId={}", open.getId(), open.getMemberId(), book.getId());
		return new LoanResponse(open);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<LoanResponse> borrowingHistory(Long memberId, int page, int size) {
		log.info("borrow history memberId={} page={} size={}", memberId, page, size);
		if (memberId == null || memberId <= 0) {
			log.warn("borrow history rejected reason=invalid member id memberId={}", memberId);
			throw new BadRequestException("invalid member id");
		}
		if (!currentUser.librarian()) {
			Member self = currentUser.requireMemberAccount();
			if (!memberId.equals(self.getId())) {
				log.warn("borrow history rejected memberId={} reason=forbidden", memberId);
				throw new ForbiddenException("Members can only list their own loans");
			}
		}
		if (members.findById(memberId).isEmpty()) {
			log.warn("borrow history rejected reason=invalid member id memberId={}", memberId);
			throw new BadRequestException("invalid member id");
		}
		Pageable pageable = Pages.of(page, size);
		return loans.findLoans(memberId, pageable).map(LoanResponse::new);
	}

}
