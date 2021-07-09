package cyou.shinobi9.cirrus.handler.message

import cyou.shinobi9.cirrus.network.packet.CMD.*
import cyou.shinobi9.cirrus.network.packet.searchCMD
import kotlinx.serialization.json.*
import java.lang.RuntimeException

interface SimpleMessageHandler : MessageHandler {
    fun onReceiveDanmu(block: (user: String, said: String) -> Unit)
    fun onReceiveGift(block: (user: String, num: Int, giftName: String) -> Unit)
    fun onVipEnterInLiveRoom(block: (user: String) -> Unit)
    fun onGuardEnterInLiveRoom(block: (user: String) -> Unit)
    fun onAllTypeMessage(block: (message: String) -> Unit)
    fun onUnknownTypeMessage(block: (message: String) -> Unit)
    fun onError(block: (message: String, e: MessageException) -> Unit)
}

class SimpleMessageHandlerImpl(
    private var receiveDanmu: ((user: String, said: String) -> Unit)? = null,
    private var receiveGift: ((user: String, num: Int, giftName: String) -> Unit)? = null,
    private var vipEnterInLiveRoom: ((user: String) -> Unit)? = null,
    private var guardEnterInLiveRoom: ((user: String) -> Unit)? = null,
    private var allTypeMessage: ((message: String) -> Unit)? = null,
    private var unknownTypeMessage: ((message: String) -> Unit)? = null,
    private var error: ((message: String, e: MessageException) -> Unit)? = null
) : SimpleMessageHandler {
    override fun onReceiveDanmu(block: (user: String, said: String) -> Unit) {
        receiveDanmu = block
    }

    override fun onReceiveGift(block: (user: String, num: Int, giftName: String) -> Unit) {
        receiveGift = block
    }

    override fun onVipEnterInLiveRoom(block: (user: String) -> Unit) {
        vipEnterInLiveRoom = block
    }

    override fun onGuardEnterInLiveRoom(block: (user: String) -> Unit) {
        guardEnterInLiveRoom = block
    }

    override fun onAllTypeMessage(block: (message: String) -> Unit) {
        allTypeMessage = block
    }

    override fun onUnknownTypeMessage(block: (message: String) -> Unit) {
        unknownTypeMessage = block
    }

    override fun onError(block: (message: String, e: MessageException) -> Unit) {
        error = block
    }

    override fun handle(message: String) {
        try {
            allTypeMessage?.invoke(message)
            val json = Json.parseToJsonElement(message)
            val cmd = json.jsonObject["cmd"]?.jsonPrimitive?.content
                ?: throw Exception("unexpect json format, missing [cmd] !")
            when (searchCMD(cmd)) {
                DANMU_MSG -> {
                    val said = json.jsonObject["info"]!!.jsonArray[1].jsonPrimitive.content
                    val user = json.jsonObject["info"]!!.jsonArray[2].jsonArray[1].jsonPrimitive.content
                    receiveDanmu?.invoke(user, said)
                }
                SEND_GIFT -> {
                    val user = json.jsonObject["data"]!!.jsonObject["uname"]!!.jsonPrimitive.content
                    val num = json.jsonObject["data"]!!.jsonObject["num"]!!.jsonPrimitive.int
                    val giftName = json.jsonObject["data"]!!.jsonObject["giftName"]!!.jsonPrimitive.content
                    receiveGift?.invoke(user, num, giftName)
                }
                WELCOME -> {
                    val user = json.jsonObject["data"]!!.jsonObject["uname"]!!.jsonPrimitive.content
                    vipEnterInLiveRoom?.invoke(user)
                }
                WELCOME_GUARD -> {
                    val user = json.jsonObject["data"]!!.jsonObject["username"]!!.jsonPrimitive.content
                    guardEnterInLiveRoom?.invoke(user)
                }
                UNKNOWN -> {
                    unknownTypeMessage?.invoke(message)
                }
                else -> {
                }
            }
        } catch (e: Throwable) {
            error?.invoke(message, MessageException("catch an exception while handling a message : $message", e))
        }
    }
}

class MessageException(message: String, e: Throwable) : RuntimeException(message, e)

inline fun simpleMessageHandler(content: SimpleMessageHandler.() -> Unit) = SimpleMessageHandlerImpl().apply(content)
