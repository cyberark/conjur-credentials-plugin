package org.conjur.jenkins.configuration;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.conjur.jenkins.api.ConjurAPIUtils;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.remoting.Channel;
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

	static Logger getLogger() {
		return Logger.getLogger(GlobalConjurConfiguration.class.getName());
	}

	public static GlobalConjurConfiguration globalConfigurationFromMaster(Channel channel) {
		GlobalConjurConfiguration result = null;
		try {
			result = channel.call(new ConjurAPIUtils.NewGlobalConfiguration());
		} catch (IOException | InterruptedException e) {
			getLogger().log(Level.INFO, "Exception getting global configuration", e);
			e.printStackTrace();
		}
		return result;
	}

	/** @return the singleton instance */
	@Nonnull
	public static GlobalConjurConfiguration get() {
		Channel channel = Channel.current();

		GlobalConjurConfiguration result = null;
		if (channel == null) {
			result = GlobalConfiguration.all().get(GlobalConjurConfiguration.class);
		} else {
			result = globalConfigurationFromMaster(channel);
		}
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
