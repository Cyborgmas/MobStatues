package com.cyborgmas.mobstatues.network;

import com.cyborgmas.mobstatues.MobStatues;
import com.cyborgmas.mobstatues.client.ClientEntityReceiver;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class MSNetwork {
    private static SimpleChannel channel;

    public static void init() {
        channel = NetworkRegistry.ChannelBuilder.named(MobStatues.getId("channel"))
                .serverAcceptedVersions(s -> s.equals("1"))
                .clientAcceptedVersions(s -> s.equals("1"))
                .networkProtocolVersion(() -> "1")
                .simpleChannel();

        channel.messageBuilder(SpawnWrapperPacket.class, 0)
                .encoder(SpawnWrapperPacket::encode)
                .decoder(SpawnWrapperPacket::decode)
                .consumer(ClientEntityReceiver::handle)
                .add();
    }
}
