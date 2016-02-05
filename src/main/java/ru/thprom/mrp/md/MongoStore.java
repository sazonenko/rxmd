package ru.thprom.mrp.md;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import org.bson.Document;

import java.util.Date;
import java.util.Map;

/**
 * Created by void on 11.12.15
 */
public class MongoStore {

	public static final String INCOMING_XML = "incoming.xml";
	public static final String INCOMING_BIN = "incoming.bin";

	private MongoClient mongoClient;
	private MongoDatabase database;
	private String connectionURI;
	private String databaseName;

	public void connect() {
		mongoClient = new MongoClient(new MongoClientURI(connectionURI));
		database = mongoClient.getDatabase(databaseName);
	}

	public void saveEvent(String filename, String path) {
		MongoCollection<Document> collection;
		if (filename.endsWith(".xml")) {
			collection = database.getCollection(INCOMING_XML);
		} else {
			collection = database.getCollection(INCOMING_BIN);
		}
		Document document = new Document("filename", filename)
				.append("path", path)
				.append("state", "income")
				.append("mTime", new Date());
		collection.insertOne(document);
	}

	public Map<String, Object> getIncomeEvent(String collection) {
		MongoCollection<Document> incoming = database.getCollection(collection);
		Document filter = new Document("state", "income");
		Document update = new Document("$set", new Document("state", "process").append("mTime", new Date()));

		return incoming.findOneAndUpdate(filter, update, new FindOneAndUpdateOptions().sort(new Document("_id", 1)));
	}

	public void close() {
		mongoClient.close();
	}

	public void setConnectionURI(String connectionURI) {
		this.connectionURI = connectionURI;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}
}
