package org.conjur.jenkins.configuration;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;

import javax.annotation.Nonnull;

import org.kohsuke.stapler.DataBoundSetter;

/**
 * Example of Jenkins global configuration.
 */
@Extension
public class GlobalConjurConfiguration extends GlobalConfiguration {

	private ConjurConfiguration conjurConfiguration;

	/** @return the singleton instance */
	@Nonnull
	public static GlobalConjurConfiguration get() {
		GlobalConjurConfiguration result = GlobalConfiguration.all().get(GlobalConjurConfiguration.class);
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

	@DataBoundSetter
	public void setConjurConfiguration(ConjurConfiguration conjurConfiguration) {
		this.conjurConfiguration = conjurConfiguration;
		save();
	}

}
