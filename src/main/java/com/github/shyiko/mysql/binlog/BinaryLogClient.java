/*
 * Copyright 2013 Stanley Shyiko
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
package com.github.shyiko.mysql.binlog;

import com.github.shyiko.mysql.binlog.event.Event;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MySQL replication stream client.
 *
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class BinaryLogClient extends AbstractBinaryLogClient {

    private final List<EventListener> eventListeners = new LinkedList<EventListener>();
    private final List<LifecycleListener> lifecycleListeners = new LinkedList<LifecycleListener>();
    private ThreadFactory threadFactory;
    private final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * Alias for BinaryLogClient("localhost", 3306, &lt;no schema&gt; = null, username, password).
     * @see BinaryLogClient#BinaryLogClient(String, int, String, String, String)
     */
    public BinaryLogClient(String username, String password) {
        this("localhost", 3306, null, username, password);
    }

    /**
     * Alias for BinaryLogClient("localhost", 3306, schema, username, password).
     * @see BinaryLogClient#BinaryLogClient(String, int, String, String, String)
     */
    public BinaryLogClient(String schema, String username, String password) {
        this("localhost", 3306, schema, username, password);
    }

    /**
     * Alias for BinaryLogClient(hostname, port, &lt;no schema&gt; = null, username, password).
     * @see BinaryLogClient#BinaryLogClient(String, int, String, String, String)
     */
    public BinaryLogClient(String hostname, int port, String username, String password) {
        this(hostname, port, null, username, password);
    }

    /**
     * @param hostname mysql server hostname
     * @param port mysql server port
     * @param schema database name, nullable. Note that this parameter has nothing to do with event filtering. It's
     * used only during the authentication.
     * @param username login name
     * @param password password
     */
    public BinaryLogClient(String hostname, int port, String schema, String username, String password) {
        super(username, port, hostname, password, schema);
    }

    /**
     * @param threadFactory custom thread factory to use for "connect in separate thread". If not provided, thread
     * will be created using simple "new Thread()".
     */
    public void setThreadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    /**
     * Connect to the replication stream in a separate thread.
     * @param timeoutInMilliseconds timeout in milliseconds
     * @throws com.github.shyiko.mysql.binlog.network.AuthenticationException in case of failed authentication
     * @throws java.io.IOException if anything goes wrong while trying to connect
     * @throws java.util.concurrent.TimeoutException if client wasn't able to connect in the requested period of time
     */
    public void connect(long timeoutInMilliseconds) throws IOException, TimeoutException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        BinaryLogClient.AbstractLifecycleListener connectListener = new BinaryLogClient.AbstractLifecycleListener() {
            @Override
            public void onConnect(BinaryLogClient client) {
                countDownLatch.countDown();
            }
        };
        registerLifecycleListener(connectListener);
        final AtomicReference<IOException> exceptionReference = new AtomicReference<IOException>();
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                try {
                    connect();
                } catch (IOException e) {
                    exceptionReference.set(e);
                    countDownLatch.countDown(); // making sure we don't end up waiting whole "timeout"
                }
            }
        };
        Thread thread = threadFactory == null ? new Thread(runnable) : threadFactory.newThread(runnable);
        thread.start();
        boolean started = false;
        try {
            started = countDownLatch.await(timeoutInMilliseconds, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, e.getMessage());
            }
        }
        unregisterLifecycleListener(connectListener);
        if (exceptionReference.get() != null) {
            throw exceptionReference.get();
        }
        if (!started) {
            throw new TimeoutException("BinaryLogClient was unable to connect in " + timeoutInMilliseconds + "ms");
        }
    }

    /**
     * @return registered event listeners
     */
    public List<EventListener> getEventListeners() {
        return Collections.unmodifiableList(eventListeners);
    }

    /**
     * Register event listener. Note that multiple event listeners will be called in order they
     * where registered.
     */
    public void registerEventListener(EventListener eventListener) {
        synchronized (eventListeners) {
            eventListeners.add(eventListener);
        }
    }

    /**
     * Unregister all event listener of specific type.
     */
    public void unregisterEventListener(Class<? extends EventListener> listenerClass) {
        synchronized (eventListeners) {
            Iterator<EventListener> iterator = eventListeners.iterator();
            while (iterator.hasNext()) {
                EventListener eventListener = iterator.next();
                if (listenerClass.isInstance(eventListener)) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Unregister single event listener.
     */
    public void unregisterEventListener(EventListener eventListener) {
        synchronized (eventListeners) {
            eventListeners.remove(eventListener);
        }
    }

    @Override
    protected void onEvent(Event event) {
        synchronized (eventListeners) {
            for (EventListener eventListener : eventListeners) {
                try {
                    eventListener.onEvent(event);
                } catch (Exception e) {
                    if (logger.isLoggable(Level.WARNING)) {
                        logger.log(Level.WARNING, eventListener + " choked on " + event, e);
                    }
                }
            }
        }
    }

    /**
     * @return registered lifecycle listeners
     */
    public List<LifecycleListener> getLifecycleListeners() {
        return Collections.unmodifiableList(lifecycleListeners);
    }

    /**
     * Register lifecycle listener. Note that multiple lifecycle listeners will be called in order they
     * where registered.
     */
    public void registerLifecycleListener(LifecycleListener lifecycleListener) {
        synchronized (lifecycleListeners) {
            lifecycleListeners.add(lifecycleListener);
        }
    }

    /**
     * Unregister all lifecycle listener of specific type.
     */
    public synchronized void unregisterLifecycleListener(Class<? extends LifecycleListener> listenerClass) {
        synchronized (lifecycleListeners) {
            Iterator<LifecycleListener> iterator = lifecycleListeners.iterator();
            while (iterator.hasNext()) {
                LifecycleListener lifecycleListener = iterator.next();
                if (listenerClass.isInstance(lifecycleListener)) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Unregister single lifecycle listener.
     */
    public synchronized void unregisterLifecycleListener(LifecycleListener eventListener) {
        synchronized (lifecycleListeners) {
            lifecycleListeners.remove(eventListener);
        }
    }

    @Override
    protected void onConnect() {
        synchronized (lifecycleListeners) {
            for (LifecycleListener lifecycleListener : lifecycleListeners) {
                lifecycleListener.onConnect(this);
            }
        }
    }

    @Override
    protected void onCommunicationFailure(Exception ex) {
        synchronized (lifecycleListeners) {
            for (LifecycleListener lifecycleListener : lifecycleListeners) {
                lifecycleListener.onCommunicationFailure(this, ex);
            }
        }

    }

    @Override
    protected void onEventDeserializationFailure(Exception ex) {
        synchronized (lifecycleListeners) {
            for (LifecycleListener lifecycleListener : lifecycleListeners) {
                lifecycleListener.onEventDeserializationFailure(this, ex);
            }
        }

    }

    @Override
    protected void onDisconnect() {
        synchronized (lifecycleListeners) {
            for (LifecycleListener lifecycleListener : lifecycleListeners) {
                lifecycleListener.onDisconnect(this);
            }
        }
    }

    /**
     * {@link BinaryLogClient}'s event listener.
     */
    public interface EventListener {

        void onEvent(Event event);
    }

    /**
     * {@link BinaryLogClient}'s lifecycle listener.
     */
    public interface LifecycleListener {

        /**
         * Called once client has successfully logged in but before started to receive binlog events.
         * @param client
         */
        void onConnect(BinaryLogClient client);

        /**
         * It's guarantied to be called before {@link #onDisconnect(BinaryLogClient)}) in case of
         * communication failure.
         */
        void onCommunicationFailure(BinaryLogClient client, Exception ex);

        /**
         * Called in case of failed event deserialization. Note this type of error does NOT cause client to
         * disconnect. If you wish to stop receiving events you'll need to fire client.disconnect() manually.
         */
        void onEventDeserializationFailure(BinaryLogClient client, Exception ex);

        /**
         * Called upon disconnect (regardless of the reason).
         * @param client
         */
        void onDisconnect(BinaryLogClient client);
    }

    /**
     * Default (no-op) implementation of {@link LifecycleListener}.
     */
    public static abstract class AbstractLifecycleListener implements LifecycleListener {

        @Override
        public void onConnect(BinaryLogClient client) {
        }

        @Override
        public void onCommunicationFailure(BinaryLogClient client, Exception ex) {
        }

        @Override
        public void onEventDeserializationFailure(BinaryLogClient client, Exception ex) {
        }

        @Override
        public void onDisconnect(BinaryLogClient client) {
        }

    }

    /**
     * A {@link LifecycleListener} that rebroadcasts events to a dynamic list of children.
     */
    public static class BroadcastLifecycleListener implements LifecycleListener {
        private final List<LifecycleListener> lifecycleListeners = new LinkedList<LifecycleListener>();

        @Override
        public void onConnect(BinaryLogClient client) {
            synchronized (lifecycleListeners) {
                for (LifecycleListener listener : lifecycleListeners) {
                    listener.onConnect(client);
                }
            }
        }

        @Override
        public void onCommunicationFailure(BinaryLogClient client, Exception ex) {
            synchronized (lifecycleListeners) {
                for (LifecycleListener listener : lifecycleListeners) {
                    listener.onCommunicationFailure(client, ex);
                }
            }
        }

        @Override
        public void onEventDeserializationFailure(BinaryLogClient client, Exception ex) {
            synchronized (lifecycleListeners) {
                for (LifecycleListener listener : lifecycleListeners) {
                    listener.onEventDeserializationFailure(client, ex);
                }
            }
        }

        @Override
        public void onDisconnect(BinaryLogClient client) {
            synchronized (lifecycleListeners) {
                for (LifecycleListener listener : lifecycleListeners) {
                    listener.onDisconnect(client);
                }
            }
        }

        /**
         * @return registered lifecycle listeners
         */
        public List<LifecycleListener> getLifecycleListeners() {
            return Collections.unmodifiableList(lifecycleListeners);
        }

        /**
         * Register lifecycle listener. Note that multiple lifecycle listeners will be called in order they
         * where registered.
         */
        public void registerLifecycleListener(LifecycleListener listener) {
            synchronized (lifecycleListeners) {
                lifecycleListeners.add(listener);
            }
        }

        /**
         * Unregister all lifecycle listener of specific type.
         */
        public synchronized void unregisterLifecycleListener(Class<? extends LifecycleListener> listenerClass) {
            synchronized (lifecycleListeners) {
                Iterator<LifecycleListener> iterator = lifecycleListeners.iterator();
                while (iterator.hasNext()) {
                    LifecycleListener lifecycleListener = iterator.next();
                    if (listenerClass.isInstance(lifecycleListener)) {
                        iterator.remove();
                    }
                }
            }
        }

        /**
         * Unregister single lifecycle listener.
         */
        public synchronized void unregisterLifecycleListener(LifecycleListener listener) {
            synchronized (lifecycleListeners) {
                lifecycleListeners.remove(listener);
            }
        }
    }

}
