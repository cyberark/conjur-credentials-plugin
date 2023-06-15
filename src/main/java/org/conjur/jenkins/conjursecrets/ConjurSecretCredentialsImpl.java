package org.conjur.jenkins.conjursecrets;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;

import org.conjur.jenkins.api.ConjurAPI;
import org.conjur.jenkins.api.ConjurAPIUtils;
import org.conjur.jenkins.configuration.ConjurConfiguration;
import org.conjur.jenkins.exceptions.InvalidConjurSecretException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;

import hudson.Extension;
import hudson.model.ModelObject;
import hudson.util.Secret;
import okhttp3.OkHttpClient;

/** Class to retrieve the secrets */
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

	/**
	 * to set the varaiblePath,scope,id,description
	 * 
	 * @param scope
	 * @param id
	 * @param variablePath
	 * @param description
	 */
	@DataBoundConstructor
	public ConjurSecretCredentialsImpl(@CheckForNull CredentialsScope scope, @CheckForNull String id,
			@CheckForNull String variablePath, @CheckForNull String description) {
		super(scope, id, description);
		this.variablePath = variablePath;
	}

	/**
	 * @return the DisplayName
	 */

	@Override
	public String getDisplayName() {
		return "ConjurSecret:" + this.variablePath;
	}

	/**
	 * @retrun the Secret based on the credentialId
	 * @param secretString
	 * @return
	 */
	static Secret secretFromString(String secretString) {
		return Secret.fromString(secretString);
	}

	/**
	 * @return the Secret calling the {@link ConjurAPI } class , Gets the
	 *         OkHttpclient by calling getHttpclient of {@link ConjurAPIUtils} Get
	 *         the AuthToken by calling getAuthorizationToken of {@link ConjurAPI }
	 *         Get the secret by calling teh getSecret of {@link ConjurAPI }
	 */
	public Secret getSecret() {
		LOGGER.log(Level.FINEST, "Start of getSecret()");
		String result = "";
		try {
			// Get Http Client
			OkHttpClient client = ConjurAPIUtils.getHttpClient(this.conjurConfiguration);
			// Authenticate to Conjur
			String authToken = ConjurAPI.getAuthorizationToken(client, this.conjurConfiguration, storeContext);
			// Retrieve secret from Conjur
			String secretString = ConjurAPI.getSecret(client, this.conjurConfiguration, authToken, this.variablePath);
			result = secretString;
		} catch (IOException e) {
			LOGGER.log(Level.FINE, "EXCEPTION:{0} ", e.getMessage());

		}
		LOGGER.log(Level.FINEST, "End of getSecret()");
		return secretFromString(result);
	}

	/**
	 * 
	 * @return variablePath as String
	 */
	public String getVariablePath() {
		return this.variablePath;
	}

	/**
	 * set the Conjurconfiguration parameters
	 */
	public void setConjurConfiguration(ConjurConfiguration conjurConfiguration) {
		if (conjurConfiguration != null)
			this.conjurConfiguration = conjurConfiguration;
	}

	/**
	 * set the ModelObject context
	 */
	public void setContext(ModelObject context) {
		LOGGER.log(Level.FINEST, "Setting context");
		this.context = context;
		setConjurConfiguration(ConjurAPI.getConfigurationFromContext(context, storeContext));
	}

	/**
	 * set the store Context ModelObject
	 */

	public void setStoreContext(ModelObject storeContext) {
		LOGGER.log(Level.FINEST, "Setting store context");
		this.storeContext = storeContext;
		setConjurConfiguration(ConjurAPI.getConfigurationFromContext(context, storeContext));
	}

	/**
	 * set the variablePath as String
	 * 
	 * @param variablePath
	 */
	@DataBoundSetter
	public void setVariablePath(String variablePath) {
		this.variablePath = variablePath;
	}

	/**
	 * @return the Name Tag
	 */
	@Override
	public String getNameTag() {
		return "";
	}

}
