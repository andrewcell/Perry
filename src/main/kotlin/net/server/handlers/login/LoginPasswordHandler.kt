package net.server.handlers.login

import client.Client
import constants.ServerConstants
import mu.KLoggable
import net.PacketHandler
import net.server.Server
import tools.CoroutineManager
import tools.PacketCreator
import tools.ServerJSON
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.InteractPacket
import tools.packet.LoginPacket

class LoginPasswordHandler : PacketHandler, KLoggable {
    override val logger = logger()

    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        /*
        if(!c.getClient()) {
            c.getSession().write(MessagePacket.serverNotice(1, "정식 클라이언트로 실행하여 주세요."));
            c.getSession().write(PacketCreator.getLoginFailed(20));
            return;
        } else {
            c.setConnector(false);
        }
        */
        /*if(!c.getConnector()) {
           c.getSession().write(MessagePacket.serverNotice(1, "접속기가 최신 버전이 아니거나 관리자가 서버 내 오류 해결중 입니다."));
           c.getSession().write(PacketCreator.getLoginFailed(20));
          return;
        }*/
        val emailValue = slea.readGameASCIIString()
        val email = if (emailValue[0] == '3') emailValue.substring(1) else emailValue
        val password = slea.readGameASCIIString()
        c.accountName = email
        if (ServerJSON.settings.autoRegister) {
            when (val checkId = AutoRegister.checkAccount(c, email, password)) {
                0, 1, 2, 6 -> {
                    val female = emailValue[0] == '3'
                    if (checkId == 0) AutoRegister.registerAccount(c.getSessionIPAddress(), email, password, female)
                    val message = when (checkId) {
                        0 -> "회원 가입이 완료되었습니다.\r\n주민등록번호는 1234567 입니다."
                        1 -> "해당하는 계정이 사이트에 없습니다. 먼저 회원가입을 해주시기 바랍니다."
                        2 -> "오류가 발생했습니다."
                        6 -> "아이피당 3개의 계정만 생성이 가능합니다."
                        else -> ""
                    }
                    c.session.write(InteractPacket.serverNotice(1, message))
                    c.session.write(LoginPacket.getLoginFailed(20))
                    c.session.write(PacketCreator.enableActions())
                    return
                }
                else -> {
                }
            }
        }
        val loginResult = c.login(email, password)
        if (c.hasBannedIp() || c.hasBannedMac()) c.announce(LoginPacket.getLoginFailed(3))
        val tempBan = c.getTempBanCalendar()
        if (tempBan != null) {
            if (tempBan.timeInMillis > System.currentTimeMillis()) {
                c.announce(LoginPacket.getTempBan(tempBan.timeInMillis, c.getGReason()))
                c.session.write(InteractPacket.serverNotice(1, "불법프로그램을 사용하여 제재된 계정입니다."))
                c.session.write(LoginPacket.getLoginFailed(20))
                return
            }
        }
        if (loginResult == 3) {
            c.announce(LoginPacket.getPermBan(c.getGReason()))
            return
        } else if (loginResult != 0) {
            c.announce(LoginPacket.getLoginFailed(loginResult))
            return
        }
        if (c.finishLogin() == 0) {
            c.announce(LoginPacket.getAuthSuccess(c))
            logger.info { "Client logged in to $email. IP: ${c.getSessionIPAddress()} " }
            Server.worlds.forEach {
                c.announce(LoginPacket.getServerList(it.id, ServerConstants.worldNames[it.id], it.flag, it.eventMessage, it.channels))
            }
            c.announce(LoginPacket.getEndOfServerList())
            c.idleTask = CoroutineManager.schedule({
                c.disconnect(shutdown = false, cashShop = false)
            }, 600000)
        } else {
            c.announce(LoginPacket.getLoginFailed(7))
        }
    }

    override fun validateState(c: Client) = !c.loggedIn
}