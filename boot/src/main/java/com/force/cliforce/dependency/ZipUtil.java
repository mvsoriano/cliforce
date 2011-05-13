package com.force.cliforce.dependency;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class ZipUtil {

	public static final String TEMP_SUB_DIR_NAME = ".force/warTemporaryDir/";
	
	/**
	 * Unzip the war or zip file to the destination directory
	 * 
	 * @param file
	 * @param destinationDir
	 * @throws ZipException if war/zip file is invalid
	 * @throws IOException on file and directory errors
	 */
	public static void unzipWarFile(File file, File destinationDir) throws ZipException, IOException {

		int BUFFER = 2048;

		ZipFile zip = new ZipFile(file);

		destinationDir.mkdir();
		Enumeration<? extends ZipEntry> zipFileEntries = zip.entries();

		// Process each entry
		while (zipFileEntries.hasMoreElements()) {
			// grab a zip file entry
			ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();

			String currentEntry = entry.getName();

			File destFile = new File(destinationDir, currentEntry);
			File destinationParent = destFile.getParentFile();

			// create the parent directory structure if needed
			destinationParent.mkdirs();
			if (!entry.isDirectory()) {
				BufferedInputStream is = new BufferedInputStream(
						zip.getInputStream(entry));
				int currentByte;
				// establish buffer for writing file
				byte data[] = new byte[BUFFER];

				// write the current file to disk
				FileOutputStream fos = new FileOutputStream(destFile);
				BufferedOutputStream dest = new BufferedOutputStream(fos,
						BUFFER);

				// read and write until last byte is encountered
				while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
					dest.write(data, 0, currentByte);
				}
				dest.flush();
				dest.close();
				is.close();
			}
		}
	}
	
	/**
	 * Deletes all files and subdirectories under dir.
	 * If a deletion fails, the method stops attempting to delete and returns false.
	 * 
	 * @return true if all deletions were successful
	 */
	public static boolean deleteDir(File dir) {
	    if (dir.isDirectory()) {
	        String[] children = dir.list();
	        for (int i=0; i<children.length; i++) {
	            boolean success = deleteDir(new File(dir, children[i]));
	            if (!success) {
	                return false;
	            }
	        }
	    }
	    // The directory is now empty so delete it
	    return dir.delete();
	}

}
