/****************************************************************************************
 *  FileGDBLibraryLoader.java
 *
 *  Created: Feb 7, 2013
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2013
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
package org.opensextant.giscore.input.gdb;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import org.opensextant.giscore.utils.LibraryLoader;

public class FileGDBLibraryLoader extends LibraryLoader {
	
	public FileGDBLibraryLoader() throws IOException {
		super(Package.getPackage("org.opensextant.giscore.filegdb"), "filegdb");
	}

	/* This method causes the debug library to be run while debugging. */
	public void loadLibrary() throws IOException, ParseException {
		boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().
			    getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;
		if (isDebug) {
			loadDebug();
		} else {
			super.loadLibrary();
		}
	}
	
	/**
	 * Method uses knowledge of the workspace layout to load the appropriate
	 * library when testing and debugging
	 */
	protected void loadDebug() {
		try {
			if ("win64".equals(osarch)) {
				File libpath = new File("filegdb/win64/filegdb/x64/Debug/filegdb.dll");
				System.load(libpath.getAbsolutePath());
			} else if ("linux64".equals(osarch)) {
				File libpath = new File("filegdb/linux/filegdb/dist/Debug/GNU-Linux-x86/libfilegdb.so");
				System.load(libpath.getAbsolutePath());
			} else {
				throw new RuntimeException("Architecture " + osarch + " not supported for debugger until you configure this method");
			}
		} catch(UnsatisfiedLinkError e) {
			System.err.println(e.toString());
			e.printStackTrace(System.err);
		}
	}
}
