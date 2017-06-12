/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.duplicate;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.dedupe.file.MetaData;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;

public class SizeGroup {
	private static final Logger LOGGER = LoggerFactory.getLogger(SizeGroup.class);

	private final Multimap<Long, Path> sizeGroups;
	private final MetaData metaData;

	/**
	 * Create an instance that can group files based on size.
	 * 
	 * @param metaData
	 *            to use for file size queries
	 */
	public SizeGroup(MetaData metaData) {
		this.sizeGroups = MultimapBuilder.treeKeys().hashSetValues().build();
		this.metaData = metaData;
	}

	/**
	 * Add the paths in the stream to the group, based on the file size of the path.
	 * 
	 * @param stream
	 *            of files to group by size
	 */
	public void add(Stream<Path> stream) {
		Multimap<Long, Path> sync = Multimaps.synchronizedMultimap(sizeGroups);

		stream.parallel().forEach(new Consumer<Path>() {
			@Override
			public void accept(Path t) {
				try {
					sync.put(metaData.size(t), t);
				} catch (IOException e) {
					LOGGER.warn("Failed to get size for {}: {}", t, e.toString());
				}
				
			}
		});
	}

	/**
	 * Get the files that have the same size as at least one other file.
	 * 
	 * @return a list of files with at least a other same size file
	 */
	public List<Path> sameSizeFiles() {
		List<Path> sameSize = new LinkedList<Path>();
		sizeGroups.asMap().values().forEach(new Consumer<Collection<Path>>() {

			@Override
			public void accept(Collection<Path> t) {
				if (t.size() > 1) {
					sameSize.addAll(t);
				}
			}
		});

		
		return sameSize;
	}
}
