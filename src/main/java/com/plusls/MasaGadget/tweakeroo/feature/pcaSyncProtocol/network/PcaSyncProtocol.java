package com.plusls.MasaGadget.tweakeroo.feature.pcaSyncProtocol.network;

import com.plusls.MasaGadget.MasaGadgetMod;
import io.netty.buffer.Unpooled;
import net.earthcomputer.multiconnect.api.ICustomPayloadEvent;
import net.earthcomputer.multiconnect.api.ICustomPayloadListener;
import net.earthcomputer.multiconnect.api.MultiConnectAPI;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PcaSyncProtocol {
    private static final String NAMESPACE = "pca";

    private static Identifier id(String path) {
        return new Identifier(NAMESPACE, path);
    }

    // send
    private static final Identifier SYNC_BLOCK_ENTITY = id("sync_block_entity");
    private static final Identifier SYNC_ENTITY = id("sync_entity");
    private static final Identifier CANCEL_SYNC_REQUEST_BLOCK_ENTITY = id("cancel_sync_block_entity");
    private static final Identifier CANCEL_SYNC_ENTITY = id("cancel_sync_entity");

    // recv
    private static final Identifier ENABLE_PCA_SYNC_PROTOCOL = id("enable_pca_sync_protocol");
    private static final Identifier DISABLE_PCA_SYNC_PROTOCOL = id("disable_pca_sync_protocol");
    private static final Identifier UPDATE_ENTITY = id("update_entity");
    private static final Identifier UPDATE_BLOCK_ENTITY = id("update_block_entity");

    private static BlockPos lastBlockPos = null;
    private static int lastEntityId = -1;
    public static boolean enable = false;

    private static final ClientboundIdentifierCustomPayloadListener clientboundIdentifierCustomPayloadListener =
            new ClientboundIdentifierCustomPayloadListener();
    private static final ServerboundIdentifierCustomPayloadListener serverboundIdentifierCustomPayloadListener =
            new ServerboundIdentifierCustomPayloadListener();


    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(ENABLE_PCA_SYNC_PROTOCOL, PcaSyncProtocol::enablePcaSyncProtocolHandle);
        ClientPlayNetworking.registerGlobalReceiver(DISABLE_PCA_SYNC_PROTOCOL, PcaSyncProtocol::disablePcaSyncProtocolHandle);
        ClientPlayNetworking.registerGlobalReceiver(UPDATE_ENTITY, PcaSyncProtocol::updateEntityHandler);
        ClientPlayNetworking.registerGlobalReceiver(UPDATE_BLOCK_ENTITY, PcaSyncProtocol::updateBlockEntityHandler);
        ClientPlayConnectionEvents.DISCONNECT.register(PcaSyncProtocol::onDisconnect);
        MultiConnectAPI.instance().addClientboundIdentifierCustomPayloadListener(clientboundIdentifierCustomPayloadListener);
        MultiConnectAPI.instance().addServerboundIdentifierCustomPayloadListener(serverboundIdentifierCustomPayloadListener);
    }

    private static class ServerboundIdentifierCustomPayloadListener implements ICustomPayloadListener<Identifier> {
        @Override
        public void onCustomPayload(ICustomPayloadEvent<Identifier> event) {
            Identifier channel = event.getChannel();
            if (channel.equals(SYNC_BLOCK_ENTITY)) {
                MultiConnectAPI.instance().forceSendCustomPayload(event.getNetworkHandler(), event.getChannel(), event.getData());
            } else if (channel.equals(SYNC_ENTITY)) {
                MultiConnectAPI.instance().forceSendCustomPayload(event.getNetworkHandler(), event.getChannel(), event.getData());
            } else if (channel.equals(CANCEL_SYNC_REQUEST_BLOCK_ENTITY)) {
                MultiConnectAPI.instance().forceSendCustomPayload(event.getNetworkHandler(), event.getChannel(), event.getData());
            } else if (channel.equals(CANCEL_SYNC_ENTITY)) {
                MultiConnectAPI.instance().forceSendCustomPayload(event.getNetworkHandler(), event.getChannel(), event.getData());
            }
        }
    }

    private static class ClientboundIdentifierCustomPayloadListener implements ICustomPayloadListener<Identifier> {
        @Override
        public void onCustomPayload(ICustomPayloadEvent<Identifier> event) {
            Identifier channel = event.getChannel();
            if (channel.equals(ENABLE_PCA_SYNC_PROTOCOL)) {
                enablePcaSyncProtocolHandle(MinecraftClient.getInstance(), null, event.getData(), null);
            } else if (channel.equals(DISABLE_PCA_SYNC_PROTOCOL)) {
                disablePcaSyncProtocolHandle(MinecraftClient.getInstance(), null, event.getData(), null);
            } else if (channel.equals(UPDATE_ENTITY)) {
                updateEntityHandler(MinecraftClient.getInstance(), null, event.getData(), null);
            } else if (channel.equals(UPDATE_BLOCK_ENTITY)) {
                updateBlockEntityHandler(MinecraftClient.getInstance(), null, event.getData(), null);
            }
        }
    }


    private static void onDisconnect(ClientPlayNetworkHandler clientPlayNetworkHandler, MinecraftClient minecraftClient) {
        MasaGadgetMod.LOGGER.info("pcaSyncProtocol disable.");
        enable = false;
    }

    private static void enablePcaSyncProtocolHandle(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        if (!client.isIntegratedServerRunning()) {
            MasaGadgetMod.LOGGER.info("pcaSyncProtocol enable.");
            enable = true;
        }
    }

    private static void disablePcaSyncProtocolHandle(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        if (!client.isIntegratedServerRunning()) {
            MasaGadgetMod.LOGGER.info("pcaSyncProtocol disable.");
            enable = false;
        }
    }

    // 反序列化实体数据
    private static void updateEntityHandler(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        World world = player.world;
        if (!world.getRegistryKey().getValue().equals(buf.readIdentifier())) {
            return;
        }
        int entityId = buf.readInt();
        CompoundTag tag = buf.readCompoundTag();
        Entity entity = world.getEntityById(entityId);

        if (entity != null) {
            MasaGadgetMod.LOGGER.debug("update entity!");
            Vec3d localPos = entity.getPos();
            double prevX = entity.prevX;
            double prevY = entity.prevY;
            double prevZ = entity.prevZ;
            float pitch = entity.pitch;
            float horizontalSpeed = entity.horizontalSpeed;
            float yaw = entity.yaw;
            float prevPitch = entity.prevPitch;
            float prevHorizontalSpeed = entity.prevHorizontalSpeed;
            float prevYaw = entity.prevYaw;
            Vec3d velocity = entity.getVelocity();

            entity.fromTag(tag);

            entity.prevX = prevX;
            entity.prevY = prevY;
            entity.prevZ = prevZ;
            entity.setPos(localPos.x, localPos.y, localPos.z);
            entity.prevPitch = prevPitch;
            entity.prevHorizontalSpeed = prevHorizontalSpeed;
            entity.prevYaw = prevYaw;
            entity.pitch = pitch;
            entity.horizontalSpeed = horizontalSpeed;
            entity.yaw = yaw;
            entity.setVelocity(velocity);
        }
    }

    // 反序列化 blockEntity 数据
    private static void updateBlockEntityHandler(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        World world = player.world;
        if (!world.getRegistryKey().getValue().equals(buf.readIdentifier())) {
            return;
        }
        BlockPos pos = buf.readBlockPos();
        CompoundTag tag = buf.readCompoundTag();
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null) {
            MasaGadgetMod.LOGGER.debug("update blockEntity!");
            blockEntity.fromTag(world.getBlockState(pos), tag);
        }
    }


    static public void syncBlockEntity(BlockPos pos) {
        if (lastBlockPos != null && lastBlockPos.equals(pos)) {
            return;
        }
        MasaGadgetMod.LOGGER.debug("syncBlockEntity: {}", pos);
        lastBlockPos = pos;
        lastEntityId = -1;
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBlockPos(pos);
        ClientPlayNetworking.send(SYNC_BLOCK_ENTITY, buf);
    }

    static public void syncEntity(int entityId) {
        if (lastEntityId != -1 && lastEntityId == entityId) {
            return;
        }
        MasaGadgetMod.LOGGER.debug("syncEntity: {}", entityId);
        lastEntityId = entityId;
        lastBlockPos = null;
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(entityId);
        ClientPlayNetworking.send(SYNC_ENTITY, buf);
    }

    static public void cancelSyncBlockEntity() {
        if (lastBlockPos == null) {
            return;
        }
        lastBlockPos = null;
        MasaGadgetMod.LOGGER.debug("cancelSyncBlockEntity.");
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        ClientPlayNetworking.send(CANCEL_SYNC_REQUEST_BLOCK_ENTITY, buf);
    }

    static public void cancelSyncEntity() {
        if (lastEntityId == -1) {
            return;
        }
        lastEntityId = -1;
        MasaGadgetMod.LOGGER.debug("cancelSyncEntity.");
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        ClientPlayNetworking.send(CANCEL_SYNC_ENTITY, buf);
    }
}
