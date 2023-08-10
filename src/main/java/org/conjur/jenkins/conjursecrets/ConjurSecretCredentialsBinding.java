package org.conjur.jenkins.conjursecrets;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.conjur.jenkins.credentials.ConjurCredentialStore;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.credentialsbinding.BindingDescriptor;
import org.jenkinsci.plugins.credentialsbinding.MultiBinding;
import org.jenkinsci.plugins.credentialsbinding.impl.CredentialNotFoundException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.IdCredentials;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;

/**
 * ConjurSecretCredentialsBinding entry level class to when build is invoked to
 * authorize and retrieve secrets
 */
public class ConjurSecretCredentialsBinding extends MultiBinding<ConjurSecretCredentials> {

	@Symbol("conjurSecretCredential")
	@Extension
	public static class DescriptorImpl extends BindingDescriptor<ConjurSecretCredentials> {

		@Override
		public String getDisplayName() {
			return "Conjur Secret credentials";
		}

		@Override
		public boolean requiresWorkspace() {
			return false;
		}

		@Override
		protected Class<ConjurSecretCredentials> type() {
			return ConjurSecretCredentials.class;
		}
	}

	private static final Logger LOGGER = Logger.getLogger(ConjurSecretCredentialsBinding.class.getName());

	private String variable;

	private String credentialsId;

	private boolean isParent;

	public boolean isParent() {
		return isParent;
	}

	public void setParent(boolean isParent) {
		this.isParent = isParent;
	}

	@DataBoundConstructor
	public ConjurSecretCredentialsBinding(String credentialsId) {
		super(credentialsId);
		this.credentialsId = credentialsId;
	}

	/**
	 * Bind method invoked on Jenkins build process
	 */
	// @Override
	public MultiEnvironment bind(Run<?, ?> build, FilePath workSpace, Launcher launcher, TaskListener listener)
			throws IOException, InterruptedException {

		LOGGER.log(Level.FINE, "**** binding **** : {0}", build);
		ConjurCredentialStore store = ConjurCredentialStore.getAllStores()
				.get(String.valueOf(build.getParent().hashCode()));

		if (store != null) {
			LOGGER.log(Level.FINE, "Store details >> {0}", store);
			store.getProvider().getStore(build);
		}

		ConjurSecretCredentials conjurSecretCredential = getCredentialsForBind(build);

		LOGGER.log(Level.FINE, "Get Parent flage status >> {0}", isParent);
		if (!isParent) {
			LOGGER.log(Level.FINE, "Context Set");
			conjurSecretCredential.setContext(build);

		} else {
			LOGGER.log(Level.FINE, "Context Set not for parent >> {0}", conjurSecretCredential.getDescription());
			if (conjurSecretCredential != null) {
				Item item = Jenkins.get().getItemByFullName(conjurSecretCredential.getDescription());
				if (item != null) {
					conjurSecretCredential.setContext(item);

					LOGGER.log(Level.FINE, "Context Set not for parent >> {0}", item.getDisplayName());
				}
			}

		}

		return new MultiEnvironment(
				Collections.singletonMap(variable, conjurSecretCredential.getSecret().getPlainText()));
	}

	private final @Nonnull <C> C getCredentialsForBind(@Nonnull Run<?, ?> build) throws IOException {

		IdCredentials cred = CredentialsProvider.findCredentialById(credentialsId, IdCredentials.class, build);
		LOGGER.log(Level.FINE, "Calling getCredential >> {0}", build.getFullDisplayName());
		String newCredentialId = "";

		if (cred == null) {

			setParent(true);

			Item item = (Item) build.getParent();

			if (item != null) {
				LOGGER.log(Level.FINE, "Item Name >> {0}", item.getParent().getDisplayName());
				newCredentialId = credentialsId.replaceAll("([${}])", "");
				LOGGER.log(Level.FINE, "CredentialId after removing ${} >> {0}", newCredentialId);

				ConjurSecretCredentials conjurSecretCredential = null;

				conjurSecretCredential = ConjurSecretCredentials.credentialWithID(newCredentialId, build);
				LOGGER.log(Level.FINE, "From Binding Credential >> {0}", conjurSecretCredential.getDisplayName());

				cred = conjurSecretCredential;

			}
		}

		if (type().isInstance(cred)) {
			CredentialsProvider.track(build, cred);
			return (C) type().cast(cred);
		}

		Descriptor<?> expected = Jenkins.getActiveInstance().getDescriptor(type());
		throw new CredentialNotFoundException(
				"Credentials '" + credentialsId + "' is of type '" + cred.getDescriptor().getDisplayName() + "' where '"
						+ (expected != null ? expected.getDisplayName() : type().getName()) + "' was expected");

	}

	/** @return variable */
	public String getVariable() {
		return this.variable;
	}

	/** set the variable */
	@DataBoundSetter
	public void setVariable(String variable) {
		LOGGER.log(Level.FINE, "Setting variable to {0}", variable);
		this.variable = variable;
	}

	@Override
	protected Class<ConjurSecretCredentials> type() {
		return ConjurSecretCredentials.class;
	}

	@Override
	public Set<String> variables() {
		return Collections.singleton(variable);
	}

}
