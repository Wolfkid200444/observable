package observable.fabric

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import observable.Observable
import observable.client.Overlay

class Client : ClientModInitializer {
    override fun onInitializeClient() {
        Observable.clientInit()

        WorldRenderEvents.END.register {
            val stack = it.matrixStack() ?: return@register
            val projection = it.projectionMatrix()
            Overlay.render(stack, it.tickCounter().getGameTimeDeltaPartialTick(true), projection)
        }
    }
}
