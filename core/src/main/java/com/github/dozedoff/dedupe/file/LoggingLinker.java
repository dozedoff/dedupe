/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.file;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link FileLinker} that logs files that would be linked.
 * 
 * @author Nicholas Wright
 *
 */
public class LoggingLinker implements FileLinker {
	private static final Logger LOGGER = LoggerFactory.getLogger(LoggingLinker.class);

	/**
	 * Output a pretty log of the link actions that would be performed.
	 * 
	 * @param {@inheritDoc}
	 */
	@Override
	public void link(Path source, Path... targets) {
		link(source, Arrays.asList(targets));
	}

	/**
	 * Output a pretty log of the link actions that would be performed.
	 * 
	 * @param {@inheritDoc}
	 */
	@Override
	public void link(Path source, Collection<Path> targets) {
		LOGGER.info("Would link to {}:", source);
		for (Path target : targets) {
			LOGGER.info("    -> {}", target);
		}
	}
}
