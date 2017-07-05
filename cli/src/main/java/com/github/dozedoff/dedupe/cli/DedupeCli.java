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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.dedupe.db.BatchWriter;
import com.github.dozedoff.dedupe.db.Database;
import com.github.dozedoff.dedupe.db.dao.FileLinkDao;
import com.github.dozedoff.dedupe.db.dao.FileMetaDataDao;
import com.github.dozedoff.dedupe.db.table.FileLink;
import com.github.dozedoff.dedupe.db.table.FileMetaData;
import com.github.dozedoff.dedupe.duplicate.CompareFile;
import com.github.dozedoff.dedupe.duplicate.HashGroup;
import com.github.dozedoff.dedupe.duplicate.SizeGroup;
import com.github.dozedoff.dedupe.duplicate.VerifyMetaData;
import com.github.dozedoff.dedupe.file.FileFinder;
import com.github.dozedoff.dedupe.file.FileLinker;
import com.github.dozedoff.dedupe.file.HardLinker;
import com.github.dozedoff.dedupe.file.LinkedFilter;
import com.github.dozedoff.dedupe.file.LoggingLinker;
import com.github.dozedoff.dedupe.file.MetaData;
import com.github.dozedoff.dedupe.file.MetaDataUpdaterFunction;
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
	private FileLinkDao linkDao;
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

		linkDao = DaoManager.createDao(database.getConnectionSource(), FileLink.class);
		linkDao.setObjectCache(new LruObjectCache(100));

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

		BatchWriter<FileMetaDataDao, FileMetaData> batchWriter = new BatchWriter<FileMetaDataDao, FileMetaData>(dao);

		VerifyMetaData verify = new VerifyMetaData(metaData);
		MetaDataUpdaterFunction metaUpdate = new MetaDataUpdaterFunction(dao, linkDao, verify, metaData,
				batchWriter);

		hashGroup.add(sizeBasedCandidates.parallelStream().map(metaUpdate));

		LOGGER.info(
				"From a total of {} files, {} files were already known, of which {} were updated, {} new metadata entries were added and {} errors were encountered",
				metaUpdate.total(), metaUpdate.existing(), metaUpdate.updated(), metaUpdate.created(),
				metaUpdate.errors());

		LOGGER.info("Finished generating metadata for {} files in {}", metaUpdate.created() + metaUpdate.updated(),
				metadataSW);

		batchWriter.flush();

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
		long linked = 0;

		LinkedFilter linkedFilter = new LinkedFilter(linkDao);

		Stopwatch linkTime = Stopwatch.createStarted();

		for (Collection<FileMetaData> duplicateGroup : duplicateGroups) {
			if (isValidDuplicateGroup(duplicateGroup)) {
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

			FileMetaData source = iter.next();
			iter.remove();
			
			duplicateGroup = linkedFilter.filterLinked(source, duplicateList);

			if (duplicateGroup.isEmpty()) {
				skipped++;
				continue;
			}

			linked++;

			boolean allOk = fileLinker.link(source.getPath(),
					duplicateGroup.parallelStream().map(DedupeCli::pathFromMeta).collect(Collectors.toList()));

			if (allOk && !ns.getBoolean("dry_run")) {
				duplicateGroup.parallelStream().forEach(meta -> {
					try {
						linkDao.linkFiles(source, meta);
						batchWriter.add(meta);
					} catch (SQLException e) {
						LOGGER.warn("Failed to link {} to {} due to: {}", meta, source, e.toString());
					}
				});
			}
		}

		batchWriter.shutdown();
		LOGGER.info("In {}, linked {} groups and skipped {} groups", linkTime, linked, skipped);
	}

	private boolean isValidDuplicateGroup(Collection<FileMetaData> duplicateGroup) {
		return duplicateGroup.size() < 2;
	}

	private static Path pathFromMeta(FileMetaData meta) {
		return meta.getPath();
	}
}
