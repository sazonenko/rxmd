package ru.thprom.mrp.md.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.thprom.mrp.md.MongoStore;
import rx.Observable;
import rx.Producer;
import rx.Subscriber;
import rx.internal.operators.BackpressureUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by void on 03.02.16
 */
public final class RxMongo implements Observable.OnSubscribe<Map> {
	private final Logger log = LoggerFactory.getLogger(getClass());

	private AtomicLong requested = new AtomicLong(0);
	private MongoStore mongoStore;
	private String collection;

	private TimeUnit checkDelayTimeUnit = TimeUnit.MILLISECONDS;
	private long checkDelay = 1000;

	public RxMongo(String collection) {
		this.collection = collection;
	}

	@Override
	public void call(Subscriber<? super Map> subscriber) {
		MongoProducer producer = new MongoProducer(subscriber);
		subscriber.setProducer(producer);
	}

	public Observable<Map> toObservable() {
		return Observable.create(this);
	}

	public void setMongoStore(MongoStore mongoStore) {
		this.mongoStore = mongoStore;
	}

	public void setCheckDelayTimeUnit(TimeUnit checkDelayTimeUnit) {
		this.checkDelayTimeUnit = checkDelayTimeUnit;
	}

	public void setCheckDelay(long checkDelay) {
		this.checkDelay = checkDelay;
	}

	class MongoProducer implements Producer {

		private final Subscriber<? super Map> child;

		public MongoProducer(Subscriber<? super Map> child) {
			this.child = child;
		}

		@Override
		public void request(long n) {
			log.debug("requested {}", n);
			if (n <= 0) {
				return;
			}
			// increment with overflow check
			if (BackpressureUtils.getAndAddRequest(requested, n) != 0) {
				return;   //Thread safety// ? только 1 поток генерит значения ?
			}

			long batchSize = n;
			do {
				long emitted = 0;
				while (batchSize > 0) {
					Map<String, Object> value = null;
					while (null == value) {
						if (child.isUnsubscribed()) {
							return;
						}
						value = mongoStore.getIncomeEvent(collection);
						if (null == value) {
							try {
								checkDelayTimeUnit.sleep(checkDelay);
							} catch (InterruptedException e) {
								child.onError(e);
								return;
							}
						}
					}
					child.onNext(value);
					if (child.isUnsubscribed()) {
						return;
					}
					batchSize--;
					emitted++;
				}

				batchSize = requested.getAndAdd(-emitted);
			} while (batchSize > 0);

		}
	}
}
