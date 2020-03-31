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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;

public class BroadcastPacket extends Packet {
    private ByteBuf byteBuf;

    public static BroadcastPacket from(Packet packet) {
        if (packet instanceof BroadcastPacket) {
            return (BroadcastPacket) packet;
        }
        BroadcastPacket broadcastPacket = new BroadcastPacket(packet.getType());
        broadcastPacket.setSubType(packet.getSubType());
        broadcastPacket.setName(packet.getName());
        broadcastPacket.setData(packet.getData());
        return broadcastPacket;
    }

    public BroadcastPacket(PacketType type) {
        super(type);
    }

    public ByteBuf getByteBuf(BroadcastPacketEncoder encoder, ChannelHandlerContext ctx) throws IOException {
        if (byteBuf != null) {
            return byteBuf.copy();
        }
        byteBuf = encoder.encode(this, ctx);
        return byteBuf.copy();
    }

    public ByteBuf getByteBuf() {
        return byteBuf;
    }

    public interface BroadcastPacketEncoder {
        ByteBuf encode(BroadcastPacket broadcastPacket, ChannelHandlerContext ctx) throws IOException;
    }
}
