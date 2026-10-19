package com.library.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "books")
public class Book {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, length = 200)
	private String author;

	@Column(nullable = false, unique = true, length = 20)
	private String isbn;

	@Column(nullable = false)
	private int totalCopies;

	@Column(nullable = false)
	private int availableCopies;

	@Version
	private Long version;

	protected Book() {
	}

	public Book(Long id, String title, String author, String isbn, int totalCopies, int availableCopies) {
		this.id = id;
		this.title = title;
		this.author = author;
		this.isbn = isbn;
		this.totalCopies = totalCopies;
		this.availableCopies = availableCopies;
	}

	public static Book register(String title, String author, String isbn, int copies) {
		return new Book(null, title.trim(), author.trim(), isbn.trim(), copies, copies);
	}

	public void setTitle(String title) {
		this.title = title.trim();
	}

	public void setAuthor(String author) {
		this.author = author.trim();
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn.trim();
	}

	public void resize(int newTotalCopies) {
		int onLoan = totalCopies - availableCopies;
		if (newTotalCopies < onLoan) {
			throw new IllegalArgumentException("Total copies cannot be lower than the number of copies on loan");
		}
		availableCopies = availableCopies + (newTotalCopies - totalCopies);
		totalCopies = newTotalCopies;
	}

	public void borrowOne() {
		if (availableCopies <= 0) {
			throw new IllegalStateException("No copies available");
		}
		availableCopies--;
	}

	public void returnOne() {
		if (availableCopies >= totalCopies) {
			throw new IllegalStateException("All copies are already on the shelf");
		}
		availableCopies++;
	}

	public Long getId() {
		return id;
	}

	public String getIsbn() {
		return isbn;
	}

	public String getTitle() {
		return title;
	}

	public String getAuthor() {
		return author;
	}

	public int getTotalCopies() {
		return totalCopies;
	}

	public int getAvailableCopies() {
		return availableCopies;
	}

}
