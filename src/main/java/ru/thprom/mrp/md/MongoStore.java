package ru.thprom.mrp.md;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;

import java.util.Date;
import java.util.Map;

/**
 * Created by void on 11.12.15
 */
public class MongoStore {

	public static final String COLLECTION_INCOMING = "incoming";

	private MongoClient mongoClient;
	private MongoDatabase database;
	private String connectionURI;
	private String databaseName;

	public void connect() {
		mongoClient = new MongoClient(new MongoClientURI(connectionURI));
		database = mongoClient.getDatabase(databaseName);
	}

	public void saveEvent(String filename, String path) {
		MongoCollection<Document> incoming = database.getCollection(COLLECTION_INCOMING);
		Document document = new Document("filename", filename)
				.append("path", path)
				.append("state", "income")
				.append("mTime", new Date());
		incoming.insertOne(document);
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
