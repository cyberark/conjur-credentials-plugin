package org.conjur.jenkins.configuration;

import java.io.Serializable;
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

	/**
	 * 
	 */
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

	static Logger getLogger() {
		return Logger.getLogger(GlobalConjurConfiguration.class.getName());
	}



	public FormValidation doCheckTokenDurarionInSeconds(@AncestorInPath AbstractItem anc,
														@QueryParameter("tokenDurarionInSeconds") String tokenDurarionInSeconds,
														@QueryParameter("keyLifetimeInMinutes") String keyLifetimeInMinutes) {
		try {
			int tokenttl = Integer.parseInt(tokenDurarionInSeconds);
			int keyttl = Integer.parseInt(keyLifetimeInMinutes);
			if (tokenttl > keyttl*60) {
				return FormValidation.error("Token cannot last longer than key");
			} else {
				return FormValidation.ok();
			}
		} catch (NumberFormatException e) {
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

	public GlobalConjurConfiguration() {
		// When Jenkins is restarted, load any saved configuration from disk.
		load();
	}

	public ConjurConfiguration getConjurConfiguration() {
		return conjurConfiguration;
	}

	public Boolean getEnableJWKS() {
		return enableJWKS;
	}

	public Boolean getEnableContextAwareCredentialStore() {
		return enableContextAwareCredentialStore;
	}

	public String getAuthWebServiceId() {
		return authWebServiceId;
	}

	@DataBoundSetter
	public void setAuthWebServiceId(String authWebServiceId) {
		this.authWebServiceId = authWebServiceId;
		save();
	}

	public String getidentityFieldName() {
		return identityFieldName;
	}

	@DataBoundSetter
	public void setIdentityFieldName(String identityFieldName) {
		this.identityFieldName = identityFieldName;
		save();
	}

	public String getIdentityFormatFieldsFromToken() {
		return identityFormatFieldsFromToken;
	}

	@DataBoundSetter
	public void setIdentityFormatFieldsFromToken(String identityFormatFieldsFromToken) {
		this.identityFormatFieldsFromToken = identityFormatFieldsFromToken;
		save();
	}

	public String getIdentityFieldsSeparator() {
		return identityFieldsSeparator;
	}

	@DataBoundSetter
	public void setIdentityFieldsSeparator(String identityFieldsSeparator) {
		this.identityFieldsSeparator = identityFieldsSeparator;
		save();
	}

	public String getJwtAudience() {
		return jwtAudience;
	}

	@DataBoundSetter
	public void setJwtAudience(String jwtAudience) {
		this.jwtAudience = jwtAudience;
		save();
	}

	public long getKeyLifetimeInMinutes() {
		return keyLifetimeInMinutes;
	}

	@DataBoundSetter
	public void setKeyLifetimeInMinutes(long keyLifetimeInMinutes) {
		this.keyLifetimeInMinutes = keyLifetimeInMinutes;
		save();
	}

	public long getTokenDurarionInSeconds() {
		return tokenDurarionInSeconds;
	}

	@DataBoundSetter
	public void setTokenDurarionInSeconds(long tokenDurarionInSeconds) {
		this.tokenDurarionInSeconds = tokenDurarionInSeconds;
		save();
	}

	@DataBoundSetter
	public void setConjurConfiguration(ConjurConfiguration conjurConfiguration) {
		this.conjurConfiguration = conjurConfiguration;
		save();
	}

	@DataBoundSetter
	public void setEnableJWKS(Boolean enableJWKS) {
		this.enableJWKS = enableJWKS;
		save();
	}

	@DataBoundSetter
	public void setEnableContextAwareCredentialStore(Boolean enableContextAwareCredentialStore) {
		this.enableContextAwareCredentialStore = enableContextAwareCredentialStore;
		save();
	}

}
