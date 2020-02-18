package org.conjur.jenkins.exceptions;

public class InvalidConjurSecretException 
  extends RuntimeException {
    public InvalidConjurSecretException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}