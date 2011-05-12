package com.force.cliforce.dependency;

import java.io.File;


import org.testng.Assert;
import org.testng.annotations.Test;

import com.force.cliforce.dependency.ZipUtil;

public class ZipUtilTest {

	private File destDir = new File(System.getProperty("user.home") + "/jpaPluginZipUtilTestTemp/");
	private int filecount = 77;
	
	@Test
	public void testUnzip() throws Exception {
		try {
			if(destDir.exists()) {
				ZipUtil.deleteDir(destDir);
			}
			Assert.assertNotNull(this.getClass().getClassLoader().getResource("test.war"), "test.war could not be found on the classpath");
			File warFile = new File(this.getClass().getClassLoader().getResource("test.war").getPath());
			ZipUtil.unzipWarFile(warFile, destDir);
			Assert.assertTrue(destDir.exists(), "Destination directory should exist after unzipping");
			Assert.assertEquals(countFiles(destDir), filecount, "The number of files in the expoded war is incorrect");
		} finally {
			if(destDir.exists()) {
				ZipUtil.deleteDir(destDir);
			}
		}
	}
	
	//counts the number of files below the passed in directory
	private int countFiles(File dir) {
	    if (dir.isDirectory()) {
	    	int fileCount = 0;
	        String[] children = dir.list();
	        for (int i=0; i<children.length; i++) {
	            fileCount += countFiles(new File(dir, children[i]));
	        }
	        return fileCount;
	    } else {
		    //This is a file so count as one
	    	return 1;
	    }
	}

	
}
