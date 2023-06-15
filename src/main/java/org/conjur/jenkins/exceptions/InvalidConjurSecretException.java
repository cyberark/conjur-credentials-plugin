package org.conjur.jenkins.exceptions;

/**
 * Custom Exception if no secert is found or malformed authentication
 * 
 * @author Jaleela.FaizurRahman
 *
 */
public class InvalidConjurSecretException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Throw error message if secret is not found
	 * 
	 * @param errorMessage
	 * @param err
	 */
	public InvalidConjurSecretException(String errorMessage, Throwable err) {
		super(errorMessage, err);
	}

	/**
	 * throws error message if secret is not found
	 * 
	 * @param errorMessage
	 */

	public InvalidConjurSecretException(String errorMessage) {
		super(errorMessage);
	}

}