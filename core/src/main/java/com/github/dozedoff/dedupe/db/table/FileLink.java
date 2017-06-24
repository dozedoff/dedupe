/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.db.table;

import java.util.Objects;

import com.github.dozedoff.dedupe.db.dao.FileLinkDao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Database table to track linked files
 * 
 * @author Nicholas Wright
 *
 */
@DatabaseTable(daoClass = FileLinkDao.class)
public class FileLink {
	@DatabaseField(generatedId = true)
	private int id;

	@DatabaseField(index = true, foreign = true, foreignAutoRefresh = true)
	private FileMetaData source;

	/**
	 * Unique because a file can only ever be linked to one source.
	 */
	@DatabaseField(index = true, unique = true, foreign = true, foreignAutoRefresh = true)
	private FileMetaData link;

	/**
	 * Create a new link with the given {@link FileMetaData}.
	 * 
	 * @param source
	 *            of the link
	 * @param link
	 *            the link itself
	 */
	public FileLink(FileMetaData source, FileMetaData link) {
		this.source = source;
		this.link = link;
	}

	/**
	 * Create a new empty file link. Used by the DAO.
	 */
	public FileLink() {
	}

	/**
	 * Get the source for the link
	 * 
	 * @return source {@link FileMetaData}
	 */
	public FileMetaData getSource() {
		return source;
	}

	/**
	 * Get the link.
	 * 
	 * @return link {@link FileMetaData}
	 */
	public FileMetaData getLink() {
		return link;
	}

	/**
	 * Check if the instance is equal. Instances are equal if source and link match.
	 * 
	 * @param obj
	 *            instance to compare
	 * 
	 * @return true if the instances are equal
	 */
	@Override
	public final boolean equals(Object obj) {
		if(obj instanceof FileLink) {
			FileLink other = (FileLink) obj;
			
			return Objects.equals(this.link, other.link) && Objects.equals(this.source, other.source);
		}
		
		return false;
	}

	/**
	 * Generates a hashcode based on the source and link.
	 * 
	 * @return the hashcode of this instance
	 */
	@Override
	public final int hashCode() {
		return Objects.hash(this.link, this.source);
	}
}
