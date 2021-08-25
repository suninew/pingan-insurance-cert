package com.pingan.pinganproject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;

import org.apache.log4j.Logger;

public class CheckMissingRecord {
	private static Logger logger = Logger.getLogger(CheckMissingRecord.class);

	public static void main(String[] args) throws IOException {

		File downloadFileDirFile = new File("file/download/");
		HashSet<String> downloadedFileSet = new HashSet<>();

		for (File downloadFile : downloadFileDirFile.listFiles())
			downloadedFileSet.add(downloadFile.getName().split("[.]")[0]);

		// Read configure from file/config.txt
		File configFile = new File("file/config.txt");
		InputStreamReader inputReader = new InputStreamReader(new FileInputStream(configFile), "UTF-8");
		BufferedReader bf = new BufferedReader(inputReader);
		String str;
		// skip first line
		str = bf.readLine();

		while ((str = bf.readLine()) != null)
			if (!str.equalsIgnoreCase(""))
				if (!downloadedFileSet.contains(str.split(",")[0]))
					logger.error("记录 " + str + " 相关凭证没有成功导出！");
		bf.close();
		inputReader.close();

	}

}
