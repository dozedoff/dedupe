/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.file;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import com.google.common.jimfs.Jimfs;


public class FileFinderTest {
	private FileFinder cut;
	private FileSystem jimfs;

	private Path dirA;

	private Path fileA;
	private Path fileB;

	@Before
	public void setUp() throws Exception {
		jimfs = Jimfs.newFileSystem();

		cut = new FileFinder();
		setUpFileSystem();
	}

	private void setUpFileSystem() throws Exception {
		dirA = jimfs.getPath("dirA");
		fileA = dirA.resolve("fileA");
		fileB = dirA.resolve("fileB");

		Files.createDirectory(dirA);
		Files.createFile(fileA);
		Files.createFile(fileB);
	}
	
	@Test
	public void testFindFiles() throws Exception {
		Stream<Path> files = cut.findFiles(dirA);
		assertThat(files.collect(Collectors.toList()), hasItems(fileA,fileB));
	};

	@Test
	public void testNoDirectory() throws Exception {
		Stream<Path> files = cut.findFiles(dirA);
		
		assertThat(files.collect(Collectors.toList()), not(hasItem(dirA)));
	};
}
