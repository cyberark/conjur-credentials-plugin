package org.conjur.jenkins.conjursecrets;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;

import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;

import org.conjur.jenkins.api.ConjurAPI;
import org.conjur.jenkins.api.ConjurAPIUtils;
import org.conjur.jenkins.configuration.ConjurConfiguration;
import org.conjur.jenkins.exceptions.InvalidConjurSecretException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.model.ModelObject;
import hudson.util.Secret;
import okhttp3.OkHttpClient;

public class ConjurSecretCredentialsImpl extends BaseStandardCredentials implements ConjurSecretCredentials {

	@Extension
	public static class DescriptorImpl extends CredentialsDescriptor {

		@Override
		public String getDisplayName() {
			return "Conjur Secret Credential";
		}

	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = Logger.getLogger(ConjurSecretCredentialsImpl.class.getName());
	private String variablePath; // to be used as Username

	private ConjurConfiguration conjurConfiguration;

	private transient ModelObject context;
	private transient ModelObject storeContext;

	static Logger getLogger() {
		return Logger.getLogger(ConjurSecretCredentialsImpl.class.getName());
	}


	@DataBoundConstructor
	public ConjurSecretCredentialsImpl(@CheckForNull CredentialsScope scope, @CheckForNull String id,
			@CheckForNull String variablePath, @CheckForNull String description) {
		super(scope, id, description);
		this.variablePath = variablePath;
	}


	@Override
	public String getDisplayName() {
		return "ConjurSecret:" + this.variablePath;
	}

	static Secret secretFromString(String secretString) {
		return Secret.fromString(secretString);
	}

	public Secret getSecret() {

		String result = "";
		try {
			// Get Http Client
			OkHttpClient client = ConjurAPIUtils.getHttpClient(this.conjurConfiguration);
			// Authenticate to Conjur
			String authToken = ConjurAPI.getAuthorizationToken(client, this.conjurConfiguration, context);
			// Retrieve secret from Conjur
			String secretString = ConjurAPI.getSecret(client, this.conjurConfiguration, authToken, this.variablePath);
			result = secretString;
		} catch (IOException e) {
			LOGGER.log(Level.FINE, "EXCEPTION: " + e.getMessage());
			throw new InvalidConjurSecretException(e.getMessage(), e);
		}

		return secretFromString(result);
	}

	public String getVariablePath() {
		return this.variablePath;
	}

	public void setConjurConfiguration(ConjurConfiguration conjurConfiguration) {
		if (conjurConfiguration != null)
			this.conjurConfiguration = conjurConfiguration;
	}

	public void setContext(ModelObject context) {
		LOGGER.log(Level.FINEST, "Setting context");
		this.context = context;
		setConjurConfiguration(ConjurAPI.getConfigurationFromContext(context, storeContext));
	}

	public void setStoreContext(ModelObject storeContext) {
		LOGGER.log(Level.FINEST, "Setting store context");
		this.context = storeContext;
		setConjurConfiguration(ConjurAPI.getConfigurationFromContext(context, storeContext));
	}

	@DataBoundSetter
	public void setVariablePath(String variablePath) {
		this.variablePath = variablePath;
	}

	@Override
	public String getNameTag() {
		return "";
	}

}
