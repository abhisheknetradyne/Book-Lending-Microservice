package com.library.exception;

public class BorrowingRejectedException extends RuntimeException {

	private final String code;

	public BorrowingRejectedException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String getCode() {
		return code;
	}

}
