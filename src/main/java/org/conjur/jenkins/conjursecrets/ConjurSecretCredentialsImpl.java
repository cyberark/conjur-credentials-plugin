package org.conjur.jenkins.conjursecrets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import javax.annotation.CheckForNull;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.conjur.jenkins.api.ConjurAPI;
import org.conjur.jenkins.configuration.ConjurConfiguration;
import org.conjur.jenkins.configuration.FolderConjurConfiguration;
import org.conjur.jenkins.configuration.GlobalConjurConfiguration;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Run;
import hudson.util.Secret;
import okhttp3.OkHttpClient;

public class ConjurSecretCredentialsImpl extends BaseStandardCredentials implements ConjurSecretCredentials {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String variablePath; // to be used as Username
	
	private transient ConjurConfiguration conjurConfiguration;
	private transient Run<?, ?> context;

	private static final Logger LOGGER = Logger.getLogger( ConjurSecretCredentialsImpl.class.getName());
	
	@DataBoundConstructor
	public ConjurSecretCredentialsImpl(@CheckForNull CredentialsScope scope, 
			                           @CheckForNull String id,
			                           @CheckForNull String variablePath,
			                           @CheckForNull String description) {
		super(scope, id, description);
		this.variablePath = variablePath;
	}

	public String getVariablePath() {
		return this.variablePath;
	}
	
    @DataBoundSetter
	public void setVariablePath(String variablePath) {
		this.variablePath = variablePath;
	}

	public Secret getSecret() {
		String result = "";
		try {
			// Get Http Client 
			OkHttpClient client = ConjurAPI.getHttpClient(this.conjurConfiguration);
			// Authenticate to Conjur
			String authToken = ConjurAPI.getAuthorizationToken(client, this.conjurConfiguration, context);
			// Retrieve secret from Conjur
			String secretString = ConjurAPI.getSecret(client, this.conjurConfiguration, authToken, this.variablePath);
			result = secretString;
		} catch (IOException e) {
			Writer writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			String s = writer.toString();
			LOGGER.log(Level.WARNING, "EXCEPTION: " + s);
			result = "EXCEPTION: " + e.getMessage();
		}
		return Secret.fromString(result);
	}

	@Override
	public String getDisplayName() {
		return "ConjurSecret:" + this.variablePath;
	}
	
	public void setContext(Run<?, ?> context) {
		LOGGER.log(Level.INFO, "Setting context");
		this.context = context;
		setConjurConfiguration(getConfigurationFromContext(context));
	}
	
	public void setConjurConfiguration(ConjurConfiguration conjurConfiguration) {
		this.conjurConfiguration = conjurConfiguration;
	}
	
	@SuppressWarnings("unchecked")
	protected ConjurConfiguration getConfigurationFromContext(Run<?, ?> context) {
		LOGGER.log(Level.INFO, "Getting Configuration from Context");
		Item job = context.getParent();
		ConjurConfiguration conjurConfig = GlobalConjurConfiguration.get().getConjurConfiguration();
		for(ItemGroup<? extends Item> g = job.getParent(); g instanceof AbstractFolder; g = ((AbstractFolder<? extends Item>) g).getParent()  ) {
			FolderConjurConfiguration fconf = ((AbstractFolder<?>) g).getProperties().get(FolderConjurConfiguration.class);
			if (!(fconf == null || fconf.getInheritFromParent())) {
				// take the folder Conjur Configuration
				conjurConfig = fconf.getConjurConfiguration();
				break;
			}
		}
		LOGGER.log(Level.INFO, "<= " + conjurConfig.getApplianceURL());
		return conjurConfig;
	}

	
	@Extension
    public static class DescriptorImpl extends CredentialsDescriptor {

        @Override 
        public String getDisplayName() {
            return "Conjur Secret Credential";
        }

    }

}
