package org.conjur.jenkins.configuration;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.model.AbstractItem;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;

/**
 * Example of Jenkins global configuration.
 */
@Extension
public class GlobalConjurConfiguration extends GlobalConfiguration implements Serializable {

	private static final long serialVersionUID = 1L;

	private ConjurConfiguration conjurConfiguration;
	private Boolean enableJWKS = false;
	private String authWebServiceId = "";
	private String jwtAudience = "";
	private long keyLifetimeInMinutes = 60;
	private long tokenDurarionInSeconds = 120;
	private Boolean enableContextAwareCredentialStore = false;
	private String identityFormatFieldsFromToken = "jenkins_name";
	private String identityFieldsSeparator = "-";
	private String identityFieldName = "identity";

	private static final Logger LOGGER = Logger.getLogger(GlobalConjurConfiguration.class.getName());

	/**
	 * check the Token Duration for validity
	 * 
	 * @param Jenkins AbstractItem anc
	 * @param Token   duration in sectonds
	 * @param Token   keyLifetimeInMinutes
	 * @return
	 */
	public FormValidation doCheckTokenDurarionInSeconds(@AncestorInPath AbstractItem anc,
			@QueryParameter("tokenDurarionInSeconds") String tokenDurarionInSeconds,
			@QueryParameter("keyLifetimeInMinutes") String keyLifetimeInMinutes) {
		LOGGER.log(Level.FINE, "Inside of doCheckTokenDurarionInSeconds()");
		try {
			int tokenttl = Integer.parseInt(tokenDurarionInSeconds);
			int keyttl = Integer.parseInt(keyLifetimeInMinutes);
			if (tokenttl > keyttl * 60) {
				LOGGER.log(Level.FINE, "Token cannot last longer than key");
				return FormValidation.error("Token cannot last longer than key");
			} else {
				return FormValidation.ok();
			}
		} catch (NumberFormatException e) {
			LOGGER.log(Level.WARNING, "Key lifetime and token duration must be numbers");
			return FormValidation.error("Key lifetime and token duration must be numbers");
		}

	}

	/** @return the singleton instance */
	@Nonnull
	public static GlobalConjurConfiguration get() {

		GlobalConjurConfiguration result = null;

		result = GlobalConfiguration.all().get(GlobalConjurConfiguration.class);

		if (result == null) {
			throw new IllegalStateException();
		}
		return result;
	}

	/**
	 * When Jenkins is restarted, load any saved configuration from disk.
	 */

	public GlobalConjurConfiguration() {
		// When Jenkins is restarted, load any saved configuration from disk.
		load();
	}

	/** @return ConjurConfiguration object */
	public ConjurConfiguration getConjurConfiguration() {
		return conjurConfiguration;
	}

	/** @return boolean if JWKS is enabled */
	public Boolean getEnableJWKS() {
		return enableJWKS;
	}

	/** @return boolean for enableContextAware CredentialStore */

	public Boolean getEnableContextAwareCredentialStore() {
		return enableContextAwareCredentialStore;
	}

	/** @return Web Service ID for authentication */
	public String getAuthWebServiceId() {
		return authWebServiceId;
	}

	/** set the Authentication WebService Id */
	@DataBoundSetter
	public void setAuthWebServiceId(String authWebServiceId) {
		this.authWebServiceId = authWebServiceId;
		save();
	}

	/** @return the Identity FieldName */
	public String getidentityFieldName() {
		return identityFieldName;
	}

	/** set the IdentityFieldName */
	@DataBoundSetter
	public void setIdentityFieldName(String identityFieldName) {
		this.identityFieldName = identityFieldName;
		save();
	}

	/** @retrun IdentityFormatFieldsFromToken */
	public String getIdentityFormatFieldsFromToken() {
		return identityFormatFieldsFromToken;
	}

	/** set the IdentityFormatFieldsFromToken */
	@DataBoundSetter
	public void setIdentityFormatFieldsFromToken(String identityFormatFieldsFromToken) {
		this.identityFormatFieldsFromToken = identityFormatFieldsFromToken;
		save();
	}

	/** @return IdentityFieldsSeparator */
	public String getIdentityFieldsSeparator() {
		return identityFieldsSeparator;
	}

	/** set the IdentityFieldsSeparator */
	@DataBoundSetter
	public void setIdentityFieldsSeparator(String identityFieldsSeparator) {
		this.identityFieldsSeparator = identityFieldsSeparator;
		save();
	}

	/** @return the JWT Audience */
	public String getJwtAudience() {
		return jwtAudience;
	}

	/** set the JWT Audience */

	@DataBoundSetter
	public void setJwtAudience(String jwtAudience) {
		this.jwtAudience = jwtAudience;
		save();
	}

	/** @return the Key Life Time in Minutes */
	public long getKeyLifetimeInMinutes() {
		return keyLifetimeInMinutes;
	}

	/** set the Key Life Time in Minutes */
	@DataBoundSetter
	public void setKeyLifetimeInMinutes(long keyLifetimeInMinutes) {
		this.keyLifetimeInMinutes = keyLifetimeInMinutes;
		save();
	}

	/** @return the Token duration in seconds */
	public long getTokenDurarionInSeconds() {
		return tokenDurarionInSeconds;
	}

	/** set the Token duration in seconds */
	@DataBoundSetter
	public void setTokenDurarionInSeconds(long tokenDurarionInSeconds) {
		this.tokenDurarionInSeconds = tokenDurarionInSeconds;
		save();
	}

	/** set the Conjur Configuration parameters */
	@DataBoundSetter
	public void setConjurConfiguration(ConjurConfiguration conjurConfiguration) {
		this.conjurConfiguration = conjurConfiguration;
		save();
	}

	/** set Enable JWKS option */
	@DataBoundSetter
	public void setEnableJWKS(Boolean enableJWKS) {
		this.enableJWKS = enableJWKS;
		save();
	}

	/** set the EnablContextAwareCredentialStore selected value */
	@DataBoundSetter
	public void setEnableContextAwareCredentialStore(Boolean enableContextAwareCredentialStore) {
		this.enableContextAwareCredentialStore = enableContextAwareCredentialStore;
		save();
	}

}
