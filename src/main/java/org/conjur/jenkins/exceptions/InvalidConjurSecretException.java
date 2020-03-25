package org.conjur.jenkins.exceptions;

public class InvalidConjurSecretException 
  extends RuntimeException {
    
    /**
   *
   */
  private static final long serialVersionUID = 1L;

  public InvalidConjurSecretException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public InvalidConjurSecretException(String errorMessage) {
        super(errorMessage);
    }

}