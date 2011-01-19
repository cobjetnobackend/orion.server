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
package org.eclipse.orion.internal.server.servlets.xfer;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.orion.internal.server.core.IWebResourceDecorator;
import org.eclipse.orion.internal.server.servlets.ProtocolConstants;
import org.eclipse.orion.server.core.LogHelper;
import org.json.*;

/**
 * Adds links to the import and export services for files in the workspace.
 */
public class TransferResourceDecorator implements IWebResourceDecorator {

	/*(non-Javadoc)
	 * @see org.eclipse.orion.internal.server.core.IWebResourceDecorator#addAtributesFor(java.net.URI, org.json.JSONObject)
	 */
	public void addAtributesFor(URI resource, JSONObject representation) {
		IPath targetPath = new Path(resource.getPath());
		if (targetPath.segmentCount() > 1 && "file".equals(targetPath.segment(0))) { //$NON-NLS-1$
			targetPath = targetPath.removeFirstSegments(1);
			try {
				addTransferLinks(resource, representation, targetPath);
				JSONArray children = representation.optJSONArray(ProtocolConstants.KEY_CHILDREN);
				if (children != null) {
					for (int i = 0; i < children.length(); i++) {
						JSONObject child = children.getJSONObject(i);
						if (child.getBoolean(ProtocolConstants.KEY_DIRECTORY)) {
							IPath childPath = targetPath.append(child.getString(ProtocolConstants.KEY_NAME));
							addTransferLinks(resource, child, childPath);
						}
					}
				}
			} catch (Exception e) {
				//log and continue
				LogHelper.log(e);
			}
		}
	}

	private void addTransferLinks(URI resource, JSONObject representation, IPath targetPath) throws URISyntaxException, JSONException {
		IPath path = new Path("/xfer").append(targetPath); //$NON-NLS-1$
		URI link = new URI(resource.getScheme(), resource.getAuthority(), path.toString(), null, null);
		representation.put(ProtocolConstants.KEY_IMPORT_LOCATION, link.toString());
		path = new Path("/xfer/export").append(targetPath).addFileExtension("zip"); //$NON-NLS-1$ //$NON-NLS-2$
		link = new URI(resource.getScheme(), resource.getAuthority(), path.toString(), null, null);
		representation.put(ProtocolConstants.KEY_EXPORT_LOCATION, link.toString());
	}

}
