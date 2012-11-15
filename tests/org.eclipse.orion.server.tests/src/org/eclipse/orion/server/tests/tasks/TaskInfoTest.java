/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.orion.server.tests.tasks;

import junit.framework.TestCase;

import org.eclipse.orion.server.core.tasks.CorruptedTaskException;
import org.eclipse.orion.server.core.tasks.TaskInfo;
import org.junit.Test;

/**
 * Tests for {@link TaskInfo}.
 */
public class TaskInfoTest extends TestCase {
	@Test
	public void testBadJSON() {
		TaskInfo task = AllTaskTests.createTestTask("test");
		String json = task.toJSON().toString();
		json = json.replace('}', ')');
		boolean exceptionThrown = false;
		try {
			TaskInfo.fromJSON(json);
		} catch (CorruptedTaskException e) {
			exceptionThrown = true;
		}
		assertTrue(json, exceptionThrown);

		//missing task id
		json = "{\"Message\":\"Hello\", \"TaskID\":\"foo\"}";
		exceptionThrown = false;
		try {
			TaskInfo.fromJSON(json);
		} catch (CorruptedTaskException e) {
			exceptionThrown = true;
		}
		assertTrue(json, exceptionThrown);
	}

	/**
	 * Tests the JSON representation of tasks.
	 * @throws CorruptedTaskException 
	 */
	@Test
	public void testJSONRoundTrip() throws CorruptedTaskException {
		TaskInfo info = AllTaskTests.createTestTask("test");
		TaskInfo task2 = TaskInfo.fromJSON(info.toJSON().toString());
		AllTaskTests.assertEqualTasks(info, task2);
	}

	@Test
	public void testSetMessage() {
		TaskInfo info = new TaskInfo("test", "mytask", false);
		assertEquals("", info.getMessage());
		info.setMessage("msg");
		assertEquals("msg", info.getMessage());
		info.setMessage(null);
		assertEquals("", info.getMessage());
	}

}
