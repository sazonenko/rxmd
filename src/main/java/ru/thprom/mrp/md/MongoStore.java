package ru.thprom.mrp.md;

import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import org.bson.Document;

import javax.annotation.PreDestroy;
import java.util.Date;
import java.util.Map;

/**
 * Created by void on 11.12.15
 */
public class MongoStore {

	public static final String INCOMING_XML = "incoming.xml";
	public static final String INCOMING_BIN = "incoming.bin";

	private MongoClient softClient;
	private MongoClient hardClient;
	private MongoDatabase dbs;
	private MongoDatabase dbh;
	private String host;
	private int port;
	private String databaseName;

	public void connect() {
		ServerAddress serverAddress = new ServerAddress(host, port);
		softClient = new MongoClient(serverAddress);
		dbs = softClient.getDatabase(databaseName);

		MongoClientOptions hardOptions = new MongoClientOptions.Builder()
				.writeConcern(WriteConcern.JOURNALED)
				.writeConcern(WriteConcern.W1)
				.build();
		hardClient = new MongoClient(serverAddress, hardOptions);
		dbh = hardClient.getDatabase(databaseName);
	}

	public void saveEvent(String filename, String path) {
		MongoCollection<Document> collection;
		if (filename.endsWith(".xml")) {
			collection = dbh.getCollection(INCOMING_XML);
		} else {
			collection = dbh.getCollection(INCOMING_BIN);
		}
		Document document = new Document("filename", filename)
				.append("path", path)
				.append("state", "income")
				.append("mTime", new Date());
		collection.insertOne(document);
	}

	public Map<String, Object> getIncomeEvent(String collection) {
		MongoCollection<Document> incoming = dbh.getCollection(collection);
		Document filter = new Document("state", "income");
		Document update = new Document("$set", new Document("state", "process").append("mTime", new Date()));

		return incoming.findOneAndUpdate(filter, update, new FindOneAndUpdateOptions().sort(new Document("_id", 1)));
	}

	public Map<String, Object> findAttachment(String fileName) {
		MongoCollection<Document> incoming = dbs.getCollection(INCOMING_BIN);
		FindIterable<Document> documents = incoming.find(new Document("filename", fileName));
		MongoCursor<Document> iterator = documents.iterator();
		if (iterator.hasNext()) {
			return iterator.next();
		}
		return null;
	}

	@PreDestroy
	public void close() {
		softClient.close();
		hardClient.close();
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}
}
