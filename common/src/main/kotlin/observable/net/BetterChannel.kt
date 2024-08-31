package observable.net

import dev.architectury.networking.NetworkManager
import dev.architectury.networking.NetworkManager.Side
import dev.architectury.networking.transformers.SplitPacketTransformer
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.ProtoBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import org.apache.logging.log4j.LogManager

@OptIn(ExperimentalSerializationApi::class)
class BetterChannel(val id: ResourceLocation) {
    companion object {
        val LOGGER = LogManager.getLogger("ObservableNet")
    }

    val s2cLocation: ResourceLocation = id.withSuffix("-s2c")
    val c2sLocation: ResourceLocation = id.withSuffix("-c2s")

    class SerializedPayload(val className: String, val data: ByteArray, val location: ResourceLocation) : CustomPacketPayload {
        override fun type() = CustomPacketPayload.Type<CustomPacketPayload>(location)
    }
    inline fun <reified T> createPayload(data: T, side: Side) = SerializedPayload(T::class.java.name, ProtoBuf.encodeToByteArray(data), if (side == Side.S2C) s2cLocation else c2sLocation)

    val handlers = mutableMapOf<String, (ByteArray, NetworkManager.PacketContext) -> Unit>()

    init {
        val codec = object : StreamCodec<RegistryFriendlyByteBuf, SerializedPayload> {
            override fun decode(buf: RegistryFriendlyByteBuf): SerializedPayload {
                return SerializedPayload(buf.readUtf(), buf.readByteArray(), id)
            }

            override fun encode(buf: RegistryFriendlyByteBuf, payload: SerializedPayload) {
                buf.writeUtf(payload.className)
                buf.writeByteArray(payload.data)
            }
        }
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, CustomPacketPayload.Type(s2cLocation), codec, listOf(SplitPacketTransformer())) { value, ctx ->
            handlers[value.className]?.invoke(value.data, ctx)
        }
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CustomPacketPayload.Type(c2sLocation), codec) { value, ctx ->
            handlers[value.className]?.invoke(value.data, ctx)
        }
    }

    inline fun <reified T> register(
        noinline consumer: (T, NetworkManager.PacketContext) -> Unit
    ) {
        handlers[T::class.java.name] = { buf, ctx -> consumer(ProtoBuf.decodeFromByteArray(buf), ctx) }
        LOGGER.info("Registered ${T::class.java}")
    }

    inline fun <reified T> sendToPlayers(players: List<ServerPlayer>, msg: T) = NetworkManager.sendToPlayers(players, createPayload(msg, Side.S2C))
    inline fun <reified T> sendToPlayer(player: ServerPlayer, msg: T) = NetworkManager.sendToPlayer(player, createPayload(msg, Side.S2C))
    inline fun <reified T> sendToPlayersSplit(players: List<ServerPlayer>, msg: T) = NetworkManager.sendToPlayers(players, createPayload(msg, Side.S2C))
    inline fun <reified T> sendToServer(msg: T) = NetworkManager.sendToServer(createPayload(msg, Side.C2S))
}
