package observable.forge

import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import observable.client.Overlay

object ForgeClientHooks {
    @SubscribeEvent
    fun onRender(ev: RenderLevelStageEvent) {
        if (ev.stage == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            Overlay.render(ev.poseStack, ev.partialTick.gameTimeDeltaTicks, ev.projectionMatrix)
        }
    }
}
