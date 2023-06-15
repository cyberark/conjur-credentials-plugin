package org.conjur.jenkins.configuration;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.model.Item;
import jenkins.model.Jenkins;

/**
 * Class to hold the Folder level Conjur Configuration
 */
public class FolderConjurConfiguration extends AbstractFolderProperty<AbstractFolder<?>> {

	private Boolean inheritFromParent = true;
	private ConjurConfiguration conjurConfiguration;

	/** Constructor to set the Folder level configuration to ConjurConfiguration */
	@DataBoundConstructor
	public FolderConjurConfiguration(ConjurConfiguration conjurConfiguration) {
		super();
		this.conjurConfiguration = conjurConfiguration;
	}

	/** @return ConurConfiguration */
	public ConjurConfiguration getConjurConfiguration() {
		return conjurConfiguration;
	}

	/** set the Conjur Configuration parameter */
	@DataBoundSetter
	public void setConjurConfiguration(ConjurConfiguration conjurConfiguration) {
		this.conjurConfiguration = conjurConfiguration;
	}

	/** @return true if inheritedFromParent */
	public Boolean getInheritFromParent() {
		return inheritFromParent;
	}

	/** set the boolean value based on inheritedFromParent checkbox */
	@DataBoundSetter
	public void setInheritFromParent(Boolean inheritFromParent) {
		this.inheritFromParent = inheritFromParent;
	}

	@Extension
	public static class DescriptorImpl extends AbstractFolderPropertyDescriptor {
	}

	/** @return the Jenkins Item object baseon ownerFulleName */

	public Item getItem() {
		return Jenkins.get().getItemByFullName(this.owner.getFullName());
	}

}
