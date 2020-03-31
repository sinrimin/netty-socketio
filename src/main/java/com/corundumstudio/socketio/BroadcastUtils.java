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
package com.corundumstudio.socketio;

import com.corundumstudio.socketio.protocol.BroadcastPacket;
import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.protocol.PacketType;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class BroadcastUtils {

    private static final Logger log = LoggerFactory.getLogger(BroadcastUtils.class);

    public static void send(Iterable<SocketIOClient> clients, Packet packet) {
        BroadcastPacket broadcastPacket = BroadcastPacket.from(packet);
        for (SocketIOClient client : clients) {
            client.send(broadcastPacket);
        }

        release(broadcastPacket.getByteBuf());
    }

    public static Packet sendEvent(Iterable<SocketIOClient> clients, String name, SocketIOClient excludedClient, Object... data) {
        BroadcastPacket packet = new BroadcastPacket(PacketType.MESSAGE);
        packet.setSubType(PacketType.EVENT);
        packet.setName(name);
        packet.setData(Arrays.asList(data));

        for (SocketIOClient client : clients) {
            if (excludedClient != null && client.getSessionId().equals(excludedClient.getSessionId())) {
                continue;
            }
            client.send(packet);
        }

        release(packet.getByteBuf());
        return packet;
    }

    public static <T> void sendEvent(Iterable<SocketIOClient> clients, String name, Object data, SocketIOClient excludedClient, BroadcastAckCallback<T> ackCallback) {
        BroadcastPacket packet = new BroadcastPacket(PacketType.MESSAGE);
        packet.setSubType(PacketType.EVENT);
        packet.setName(name);
        packet.setData(Arrays.asList(data));
        for (SocketIOClient client : clients) {
            if (excludedClient != null && client.getSessionId().equals(excludedClient.getSessionId())) {
                continue;
            }
            client.send(packet, ackCallback.createClientCallback(client));
        }
        ackCallback.loopFinished();

        release(packet.getByteBuf());
    }

    private static void release(ByteBuf ref) {
        if (ref != null) {
            synchronized (ref) {
                ref.release();
            }
            if (ref.refCnt() > 0) {
                log.warn("LEAK>>>{}", ref.refCnt());
            }
        }
    }
}