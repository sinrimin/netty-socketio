/**
 * Copyright (c) 2012-2019 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio.jmh;

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.ack.AckManager;
import com.corundumstudio.socketio.handler.ClientHead;
import com.corundumstudio.socketio.handler.ClientsBox;
import com.corundumstudio.socketio.handler.EncoderHandler;
import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.protocol.JsonSupport;
import com.corundumstudio.socketio.protocol.PacketEncoder;
import com.corundumstudio.socketio.scheduler.CancelableScheduler;
import com.corundumstudio.socketio.scheduler.HashedWheelTimeoutScheduler;
import com.corundumstudio.socketio.store.MemoryStoreFactory;
import com.corundumstudio.socketio.store.StoreFactory;
import com.corundumstudio.socketio.transport.NamespaceClient;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class BroadcastTest {

    private EmbeddedChannel channel;
    private ClientHead client;
    List<SocketIOClient> clients;
    private BroadcastOperations broadcastOperations;

    @Test
    public void test01() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(BroadcastTest.class.getSimpleName())
                .warmupIterations(5)
                .forks(1)
                .measurementIterations(3)
                .result("/data/result.json")
                .resultFormat(ResultFormatType.JSON)
                .build();
        new Runner(opt).run();
    }

    class ChatMessage {
        private String message;

        public ChatMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    @Benchmark
    public int write() {
        ChatMessage chatMessage = new ChatMessage(new Random().nextInt() + " go!");
        broadcastOperations.sendEvent("chat", chatMessage);
        return 1;
    }

    @Benchmark
    public int write2() {
        ChatMessage chatMessage = new ChatMessage(new Random().nextInt() + " go!");
        for (SocketIOClient client : clients) {
            client.sendEvent("chat", chatMessage);
        }
        return 1;
    }

    @Setup
    public void prepare() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, IOException {
        Class<?> jjs = getClass().getClassLoader().loadClass("com.corundumstudio.socketio.protocol.JacksonJsonSupport");
        JsonSupport js = (JsonSupport) jjs.getConstructor().newInstance();
        Configuration configuration = new Configuration();
        PacketEncoder encoder = new PacketEncoder(configuration, js);

        StoreFactory storeFactory = new MemoryStoreFactory();
        CancelableScheduler scheduler = new HashedWheelTimeoutScheduler();
        EncoderHandler encoderHandler = new EncoderHandler(configuration, encoder);
        channel = new EmbeddedChannel(encoderHandler);
        DisconnectableHub disconnectableHub = new DisconnectableHub() {
            @Override
            public void onDisconnect(ClientHead client) {

            }
        };
        Namespace namespace = new Namespace("", configuration);

        clients = new ArrayList<SocketIOClient>();
        for (int i = 0; i < 500; i++) {
            HandshakeData handshakeData = new HandshakeData(new DefaultHttpHeaders(), new HashMap<String, List<String>>(), new InetSocketAddress(2132), new InetSocketAddress(2132), "", false);
            client = new ClientHead(UUID.randomUUID(), new AckManager(scheduler), disconnectableHub, storeFactory, handshakeData, new ClientsBox(), Transport.WEBSOCKET, scheduler, configuration);
            client.bindChannel(channel, Transport.WEBSOCKET);
            NamespaceClient nc = new NamespaceClient(client, namespace);
            clients.add(nc);
        }
        broadcastOperations = new BroadcastOperations(clients, storeFactory);
    }

}
