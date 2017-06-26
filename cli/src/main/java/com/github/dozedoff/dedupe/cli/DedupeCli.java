/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.dedupe.db.BatchWriter;
import com.github.dozedoff.dedupe.db.Database;
import com.github.dozedoff.dedupe.db.dao.FileMetaDataDao;
import com.github.dozedoff.dedupe.db.table.FileMetaData;
import com.github.dozedoff.dedupe.duplicate.CompareFile;
import com.github.dozedoff.dedupe.duplicate.HashGroup;
import com.github.dozedoff.dedupe.duplicate.SizeGroup;
import com.github.dozedoff.dedupe.duplicate.VerifyMetaData;
import com.github.dozedoff.dedupe.file.FileFinder;
import com.github.dozedoff.dedupe.file.FileLinker;
import com.github.dozedoff.dedupe.file.HardLinker;
import com.github.dozedoff.dedupe.file.LoggingLinker;
import com.github.dozedoff.dedupe.file.MetaData;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.LruObjectCache;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * The main class for Dedupe CLI
 * 
 * @author Nicholas Wright
 *
 */
public class DedupeCli {
	private static final Logger LOGGER = LoggerFactory.getLogger(DedupeCli.class);

	private Namespace ns;
	private FileMetaDataDao dao;
	Database database;
	
	public static void main(String[] args) throws SQLException {
		DedupeCli instance = new DedupeCli(args);
		instance.run();
	}

	public DedupeCli(String[] args) {
		ns = parseArgs(args);
	}

	private Namespace parseArgs(String[] args) {
		ArgumentParser parser = ArgumentParsers.newArgumentParser("Dedupe CLI").defaultHelp(true)
				.description("Find duplicate files and replace them with links");
		parser.addArgument("dir").nargs("+").help("Directories to walk for files");
		parser.addArgument("-d", "--db").help("Path to the database");
		parser.addArgument("-n", "--dry-run").help("Generate and update metadata, but do not create hard links")
				.action(Arguments.storeTrue());
		parser.addArgument("-p", "--paranoid")
				.help("Compare files with hash matches byte by byte, to be sure they match")
				.action(Arguments.storeTrue());
		parser.addArgument("-i", "--ignore").nargs("*").help("Ignore paths that match the given java regex pattern")
				.setDefault(Collections.emptyList());
		
		return parser.parseArgsOrFail(args);
	}

	private void setUpDatabase() throws SQLException {
		if (ns.getBoolean("dry_run")) {
			LOGGER.info("=== DRY RUN ===");
		}

		LOGGER.info("Opening database...");

		if (ns.getString("db") == null) {
			database = new Database();
		} else {
			database = new Database(ns.getString("db"));
		}

		dao = DaoManager.createDao(database.getConnectionSource(), FileMetaData.class);
		dao.setObjectCache(new LruObjectCache(100));

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					database.close();
				} catch (SQLException e) {
					LOGGER.error("Failed to close database: {}", e.toString());
				}
			}
		}));
	}

	private List<Path> findFiles() {
		FileFinder ff = new FileFinder(ns.getList("ignore"));

		MetaData metaData = new MetaData();
		SizeGroup sizeGroup = new SizeGroup(metaData);

		Stopwatch sw = Stopwatch.createStarted();

		for (String path : ns.<String> getList("dir")) {
			try {
				Stream<Path> stream = ff.findFiles(Paths.get(path));
				sizeGroup.add(stream);
			} catch (IOException e) {
				LOGGER.error("Failed to find files: {}", e.toString());
			}
		}

		List<Path> sizeBasedCandidates = sizeGroup.sameSizeFiles();

		LOGGER.info("Found {} files with non-unique file sizes in {}", sizeBasedCandidates.size(), sw.toString());

		return sizeBasedCandidates;
	}

	public void run() throws SQLException {
		setUpDatabase();
		
		List<Path> sizeBasedCandidates = findFiles();

		LOGGER.info("Building list of known paths...");
		
		MetaData metaData = new MetaData();

		LOGGER.info("Generating metadata for candidates...");
		Stopwatch metadataSW = Stopwatch.createStarted();
		HashGroup hashGroup = new HashGroup();

		AtomicInteger existingMeta = new AtomicInteger();
		AtomicInteger newMeta = new AtomicInteger();
		AtomicInteger totalFiles = new AtomicInteger();
		AtomicInteger updatedMeta = new AtomicInteger();

		BatchWriter<FileMetaDataDao, FileMetaData> batchWriter = new BatchWriter<FileMetaDataDao, FileMetaData>(dao);

		VerifyMetaData verify = new VerifyMetaData(metaData);

		hashGroup.add(sizeBasedCandidates.parallelStream().map(new Function<Path, FileMetaData>() {
			@Override
			public FileMetaData apply(Path t) {
				FileMetaData meta = null;

				totalFiles.getAndIncrement();

				try {
					if (dao.hasMetaData(t)) {
						existingMeta.getAndIncrement();
						meta = dao.getMetaDataForPath(t);

						if (verify.hasChanged(meta)) {
							LOGGER.info("File {} has changed, updating metadata", meta.getPath());
							updatedMeta.getAndIncrement();
							metaData.updateMetaData(meta);
							batchWriter.add(meta);
						}
					} else {
						newMeta.getAndIncrement();
						meta = metaData.createMetaDataFromFile(t);
						
						batchWriter.add(meta);
					}

					return meta;
				} catch (IOException e) {
					LOGGER.warn("Failed to generate metadata for {}: {}", t, e.toString());
				} catch (SQLException e) {
					LOGGER.warn("Failed to access database: {} cause: {}", e.toString(),
							e.getCause() == null ? "null" : e.getCause().toString());
				}

				return new FileMetaData();
			}
		}));

		LOGGER.info(
				"From a total of {} files, {} files were already known, of which {} were updated, {} new metadata entries were added and {} errors were encountered",
				totalFiles, existingMeta, updatedMeta, newMeta, 
				totalFiles.get() - newMeta.get() - existingMeta.get());
		LOGGER.info("Finished generating metadata for {} files in {}", newMeta, metadataSW);

		batchWriter.shutdown();

		Multimap<String, FileMetaData> hashBasedCandidates = hashGroup.nonUniqueMap();

		LOGGER.info("Found {} files with matching hashes in {} groups", hashBasedCandidates.size(),
				hashBasedCandidates.keySet().size());
		LOGGER.info("Comparing files by contents...");

		CompareFile compareFile = new CompareFile();
		List<Collection<FileMetaData>> duplicateGroups;
		if (ns.getBoolean("paranoid")) {
			duplicateGroups = compareFile.groupIdenticalFiles(hashBasedCandidates);
		} else {
			duplicateGroups  = new LinkedList<Collection<FileMetaData>>();
			Multimaps.asMap(hashBasedCandidates)
					.forEach((key, valueCollection) -> duplicateGroups.add(valueCollection));
		}

		LOGGER.info("After comparing and grouping, there are {} groups", duplicateGroups.size());

		FileLinker fileLinker = null;

		if (ns.getBoolean("dry_run")) {
			LOGGER.info("Using logging linker...");
			fileLinker = new LoggingLinker();
		} else {
			LOGGER.info("Using hard linker...");
			fileLinker = new HardLinker();
		}

		long skipped = 0;

		for (Collection<FileMetaData> duplicateGroup : duplicateGroups) {
			if (duplicateGroup.size() < 2) {
				skipped++;
				continue;
			}

			List<FileMetaData> duplicateList = new ArrayList<FileMetaData>(duplicateGroup);
			Collections.sort(duplicateList, new Comparator<FileMetaData>() {

				@Override
				public int compare(FileMetaData o1, FileMetaData o2) {
					return o1.getPath().compareTo(o2.getPath());
				}
			});

			Iterator<FileMetaData> iter = duplicateList.iterator();

			Path source = iter.next().getPath();
			iter.remove();

			fileLinker.link(source,
					duplicateGroup.parallelStream().map(DedupeCli::pathFromMeta).sorted().collect(Collectors.toList()));
		}

		LOGGER.info("Skipped {} groups because they contained less than two files", skipped);
	}

	private static Path pathFromMeta(FileMetaData meta) {
		return meta.getPath();
	}
}
