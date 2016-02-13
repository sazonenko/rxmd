package ru.thprom.mrp.md.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import ru.thprom.mrp.md.Constants;
import ru.thprom.mrp.md.FileStore;
import ru.thprom.mrp.md.MdException;
import ru.thprom.mrp.md.MongoStore;

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
import java.util.Map;

/**
 * Created by void on 08.02.16
 */
public class MessageProcessor {
	private static final Logger log = LoggerFactory.getLogger(MessageProcessor.class);

	private FileStore fileStore;
	private MongoStore mongoStore;
	private ThreadLocal<DocumentBuilder> localBuilder = new ThreadLocal<>();
	private static XPathFactory xPathFactory = XPathFactory.newInstance();

	private enum XPaths {
		ATTACH_NAME("/*/Info/Attach/Name"),
		MESSAGE_GUID("/*/Info/GUID"),
		ORIGIN_CODE("/*/Info/Sender/@Code"),
		DESTINATION_CODE("/*/Info/Recipient/@Code"),
		MESSAGE_DATE("/*/Info/Sender/@Date");

		private final String text;
		private final XPathExpression expression;

		XPaths(final String text) throws XPathExpressionException {
			this.text = text;
			expression = xPathFactory.newXPath().compile(text);
		}

		@Override
		public String toString() {
			return text;
		}

		public XPathExpression getExpression() {
			return expression;
		}

		public String eval(Document doc, String fileName) {
			try {
				return (String) expression.evaluate(doc, XPathConstants.STRING);
			} catch (XPathExpressionException e) {
				log.error("error in parsing XML ["+ fileName+"] : "+ e);
				return null;
			}
		}
	};

	public void processXml(Map<String, Object> m) {
		DocumentBuilder docBuilder = getDocumentBuilder();

		String fileName = (String) m.get(Constants.HEADER_CAMEL_FILE_NAME);
		try {
			File storeDir = new File(fileStore.getStoreRoot(), (String) m.get(Constants.HEADER_ATTACHMENT_PATH));
			File file = new File(storeDir, fileName);

			Document doc;
			try (InputStream stream = new FileInputStream(file)){
				doc = docBuilder.parse(stream);
			}
			String value = XPaths.MESSAGE_GUID.eval(doc, fileName); //FixMe: loop?
			if (null != value) {
				m.put("guid", value);
			}
			value = XPaths.ORIGIN_CODE.eval(doc, fileName);
			if (null != value) {
				m.put("origin", value);
			}
			value = XPaths.DESTINATION_CODE.eval(doc, fileName);
			if (null != value) {
				m.put("destination", value);
			}

			value = XPaths.ATTACH_NAME.eval(doc, fileName);
			if (null != value) {
				m.put("attach", value);
				mongoStore.findAttachment(value);
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

	public void setMongoStore(MongoStore mongoStore) {
		this.mongoStore = mongoStore;
	}
}
