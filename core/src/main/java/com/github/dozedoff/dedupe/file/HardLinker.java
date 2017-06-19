/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that replaces duplicate files with hard links.
 * 
 * @author Nicholas Wright
 *
 */
public class HardLinker implements FileLinker {
	private static final Logger LOGGER = LoggerFactory.getLogger(HardLinker.class);

	/**
	 * Replace the targets with hard links pointing to the source file. Existing files will be replaced with links.
	 * 
	 * @param {@inheritDoc}
	 */
	@Override
	public void link(Path source, Path... targets) {
		link(source, Arrays.asList(targets));
	}

	/**
	 * Replace the targets with hard links pointing to the source file. Existing files will be replaced with links.
	 * 
	 * @param {@inheritDoc}
	 */
	@Override
	public void link(Path source, Collection<Path> targets) {
		for (Path taget : targets) {
			try {
				if (!Files.getFileStore(source).equals(Files.getFileStore(taget))) {
					LOGGER.warn("{} and {} are not on the same filesystem, skipping...", source, taget);
					continue;
				}

				Files.deleteIfExists(taget);
				Files.createLink(taget, source);
			} catch (IOException e) {
				LOGGER.warn("Failed to create hard link from {} to {}: {}", source, taget, e.toString());
			}
		}
	}
}
