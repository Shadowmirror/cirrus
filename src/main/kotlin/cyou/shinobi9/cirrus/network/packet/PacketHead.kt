package cyou.shinobi9.cirrus.network.packet

data class PacketHead(
    val version: Version,
    val code: Operation,
    val seq: Int = 1
) {
    var packLength: Int = 0
    var headLength: Short = 16
}
