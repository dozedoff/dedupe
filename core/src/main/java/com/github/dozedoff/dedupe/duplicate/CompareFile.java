/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.duplicate;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.dedupe.db.table.FileMetaData;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * Class for comparing files.
 * 
 * @author Nicholas Wright
 *
 */
public class CompareFile {
	private static final Logger LOGGER = LoggerFactory.getLogger(CompareFile.class);
	private final FileSystem fileSystem;

	/**
	 * Create a new instance to group identical files using the {@link FileSystems#getDefault()} filesystem.
	 */
	public CompareFile() {
		this.fileSystem = FileSystems.getDefault();
	}

	/**
	 * Create a new instance to group identical files using the provided {@link FileSystem}.
	 * 
	 * @param fileSystem
	 *            the file system to use for resolving paths
	 */
	public CompareFile(FileSystem fileSystem) {
		this.fileSystem = fileSystem;
	}

	/**
	 * Compares the contents of two files
	 * 
	 * @param path1
	 *            of a file to compare
	 * @param path2
	 *            of a file to compare against
	 * @return true if the files have the same contents
	 * @throws IOException
	 *             if there is an error accessing the files
	 */
	public static boolean equal(Path path1, Path path2) throws IOException {
		LOGGER.trace("Comparing {} to {}", path1, path2);
		try (InputStream is1 = new BufferedInputStream(Files.newInputStream(path1))) {
			try (InputStream is2 = new BufferedInputStream(Files.newInputStream(path2))) {
				int read;
				long index = 0;

				while (true) {
					read = is1.read();

					if (read == -1) {
						boolean identical;

						if (is2.read() == -1) {
							LOGGER.trace("{} and {} are identical", path1, path2);
							identical = true;
						} else {
							LOGGER.trace("{} contents is shorter than {}, finished comparing at position {}", path1,
									path2, index);
							identical = false;
						}

						return identical;
					}

					if (read != is2.read()) {
						LOGGER.trace("{} differs from {} at byte position {}", path2, path1, index);
						return false;
					}

					index++;
				}
			}
		}
	}

	/**
	 * Group identical files into sets.
	 * 
	 * @param identaicalCandidates
	 *            possible identical files
	 * @return a list of identical file sets
	 */
	public List<Collection<FileMetaData>> groupIdenticalFiles(
			Multimap<String, FileMetaData> identaicalCandidates) {

		List<Collection<FileMetaData>> identicalFileGroups = new LinkedList<Collection<FileMetaData>>();
		List<Collection<FileMetaData>> candidatesToGroup = new LinkedList<Collection<FileMetaData>>();
		
		Multimaps.asMap(identaicalCandidates).forEach((key, valueCollection) -> {
			candidatesToGroup.add(valueCollection);
		});

		for (Collection<FileMetaData> collection : candidatesToGroup) {
			identicalFileGroups.addAll(groupFiles(collection));
		}
		
		return identicalFileGroups;
	}

	private List<Collection<FileMetaData>> groupFiles(Collection<FileMetaData> toGroup) {
		List<Collection<FileMetaData>> identicalFileGroups = new LinkedList<Collection<FileMetaData>>();
		
		while (!toGroup.isEmpty()) {
			Iterator<FileMetaData> iter = toGroup.iterator();
			Set<FileMetaData> identicalFiles = new HashSet<FileMetaData>();

			FileMetaData current = iter.next();
			iter.remove();
			identicalFiles.add(current);

			while (iter.hasNext()) {
				FileMetaData toCompare = iter.next();
				
				try {
					if (equal(current.getPath(fileSystem), toCompare.getPath(fileSystem))) {
						iter.remove();
						identicalFiles.add(toCompare);
					}
				} catch (IOException e) {
					LOGGER.warn("Failed to compare files: {}", e.toString());
				}
			}

			identicalFileGroups.add(identicalFiles);
		}
		
		return identicalFileGroups;
	}
}
