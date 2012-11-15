/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.orion.internal.server.core.tasks;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.orion.internal.server.core.IOUtilities;
import org.eclipse.orion.server.core.LogHelper;
import org.eclipse.orion.server.core.resources.Base64;

/**
 * A facility for reading/writing information about long running tasks. This store
 * will need to be reimplemented by different server implementations if they do
 * not support bare file access. This class intentionally does not understand
 * representations of tasks, to make it more easily pluggable in the future.
 */
public class TaskStore {
	private final File root;

	public TaskStore(File root) {
		this.root = root;
		this.root.mkdirs();
	}

	private String getUserDirectory(String userId) {
		return new String(Base64.encode(userId.getBytes()));
	}

	private String getUserName(String userDirectoryName) {
		try {
			return new String(Base64.decode(userDirectoryName.getBytes()));
		} catch (Exception e) {
			return null; //if this is not encoded user name than return null
		}
	}

	/**
	 * Returns a string representation of the task with the given id, or <code>null</code>
	 * if no such task exists.
	 * 
	 * @param userId id of a user that is an owner of the task
	 * @param id id of the task
	 */
	public synchronized String readTask(TaskDescription td) {
		File userDirectory = new File(root, getUserDirectory(td.getUserId()));
		if (!userDirectory.exists())
			return null;
		File taskFile = new File(userDirectory, td.getTaskId());
		if (!taskFile.exists())
			return null;
		StringWriter writer;
		FileReader reader = null;
		try {
			reader = new FileReader(taskFile);
			writer = new StringWriter();
			IOUtilities.pipe(reader, writer, true, false);
			return writer.toString();
		} catch (IOException e) {
			LogHelper.log(e);
			return null;
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					LogHelper.log(e);
					return null;
				}
		}
	}

	/**
	 * Writes task representation
	 * 
	 * @param userId id of a user that is an owner of the task
	 * @param id id of the task
	 * @param representation string representation or the task
	 */
	public synchronized void writeTask(TaskDescription td, String representation) {
		try {
			File userDirectory = new File(root, getUserDirectory(td.getUserId()));
			if (!userDirectory.exists()) {
				userDirectory.mkdir();
			}
			File taskFile = new File(userDirectory, td.getTaskId());
			FileWriter writer = new FileWriter(taskFile);
			StringReader reader = new StringReader(representation);
			IOUtilities.pipe(reader, writer, true, true);
		} catch (IOException e) {
			LogHelper.log(e);
		}
	}

	/**
	 * Removes given task from the list. This doesn't consider task status, it is caller's
	 * responsibility to make sure if task tracking can be stopped. This function does not stop the task.
	 * 
	 * @param userId id of a user that is an owner of the task
	 * @param id id of the task
	 * @return <code>true</code> if task was removed, <code>false</code> otherwise. 
	 */
	public synchronized boolean removeTask(TaskDescription td) {
		File userDirectory = new File(root, getUserDirectory(td.getUserId()));
		if (!userDirectory.exists())
			return false;
		File taskFile = new File(userDirectory, td.getTaskId());
		if (!taskFile.exists())
			return false;
		return taskFile.delete();
	}

	private List<String> internalReadAllTasks(File userDirectory) {
		List<String> result = new ArrayList<String>();
		for (File taskFile : userDirectory.listFiles()) {
			if (!taskFile.isFile())
				continue;
			StringWriter writer;
			FileReader reader = null;
			try {
				reader = new FileReader(taskFile);
				writer = new StringWriter();
				IOUtilities.pipe(reader, writer, true, false);
				result.add(writer.toString());
			} catch (IOException e) {
				LogHelper.log(e);
				return null;
			} finally {
				if (reader != null)
					try {
						reader.close();
					} catch (IOException e) {
						LogHelper.log(e);
						return null;
					}
			}
		}
		return result;
	}

	private List<TaskDescription> internalReadAllTasksDescriptions(File userDirectory) {
		List<TaskDescription> result = new ArrayList<TaskDescription>();
		String userId = getUserName(userDirectory.getName());
		if (userId == null) {
			return result; // this is not a user directory
		}
		for (File taskFile : userDirectory.listFiles()) {
			if (!taskFile.isFile())
				continue;
			result.add(new TaskDescription(userId, taskFile.getName()));
		}
		return result;
	}

	/**
	 * Returns all tasks owned by a given user.
	 * 
	 * @param userId id of a user that is an owner of tasks
	 * @return a list of tasks tracked for this user
	 */
	public synchronized List<TaskDescription> readAllTasks(String userId) {
		File userDirectory = new File(root, getUserDirectory(userId));
		if (!userDirectory.exists())
			return new ArrayList<TaskDescription>();

		return internalReadAllTasksDescriptions(userDirectory);
	}

	public synchronized List<TaskDescription> readAllTasks() {
		List<TaskDescription> result = new ArrayList<TaskDescription>();
		for (File userDirectory : root.listFiles()) {
			if (userDirectory.isDirectory()) {
				result.addAll(internalReadAllTasksDescriptions(userDirectory));
			}
		}
		return result;
	}
}
