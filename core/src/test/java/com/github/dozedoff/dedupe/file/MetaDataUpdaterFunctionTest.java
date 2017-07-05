/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.file;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.dedupe.db.BatchWriter;
import com.github.dozedoff.dedupe.db.Database;
import com.github.dozedoff.dedupe.db.dao.FileLinkDao;
import com.github.dozedoff.dedupe.db.dao.FileMetaDataDao;
import com.github.dozedoff.dedupe.db.table.FileLink;
import com.github.dozedoff.dedupe.db.table.FileMetaData;
import com.github.dozedoff.dedupe.duplicate.VerifyMetaData;
import com.google.common.jimfs.Jimfs;
import com.j256.ormlite.dao.DaoManager;

public class MetaDataUpdaterFunctionTest {
	private MetaDataUpdaterFunction cut;

	private Database database;
	private FileMetaDataDao metaDao;
	private FileLinkDao linkDao;
	private BatchWriter<FileMetaDataDao, FileMetaData> batchWriter;

	private FileSystem fs;
	private MetaData metaData;
	private VerifyMetaData verify;

	private Path existingFile;
	private Path newFile;
	private Path modifiedFile;

	private FileLink link;
	private FileMetaData existingMeta;
	private FileMetaData modifiedMeta;

	@Before
	public void setUp() throws Exception {
		database = Database.inMemoryDatabase();
		metaDao = DaoManager.createDao(database.getConnectionSource(), FileMetaData.class);
		linkDao = DaoManager.createDao(database.getConnectionSource(), FileLink.class);
		batchWriter = new BatchWriter<FileMetaDataDao, FileMetaData>(metaDao);

		fs = Jimfs.newFileSystem();
		metaData = new MetaData(fs);
		verify = new VerifyMetaData(metaData, fs);

		createFiles();

		cut = new MetaDataUpdaterFunction(metaDao, linkDao, verify, metaData, batchWriter);
	}

	private void createFiles() throws IOException, SQLException {
		existingFile = fs.getPath("existing");
		newFile = fs.getPath("new");
		modifiedFile = fs.getPath("modified");

		writeFile(existingFile);
		writeFile(modifiedFile);
		writeFile(newFile);

		existingMeta = metaData.createMetaDataFromFile(existingFile);
		modifiedMeta = metaData.createMetaDataFromFile(modifiedFile);

		metaDao.create(existingMeta);
		metaDao.create(modifiedMeta);

		link = new FileLink(existingMeta, modifiedMeta);
		linkDao.create(link);

		Files.setLastModifiedTime(modifiedFile, FileTime.fromMillis(0));

	}

	private void writeFile(Path path) throws IOException {
		Files.write(path, new byte[0]);
	}

	@After
	public void tearDown() throws Exception {
		database.close();
	}

	@Test
	public void testSetupLinkToExistingMetadataExists() throws Exception {
		assertThat(linkDao.getLinksTo(existingMeta), is(not(empty())));
	}

	@Test
	public void testSetupNewFileMetadataIsNotStored() throws Exception {
		assertThat(metaDao.getMetaDataForPath(newFile), is(nullValue()));
	}

	@Test
	public void testExisting() throws Exception {
		cut.apply(existingFile);

		assertThat(cut.existing(), is(1));
	}

	@Test
	public void testCreated() throws Exception {
		cut.apply(newFile);

		assertThat(cut.created(), is(1));
	}

	@Test
	public void testUpdated() throws Exception {
		cut.apply(modifiedFile);

		assertThat(cut.updated(), is(1));
	}

	@Test
	public void testUpdateRemovesLink() throws Exception {
		cut.apply(modifiedFile);
		
		assertThat(linkDao.getLinksTo(existingMeta), is(empty()));
	}

	@Test
	public void testChangedFileIsUpdated() throws Exception {
		cut.apply(modifiedFile);

		batchWriter.flush();
		metaDao.clearObjectCache();

		assertThat(metaDao.getMetaDataForPath(modifiedFile).getModifiedTime(), is(0L));
	}

	@Test
	public void testNewMetadataStored() throws Exception {
		cut.apply(newFile);
		batchWriter.flush();

		assertThat(metaDao.getMetaDataForPath(newFile), is(notNullValue()));
	}

	@Test
	public void testTotalExisting() throws Exception {
		cut.apply(existingFile);

		assertThat(cut.total(), is(1));
	}

	@Test
	public void testTotalUpdated() throws Exception {
		cut.apply(modifiedFile);

		assertThat(cut.total(), is(1));
	}

	@Test
	public void testTotalCreated() throws Exception {
		cut.apply(newFile);

		assertThat(cut.total(), is(1));
	}

	@Test
	public void testIoError() throws Exception {
		assertThat(cut.apply(fs.getPath("")), is(new FileMetaData()));
	}

	@Test
	public void testDatabaseError() throws Exception {
		database.close();

		assertThat(cut.apply(existingFile), is(new FileMetaData()));
	}

	@Test
	public void testErrorsFromDatabase() throws Exception {
		database.close();

		cut.apply(existingFile);

		assertThat(cut.errors(), is(1));
	}

	@Test
	public void testErrorsFromFile() throws Exception {
		cut.apply(fs.getPath(""));

		assertThat(cut.errors(), is(1));
	}

	@Test
	public void testErrorsFromMissingFile() throws Exception {
		Files.delete(existingFile);
		cut.apply(existingFile);

		assertThat(cut.errors(), is(1));
	}
}
