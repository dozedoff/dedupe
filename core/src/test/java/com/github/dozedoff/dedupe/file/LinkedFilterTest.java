/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.file;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.dedupe.db.dao.FileLinkDao;
import com.github.dozedoff.dedupe.db.table.FileMetaData;

@RunWith(MockitoJUnitRunner.class)
public class LinkedFilterTest {
	@Mock
	private FileLinkDao dao;

	@InjectMocks
	private LinkedFilter cut;

	private FileMetaData metaA;
	private FileMetaData metaB;
	private FileMetaData metaC;

	private List<FileMetaData> toFilter;

	@Before
	public void setUp() throws Exception {
		metaA = new FileMetaData("A");
		metaB = new FileMetaData("B");
		metaC = new FileMetaData("C");

		toFilter = Arrays.asList(metaB, metaC);

		when(dao.getLinksTo(any())).thenReturn(Collections.emptyList());
	}

	@Test
	public void testAllLinked() throws Exception {
		when(dao.getLinksTo(metaA)).thenReturn(Arrays.asList(metaB, metaC));

		assertThat(cut.filterLinked(metaA, toFilter), is(empty()));
	}

	@Test
	public void testNoneLinked() throws Exception {
		assertThat(cut.filterLinked(metaA, toFilter), containsInAnyOrder(metaB, metaC));
	}

	@Test
	public void testSourceRemovedFromFilter() throws Exception {
		toFilter = new ArrayList<FileMetaData>(toFilter);
		toFilter.add(metaA);

		assertThat(cut.filterLinked(metaA, toFilter), containsInAnyOrder(metaB, metaC));
	}
}
