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
package com.corundumstudio.socketio.protocol;

import com.corundumstudio.socketio.SocketIOClient;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

public class BroadcastPacket extends Packet {
    private ByteBuf byteBuf;
    private int refCnt = 0;

    private static final Logger log = LoggerFactory.getLogger(BroadcastPacket.class);

    public static BroadcastPacket from(Packet packet, Iterable<SocketIOClient> clients) {
        if (packet instanceof BroadcastPacket) {
            return (BroadcastPacket) packet;
        }
        BroadcastPacket broadcastPacket = new BroadcastPacket(packet.getType(), clients);
        broadcastPacket.setSubType(packet.getSubType());
        broadcastPacket.setName(packet.getName());
        broadcastPacket.setData(packet.getData());
        return broadcastPacket;
    }

    public BroadcastPacket(PacketType type, Iterable<SocketIOClient> clients) {
        super(type);
        this.refCnt = count(clients);
    }

    public BroadcastPacket(PacketType type, int refCnt) {
        super(type);
        this.refCnt = refCnt;
    }

    public ByteBuf getByteBuf(BroadcastPacketEncoder encoder, ChannelHandlerContext ctx) throws IOException {
        if (byteBuf != null) {
            byteBuf.resetReaderIndex();
            return byteBuf;
        }
        byteBuf = encoder.encode(this, ctx);
        if (refCnt > 1) {
            byteBuf.retain(refCnt - 1).markReaderIndex();
        }
        return byteBuf;
    }

    public interface BroadcastPacketEncoder {
        ByteBuf encode(BroadcastPacket broadcastPacket, ChannelHandlerContext ctx) throws IOException;
    }

    private int count(Iterable<SocketIOClient> clients) {
        if (clients == null) {
            return 0;
        }
        if (clients instanceof Collection) {
            return ((Collection) clients).size();
        }
        int count = 0;
        for (SocketIOClient client : clients) {
            count++;
        }
        return count;
    }
}
