/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.file;

import java.nio.file.Path;
import java.util.Collection;

/**
 * Interface for classes that can link files.
 * 
 * @author Nicholas Wright
 *
 */
public interface FileLinker {

	/**
	 * Create links from the targets to the source file. If the targets exist, they will be replaced with links.
	 * 
	 * @param source
	 *            to link to
	 * @param targets
	 *            to replace with links
	 */
	void link(Path source, Path... targets);

	/**
	 * Create links from the targets to the source file. If the targets exist, they will be replaced with links.
	 * 
	 * @param source
	 *            to link to
	 * @param targets
	 *            to replace with links
	 */
	void link(Path source, Collection<Path> targets);
}
