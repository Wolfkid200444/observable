package observable.net

import ProfilingData
import kotlinx.serialization.Serializable
import java.util.*

class S2CPacket {
    @Serializable
    data class ProfilingStarted(val endNanos: Long)

    @Serializable
    data class ProfilingResult(val data: ProfilingData)
}