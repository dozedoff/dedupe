/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.file;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.dedupe.db.dao.FileLinkDao;
import com.github.dozedoff.dedupe.db.table.FileMetaData;

public class LinkedFilter {
	private static final Logger LOGGER = LoggerFactory.getLogger(LinkedFilter.class);

	private final FileLinkDao dao;

	/**
	 * Create a filter for removing already linked files.
	 * 
	 * @param dao
	 *            to use for link queries
	 */
	public LinkedFilter(FileLinkDao dao) {
		this.dao = dao;
	}

	/**
	 * Remove all elements that have already been linked. Removes source from toFilter if present.
	 * 
	 * @param source
	 *            that the elements link to
	 * @param toFilter
	 *            elements to filter
	 * @return a list containing all unlinked elements, will never contain source
	 */
	public List<FileMetaData> filterLinked(FileMetaData source, Collection<FileMetaData> toFilter) {
		List<FileMetaData> filterList = new ArrayList<FileMetaData>(toFilter);
		filterList.remove(source);

		Iterator<FileMetaData> iter = filterList.iterator();
		
		List<FileMetaData> knownLinks;

		try {
			knownLinks = dao.getLinksTo(source);
		} catch (SQLException e) {
			LOGGER.warn("Failed to get known links for {}: {}", source, e.toString());
			knownLinks = Collections.emptyList();
		}
		
		while(iter.hasNext()) {
			FileMetaData meta = iter.next();
			
			if (knownLinks.contains(meta)) {
				iter.remove();
			}
		}
		
		return filterList;
	}
}
