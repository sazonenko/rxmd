package ru.thprom.mrp.md.process;

import ru.thprom.mrp.md.MongoStore;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Created by void on 05.02.2016
 */
public class RxContext {

    private MongoStore mongoStore;
    private CompositeSubscription compositeSubscription = new CompositeSubscription();

    @PostConstruct
    public void start() {

        MongoQueue incomingXmlQueue = new MongoQueue(MongoStore.INCOMING_XML);
        incomingXmlQueue.setMongoStore(mongoStore);
        Subscription incomingXml = incomingXmlQueue.toObservable().subscribe(
                System.out::println,
                Throwable::printStackTrace,
                () -> System.out.println("Done")
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
}
