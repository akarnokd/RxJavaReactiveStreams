/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rx.internal.reactivestreams;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Subscription;

public final class RxJavaAtomicProducer extends AtomicInteger implements rx.Producer, rx.Subscription {
    /** */
    private static final long serialVersionUID = -8651615387948310062L;
    final Subscription subscription;
    final ConcurrentLinkedQueue<Long> requests;
    volatile boolean unsubscribed;
    public RxJavaAtomicProducer(Subscription subscription) {
        if (subscription == null) {
            throw new NullPointerException("subscription");
        }
        this.subscription = subscription;
        this.requests = new ConcurrentLinkedQueue<Long>();
    }
    @Override
    public void request(long n) {
        if (n > 0 && !unsubscribed) {
            requests.offer(n);
            if (getAndIncrement() == 0) {
                do {
                    Long v = requests.poll();
                    // v may be null if unsubscribe() cleared the queue while draining
                    if (v == null || v == 0L) {
                        unsubscribed = true;
                        subscription.cancel();
                        requests.clear();
                        return;
                    } else {
                        subscription.request(n);
                    }
                } while (decrementAndGet() > 0);
            }
        }
    }
    @Override
    public void unsubscribe() {
        if (!unsubscribed) {
            requests.clear();
            requests.offer(0L);
            if (getAndIncrement() == 0) {
                unsubscribed = true;
                subscription.cancel();
                requests.clear();
            }
        }
    }
    @Override
    public boolean isUnsubscribed() {
        return unsubscribed;
    }
}
