package com.library.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "loans", indexes = {
		@Index(name = "idx_loans_member", columnList = "member_id")
})
public class Loan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "book_id", nullable = false)
	private Book book;

	@Column(nullable = false)
	private Instant borrowedAt;

	@Column(nullable = false)
	private Instant dueDate;

	private Instant returnedAt;

	protected Loan() {
	}

	public Loan(Long id, Member member, Book book, Instant borrowedAt, Instant dueDate, Instant returnedAt) {
		this.id = id;
		this.member = member;
		this.book = book;
		this.borrowedAt = borrowedAt;
		this.dueDate = dueDate;
		this.returnedAt = returnedAt;
	}

	public static Loan open(Member member, Book book, Instant borrowedAt, Instant dueDate) {
		return new Loan(null, member, book, borrowedAt, dueDate, null);
	}

	public void markReturned(Instant returnedAt) {
		this.returnedAt = returnedAt;
	}

	public boolean isOpen() {
		return returnedAt == null;
	}

	public Long getId() {
		return id;
	}

	public Long getMemberId() {
		return member.getId();
	}

	public Long getBookId() {
		return book.getId();
	}

	public Member getMember() {
		return member;
	}

	public Book getBook() {
		return book;
	}

	public Instant getBorrowedAt() {
		return borrowedAt;
	}

	public Instant getDueDate() {
		return dueDate;
	}

	public Instant getReturnedAt() {
		return returnedAt;
	}

	public void setDueDate(Instant dueDate) {
		this.dueDate = dueDate;
	}

}
