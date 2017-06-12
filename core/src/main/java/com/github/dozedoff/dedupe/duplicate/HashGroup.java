/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.duplicate;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.dedupe.db.table.FileMetaData;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.hash.HashCode;

public class HashGroup {
	private static final Logger LOGGER = LoggerFactory.getLogger(HashGroup.class);

	private final Multimap<String, FileMetaData> hashGroups;

	/**
	 * Create an instance that can group files based on hash.
	 */
	public HashGroup() {
		this.hashGroups = MultimapBuilder.treeKeys().hashSetValues().build();
	}

	/**
	 * Add the paths in the stream to the group, based on the file hash of the path.
	 * 
	 * @param stream
	 *            of files to group by hash
	 */
	public void add(Stream<FileMetaData> stream) {
		Multimap<String, FileMetaData> sync = Multimaps.synchronizedMultimap(hashGroups);

		stream.parallel().forEach(new Consumer<FileMetaData>() {
			@Override
			public void accept(FileMetaData t) {
				sync.put(HashCode.fromBytes(t.getHash()).toString(), t);
			}
		});

		LOGGER.info("Currently mapped {} files to {} unique hashes", hashGroups.size(), hashGroups.keySet().size());
	}

	/**
	 * Get the files that have the same hash as at least one other file.
	 * 
	 * @return a list of files with at least a other same size file
	 */
	public List<FileMetaData> sameHash() {
		List<FileMetaData> sameHash = new LinkedList<FileMetaData>();

		hashGroups.asMap().values().forEach(new Consumer<Collection<FileMetaData>>() {
			@Override
			public void accept(Collection<FileMetaData> t) {
				if (t.size() > 1) {
					sameHash.addAll(t);
				}
			}
		});
		
		return sameHash;
	}
}
