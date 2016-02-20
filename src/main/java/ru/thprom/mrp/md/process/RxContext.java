package ru.thprom.mrp.md.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.thprom.mrp.md.Constants;
import ru.thprom.mrp.md.MongoStore;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;

/**
 * Created by void on 05.02.2016
 */
public class RxContext {
	private final Logger log = LoggerFactory.getLogger(getClass());

    private MongoStore mongoStore;
    private CompositeSubscription compositeSubscription = new CompositeSubscription();
	private XmlMessageParser parser;

    @PostConstruct
    public void start() {

        MongoQueue incomingXmlQueue = new MongoQueue(MongoStore.INCOMING_XML);
        incomingXmlQueue.setMongoStore(mongoStore);
		Observable<Map> observable = incomingXmlQueue.toObservable();
		Subscription incomingXml = observable
				.doOnNext(m -> parser.processXml(m))
				.subscribe(
						m -> log.info("Package_Process_End;{}", ((Map)m).get(Constants.HEADER_XML_FILE_NAME)),
						e -> ((Exception)e).printStackTrace(),
						() -> log.info("Shutdown")
        );
		compositeSubscription.add(incomingXml);
    }

    @PreDestroy
    public void stop() {
        compositeSubscription.unsubscribe();
    }

    public void setMongoStore(MongoStore mongoStore) {
        this.mongoStore = mongoStore;
    }

	public void setParser(XmlMessageParser parser) {
		this.parser = parser;
	}
}
