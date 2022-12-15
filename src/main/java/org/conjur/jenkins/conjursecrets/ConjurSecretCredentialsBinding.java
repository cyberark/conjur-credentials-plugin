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

	// private ModelObject context;

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

	// @Override
	public MultiEnvironment bind(Run<?, ?> build, FilePath workSpace, Launcher launcher, TaskListener listener)
			throws IOException, InterruptedException {
		LOGGER.log(Level.FINE, "**** binding **** : " + build);
		ConjurCredentialStore store = ConjurCredentialStore.getAllStores()
				.get(String.valueOf(build.getParent().hashCode()));

		if (store != null) {
			LOGGER.log(Level.FINE, "Store details" + store);
			store.getProvider().getStore(build);
		}

		// ConjurSecretCredentials conjurSecretCredential = getCredentials(build);
		ConjurSecretCredentials conjurSecretCredential = getCredentialsFor(build);
		// LOGGER.log(Level.FINE, "Context Setin binding class" +
		// conjurSecretCredential.getDisplayName());

		LOGGER.log(Level.FINE, "Get Parent flage status", isParent);
		if (!isParent) {
			LOGGER.log(Level.FINE, "Context Set");
			conjurSecretCredential.setContext(build);

		} else {
			LOGGER.log(Level.FINE, "Context Set not for parent" + conjurSecretCredential.getDescription());
			if (conjurSecretCredential != null) {
				Item item = Jenkins.get().getItemByFullName(conjurSecretCredential.getDescription());// build.getParent();
				if (item != null) {
					conjurSecretCredential.setContext(item);

					LOGGER.log(Level.FINE, "Context Set not for parent" + item.getDisplayName());
				}
			}

			/*
			 * Item item = build.getParent();
			 * 
			 * if(item !=null) { String parentName =
			 * ((AbstractItem)(build.getParent()).getParent()).getParent().getDisplayName();
			 * LOGGER.log(Level.FINE, "Context Set not for parent 1"+parentName);
			 * if(!parentName.isEmpty() && !parentName.equalsIgnoreCase("Jenkins")) {
			 * LOGGER.log(Level.FINE,
			 * "Context Set not for parent 2"+build.getParent().getDisplayName());
			 * 
			 * conjurSecretCredential.setContext(((AbstractItem)(build.getParent()).
			 * getParent()).getParent());
			 * 
			 * 
			 * 
			 * } else { LOGGER.log(Level.FINE,
			 * "Context Set not for parent 3"+build.getParent().getParent().getDisplayName()
			 * ); conjurSecretCredential.setContext(build.getParent().getParent()); } }
			 */

		}

		return new MultiEnvironment(
				Collections.singletonMap(variable, conjurSecretCredential.getSecret().getPlainText()));
	}

	private final @Nonnull <C> C getCredentialsFor(@Nonnull Run<?, ?> build) throws IOException {

		IdCredentials cred = CredentialsProvider.findCredentialById(credentialsId, IdCredentials.class, build);
		LOGGER.log(Level.FINE, "Calling getCredential For1" + build.getFullDisplayName());
		String newCredentialId = "";

		if (cred == null) {

			setParent(true);

			Item item = (Item) build.getParent(); // Item item =(Item) build;

			if (item != null) {
				LOGGER.log(Level.FINE, "Item Name" + item.getParent().getDisplayName());
				newCredentialId = credentialsId.replaceAll("([${}])", "");
				LOGGER.log(Level.FINE, "CredentialId after removing ${}" + newCredentialId);

				ConjurSecretCredentials conjurSecretCredential = null;

				// conjurSecretCredential =
				// ConjurSecretCredentials.credentialWithID(newCredentialId, item);
				conjurSecretCredential = ConjurSecretCredentials.credentialWithID(newCredentialId, build);
				LOGGER.log(Level.FINE, "From Binding Credential" + conjurSecretCredential.getDisplayName());

				cred = conjurSecretCredential;

				// CredentialsProvider.findCredentialById(credentialsId,
				// IdCredentials.class,build);
				// throw new CredentialNotFoundException("Could not find credentials entry with
				// // ID '" + credentialsId + "'");
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

	public String getVariable() {
		return this.variable;
	}

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
