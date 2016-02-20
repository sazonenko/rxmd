package ru.thprom.mrp.md.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import ru.thprom.mrp.md.Constants;
import ru.thprom.mrp.md.FileStore;
import ru.thprom.mrp.md.MdException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by void on 18.02.16
 */
public class XmlMessageParser {
	private static final Logger log = LoggerFactory.getLogger(XmlMessageParser.class);

	private FileStore fileStore;

	private ThreadLocal<DocumentBuilder> localBuilder = new ThreadLocal<>();
	private XPathFactory xPathFactory = XPathFactory.newInstance();
	private Map<String, XPathExpression> items = new HashMap<>();

	public void processXml(Map<String, Object> m) {
		DocumentBuilder docBuilder = getDocumentBuilder();

		String fileName = (String) m.get(Constants.HEADER_CAMEL_FILE_NAME);
		try {
			File storeDir = new File(fileStore.getStoreRoot(), (String) m.get(Constants.HEADER_ATTACHMENT_PATH));
			File file = new File(storeDir, fileName);

			Document doc;
			try (InputStream stream = new FileInputStream(file)) {
				doc = docBuilder.parse(stream);
			}

			for (String key : items.keySet()) {
				m.put(key, items.get(key).evaluate(doc, XPathConstants.STRING));
			}

		} catch (Exception e) {
			log.error("error in parsing XML "+ fileName, e);
		}
	}

	private DocumentBuilder getDocumentBuilder() {
		DocumentBuilder docBuilder = localBuilder.get();
		if (null == docBuilder) {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			try {
				docBuilder = docBuilderFactory.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				throw new MdException("Can't create parser", e);
			}
			localBuilder.set(docBuilder);
		}
		docBuilder.reset();
		return docBuilder;
	}

	public void setFileStore(FileStore fileStore) {
		this.fileStore = fileStore;
	}

	public void setXPaths(Map<String, String> paths) throws XPathExpressionException {
		for (String key : paths.keySet()) {
			items.put(key, xPathFactory.newXPath().compile(paths.get(key)));
		}
	}
}
