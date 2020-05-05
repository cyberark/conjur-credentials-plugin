package org.conjur.jenkins.configuration;

import hudson.Extension;
import hudson.remoting.Channel;
import jenkins.model.GlobalConfiguration;
import jenkins.security.SlaveToMasterCallable;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.kohsuke.stapler.DataBoundSetter;

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

	/** @return the singleton instance */
	@Nonnull
	public static GlobalConjurConfiguration get() {
		Channel channel = Channel.current();

		GlobalConjurConfiguration result = null;
		if (channel == null) {
			result = GlobalConfiguration.all().get(GlobalConjurConfiguration.class);
		} else {
			try {
				result = channel.call(new NewGlobalConfiguration());
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				getLogger().log(Level.INFO, "Exception getting global configuration", e);
				e.printStackTrace();
			}
		}
		if (result == null) {
			throw new IllegalStateException();
		}
		return result;
	}

	static class NewGlobalConfiguration extends SlaveToMasterCallable<GlobalConjurConfiguration, IOException> {
		/**
		 * Standardize serialization.
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * {@inheritDoc}
		 */
		public GlobalConjurConfiguration call() throws IOException {
			GlobalConjurConfiguration result = GlobalConfiguration.all().get(GlobalConjurConfiguration.class);
			return result;
		}
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
