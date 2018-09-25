package org.conjur.jenkins.ConjurSecrets;

import javax.annotation.CheckForNull;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;

import org.conjur.jenkins.api.ConjurAPI;
import org.conjur.jenkins.configuration.ConjurConfiguration;
import org.conjur.jenkins.configuration.FolderConjurConfiguration;
import org.conjur.jenkins.configuration.GlobalConjurConfiguration;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Run;
import hudson.util.Secret;

public class ConjurSecretCredentialsImpl extends BaseStandardCredentials implements ConjurSecretCredentials {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String variablePath; // to be used as Username
	
	private transient ConjurConfiguration conjurConfiguration;
	private transient Run<?, ?> context;

	@DataBoundConstructor
	public ConjurSecretCredentialsImpl(@CheckForNull CredentialsScope scope, 
			                           @CheckForNull String id, 
			                           @CheckForNull String description,
			                           @CheckForNull String variablePath) {
		super(scope, id, description);
		this.variablePath = variablePath;
	}

	public String getVariablePath() {
		return this.variablePath;
	}

	public Secret getSecret() {
		// Authenticate to Conjur
		String authToken = ConjurAPI.getAuthorizationToken(this.conjurConfiguration, context);
		// Retrieve secret from Conjur
		String secretString = ConjurAPI.getSecret(this.conjurConfiguration, authToken, this.variablePath);
		return Secret.fromString(secretString);
	}

	@Override
	public String getDisplayName() {
		return "ConjurSecret:" + this.variablePath;
	}
	
	public void setContext(Run<?, ?> context) {
		this.context = context;
		setConjurConfiguration(getConfigurationFromContext(context));
	}
	
	public void setConjurConfiguration(ConjurConfiguration conjurConfiguration) {
		this.conjurConfiguration = conjurConfiguration;
	}
	
	@SuppressWarnings("unchecked")
	protected ConjurConfiguration getConfigurationFromContext(Run<?, ?> context) {
		Item job = context.getParent();
		ConjurConfiguration conjurConfiguration = GlobalConjurConfiguration.get().getConjurConfiguration();;
		for(ItemGroup<? extends Item> g = job.getParent(); g instanceof AbstractFolder; g = ((AbstractFolder<? extends Item>) g).getParent()  ) {
			FolderConjurConfiguration fconf = ((AbstractFolder<?>) g).getProperties().get(FolderConjurConfiguration.class);
			if (fconf == null || fconf.getInheritFromParent()) {
				continue;
			} else {
				// take the folder Conjur Configuration
				conjurConfiguration = fconf.getConjurConfiguration();
				break;
			}
		}
		return conjurConfiguration;
	}

	
	@Extension
    public static class DescriptorImpl extends CredentialsDescriptor {

        @Override 
        public String getDisplayName() {
            return "Conjur Secret Credential";
        }

    }

}
