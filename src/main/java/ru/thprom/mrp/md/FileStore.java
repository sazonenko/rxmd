package ru.thprom.mrp.md;

import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by void on 31.12.2015
 */
public class FileStore {
	private final Logger log = LoggerFactory.getLogger(getClass());

	private static final String FS = "/";
	private SimpleDateFormat month = new SimpleDateFormat("yyyy-MM");
	private SimpleDateFormat day = new SimpleDateFormat("dd");

	private String storeRoot;

	public String getAbsoluteStoreDir(Message message) {
		return storeRoot + FS + getRelativeStoreDir(message);
	}

	public String getRelativeStoreDir(Message message) {
		String storeDir = message.getHeader(Constants.HEADER_ATTACHMENT_PATH, String.class);
		if (null == storeDir) {
			String source = message.getHeader(Constants.HEADER_PACKAGE_PRODUCER, "unknown", String.class);
			Date now = new Date();
			storeDir = month.format(now) + FS + day.format(now) + FS + source + FS;
			message.setHeader(Constants.HEADER_ATTACHMENT_PATH, storeDir);
		}
		return storeDir;
	}

	public String getStoreRoot() {
		return storeRoot;
	}

	public void setStoreRoot(String storeRoot) {
		this.storeRoot = storeRoot;
	}

	public void storeFile(Message message) throws MdException {
		File targetDir = new File(getAbsoluteStoreDir(message));
		if (!targetDir.exists()) {
			try {
				Files.createDirectories(targetDir.toPath());
			} catch (IOException e) {
				String msg = "Can`t create target directory [" + targetDir + "] : " + e.toString();
				log.error(msg);
				throw new MdException(msg, e);
			}
		}
		String fileName = message.getHeader(Constants.HEADER_CAMEL_FILE_NAME, "unknown", String.class);
		File targetFile = new File(targetDir, fileName);

		try {
			Object bodyObject = message.getBody();
			if (bodyObject instanceof File) {
				File inFile = message.getBody(File.class);
				log.trace("move source [{}] to file: [{}]", inFile, targetFile);
				Files.move(inFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} else {
				log.trace("write data to file: [{}]", targetFile);
				byte[] data = message.getBody(byte[].class);
				Files.write(targetFile.toPath(), data);
			}

			String xml = message.getHeader(Constants.HEADER_XML_DATA, String.class);
			if (null != xml) {
				targetFile = new File(targetDir, message.getHeader(Constants.HEADER_XML_FILE_NAME, String.class));
				Files.write(targetFile.toPath(), xml.getBytes(StandardCharsets.UTF_8));
			}
		} catch (IOException e) {
			String msg = "Can`t save file [" + targetFile + "] to store : " + e.toString();
			log.error(msg);
			throw new MdException(msg, e);
		}

	}

}
