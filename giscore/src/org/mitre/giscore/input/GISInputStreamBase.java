/****************************************************************************************
 *  GISInputStreamBase.java
 *
 *  Created: Jan 26, 2009
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2009
 *
 *  The program is provided "as is" without any warranty express or implied, including
 *  the warranty of non-infringement and the implied warranties of merchantibility and
 *  fitness for a particular purpose.  The Copyright owner will not be liable for any
 *  damages suffered by you as a result of using the Program.  In no event will the
 *  Copyright owner be liable for any special, indirect or consequential damages or
 *  lost profits even if the Copyright owner has been advised of the possibility of
 *  their occurrence.
 *
 ***************************************************************************************/
package org.mitre.giscore.input;

import java.util.LinkedList;

import org.mitre.giscore.events.IGISObject;

/**
 * Base class that handles the mark and reset behavior.
 * 
 * @author DRAND
 * 
 */
public abstract class GISInputStreamBase implements IGISInputStream {
	/**
	 * Buffered elements that should be returned. This allows a single call to
	 * read to find several elements and return them in the right order.
	 */
	private final LinkedList<IGISObject> buffered = new LinkedList<IGISObject>();

	/**
	 * Add an element to be return later to the beginning of the list
	 * 
	 * @param obj
	 */
	protected void addFirst(IGISObject obj) {
		buffered.addFirst(obj);
	}

	/**
	 * Add an element to be return later to the end of the list
	 * 
	 * @param obj
	 */
	protected void addLast(IGISObject obj) {
		buffered.add(obj);
	}

	/**
	 * @return
	 */
	protected IGISObject readSaved() {
		return buffered.removeFirst();
	}

	/**
	 * @return
	 */
	protected boolean hasSaved() {
		return !buffered.isEmpty();
	}
}
