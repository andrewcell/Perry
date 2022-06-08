package server.events.gm

import client.Character
import provider.DataProviderFactory
import provider.DataTool
import server.maps.GameMap
import tools.CoroutineManager
import tools.ServerJSON.settings
import tools.packet.InteractPacket
import tools.packet.MiniGamePacket
import java.io.File
import kotlin.random.Random

class OxQuiz(val map: GameMap) {
    private var round = Random.nextInt(9)
    private var question = 1
    private var expGain = 200

    private fun isCorrectAnswer(chr: Character, answer: Int): Boolean {
        val x = chr.position.x
        val y = chr.position.y

        if ((x > -234 && y > -26 && answer == 0) || (x < -234 && y > -26 && answer == 1)) {
            chr.dropMessage(message = "Correct!")
            return true
        }
        return false
    }

    fun sendQuestion() {
        var gm = 0
        for (mc in map.characters) {
            if (mc.gmLevel > 0) gm++
        }
        val number = gm
        map.broadcastMessage(MiniGamePacket.showOXQuiz(round, question, true))
        CoroutineManager.schedule(Runnable {
            map.broadcastMessage(MiniGamePacket.showOXQuiz(round, question, true))
            for (chr in map.characters) {
                if (!isCorrectAnswer(chr, getOXAnswer(round, question)) && !chr.isGM()) {
                    chr.changeMap(chr.map.returnMapId)
                } else {
                    chr.gainExp(expGain, show = true, inChat = true)
                }
            }
            if ((round == 1 && question == 29) || ((round == 2 || round == 3) && question == 17) || ((round == 4 || round == 8) && question == 12) || (round == 5 && question == 26) || (round == 9 && question == 44) || ((round == 6 || round == 7) && question == 16)) {
                question = 100
            } else {
                question++
            }
            //send question
            if (map.characters.size - number <= 1) {
                map.broadcastMessage(InteractPacket.serverNotice(6, "The event has ended"))
                map.getPortal("join00")?.portalStatus = true
                map.ox = null
                map.isOxQuiz = false
                //prizes here
                return@Runnable
            }
            sendQuestion()
        }, 30000)  // 30 seconds.
    }

    companion object {
        private val stringData =
            DataProviderFactory.getDataProvider(File("${settings.wzPath}/Etc.wz"))

        private fun getOXAnswer(imgDir: Int, id: Int) = DataTool.getInt(
            stringData.getData("OXQuiz.img")?.getChildByPath("" + imgDir + "")
                ?.getChildByPath("" + id + "")?.getChildByPath("a")
        )
    }
}