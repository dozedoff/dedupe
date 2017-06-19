/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.duplicate;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.dedupe.db.table.FileMetaData;
import com.github.dozedoff.dedupe.file.MetaData;
import com.google.common.jimfs.Jimfs;

public class VerifyMetaDataTest {
	private static final byte[] DATA_A_TEMPLATE = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };

	private FileSystem fs;

	private Path fileA;

	private FileTime timeA;
	private FileTime timeB;

	private byte[] dataA;

	private FileMetaData metaA;

	private VerifyMetaData cut;

	@Before
	public void setUp() throws Exception {
		fs = Jimfs.newFileSystem();
		
		dataA = DATA_A_TEMPLATE.clone();

		fileA = fs.getPath("A");

		Files.write(fileA, dataA);

		timeA = FileTime.from(1, TimeUnit.SECONDS);
		timeB = FileTime.from(2, TimeUnit.SECONDS);

		Files.setLastModifiedTime(fileA, timeA);

		MetaData meta = new MetaData();
		metaA = meta.createMetaDataFromFile(fileA);

		cut = new VerifyMetaData(meta, fs);
	}

	@Test
	public void testHasMatchingSize() throws Exception {
		assertThat(cut.hasMatchingSize(metaA), is(true));
	}

	@Test
	public void testHasMatchingSizeMismatch() throws Exception {
		Files.delete(fileA);
		Files.write(fileA, new byte[0]);

		assertThat(cut.hasMatchingSize(metaA), is(false));
	}

	@Test
	public void testHasMatchingModifiedTime() throws Exception {
		assertThat(cut.hasMatchingModifiedTime(metaA), is(true));
	}

	@Test
	public void testHasMatchingModifiedTimeMismatch() throws Exception {
		Files.setLastModifiedTime(fileA, timeB);

		assertThat(cut.hasMatchingModifiedTime(metaA), is(false));
	}

	@Test
	public void testHasChanged() throws Exception {
		assertThat(cut.hasChanged(metaA), is(false));
	}

	@Test
	public void testHasChangedModTime() throws Exception {
		Files.setLastModifiedTime(fileA, timeB);

		assertThat(cut.hasChanged(metaA), is(true));
	}
}
