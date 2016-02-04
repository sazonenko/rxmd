package ru.thprom.mrp.md.process;

import rx.Observable;
import rx.Producer;
import rx.Subscriber;
import rx.internal.operators.BackpressureUtils;

import java.util.concurrent.atomic.AtomicLong;

public final class RxRange
		implements Observable.OnSubscribe<Integer> {
	final int start;
	final int count;
	public RxRange(int start, int count) {
		if (count < 0) {
			throw new IllegalArgumentException();
		}
		this.start = start;
		this.count = count;
	}
	@Override
	public void call(Subscriber<? super Integer> subscriber) {
		if (count == 0) {
			subscriber.onCompleted();
			return;
		}
		RangeProducer p = new RangeProducer(subscriber, start, count);
		subscriber.setProducer(p);
	}

	public Observable<Integer> toObservable() {
		return Observable.create(this);
	}

	static final class RangeProducer
			extends AtomicLong implements Producer {
		/** */
		private static final long serialVersionUID =
				5318571951669533517L;
		final Subscriber<? super Integer> child;
		int index;
		int remaining;
		public RangeProducer(
				Subscriber<? super Integer> child,
				int start, int count) {
			this.child = child;
			this.index = start;
			this.remaining = count;
		}
		@Override
		public void request(long n) {
			System.out.println("requested: "+ n);
			if (n < 0) {
				throw new IllegalArgumentException();
			}
			if (n == 0) {
				return;
			}
			if (BackpressureUtils.getAndAddRequest(this, n) != 0) {
				return;
			}
			long r = n;
			for (;;) {
				if (child.isUnsubscribed()) {
					return;
				}
				int i = index;
				int k = remaining;
				int e = 0;

				while (r > 0 && k > 0) {
					child.onNext(i);
					if (child.isUnsubscribed()) {
						return;
					}
					k--;
					if (k == 0) {
						child.onCompleted();
						return;
					}
					e++;
					i++;
					r--;
				}
				index = i;
				remaining = k;

				r = addAndGet(-e);

				if (r == 0) {
					return;
				}
			}
		}
	}

	public static void main(String[] args) {
		Observable<Integer> range =
				new RxRange(1, 10).toObservable();

		//range.take(5).subscribe(
		range.subscribe(
				System.out::println,
				Throwable::printStackTrace,
				() -> System.out.println("Done")
		);
	}
}