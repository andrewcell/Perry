package tools.packet

import client.Character
import client.Client
import net.SendPacketOpcode
import net.server.channel.handlers.PlayerInteractionHandler
import server.MiniGame
import server.events.gm.Snowball
import tools.PacketCreator
import tools.data.output.PacketLittleEndianWriter

class MiniGamePacket {
    companion object {
        fun addOmokBox(c: Character, locker: Int, ammount: Int, type: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.UPDATE_CHAR_BOX.value)
            lew.int(c.id)
            PacketCreator.addAnnounceBox(lew, c.miniGame, 1, locker, 0, ammount, type)
            return lew.getPacket()
        }

        fun coconutScore(team1: Int, team2: Int): ByteArray {
            val lew = PacketLittleEndianWriter(6)
            lew.byte(SendPacketOpcode.COCONUT_SCORE.value)
            lew.short(team1)
            lew.short(team2)
            return lew.getPacket()
        }

        fun getMatchCard(c: Client?, minigame: MiniGame, owner: Boolean, piece: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.ROOM.code)
            lew.byte(2)
            lew.byte(2)
            lew.byte(if (owner) 0 else 1)
            lew.byte(0)
            CharacterPacket.addCharLook(lew, minigame.owner, false)
            lew.gameASCIIString(minigame.owner.name)
            if (minigame.visitor != null) {
                val visitor = minigame.visitor ?: return ByteArray(0)
                lew.byte(1)
                CharacterPacket.addCharLook(lew, visitor, false)
                lew.gameASCIIString(visitor.name)
            }
            lew.byte(0xFF)
            lew.byte(0)
            lew.int(2)
            lew.int(minigame.owner.getMiniGamePoints("wins", false))
            lew.int(minigame.owner.getMiniGamePoints("ties", false))
            lew.int(minigame.owner.getMiniGamePoints("losses", false))
            lew.int(
                2000 + minigame.owner.getMiniGamePoints(
                    "wins",
                    false
                ) - minigame.owner.getMiniGamePoints("losses", false)
            )
            if (minigame.visitor != null) {
                val visitor = minigame.visitor ?: return ByteArray(0)
                lew.byte(1)
                lew.int(2)
                lew.int(visitor.getMiniGamePoints("wins", false))
                lew.int(visitor.getMiniGamePoints("ties", false))
                lew.int(visitor.getMiniGamePoints("losses", false))
                lew.int(
                    2000 + visitor.getMiniGamePoints("wins", false) - visitor.getMiniGamePoints(
                        "losses",
                        false
                    )
                )
            }
            lew.byte(0xFF)
            lew.gameASCIIString(minigame.description)
            lew.byte(piece)
            lew.byte(0)
            return lew.getPacket()
        }

        fun addMatchCardBox(c: Character, locker: Int, ammount: Int, type: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.UPDATE_CHAR_BOX.value)
            lew.int(c.id)
            PacketCreator.addAnnounceBox(lew, c.miniGame, 2, locker, 0, ammount, type)
            return lew.getPacket()
        }


        fun getMatchCardNewVisitor(c: Character, slot: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.VISIT.code)
            lew.byte(slot)
            CharacterPacket.addCharLook(lew, c, false)
            lew.gameASCIIString(c.name)
            lew.int(1)
            lew.int(c.getMiniGamePoints("wins", false))
            lew.int(c.getMiniGamePoints("ties", false))
            lew.int(c.getMiniGamePoints("losses", false))
            lew.int(2000 + c.getMiniGamePoints("wins", false) - c.getMiniGamePoints("losses", false))
            return lew.getPacket()
        }

        fun getMatchCardSelect(game: MiniGame, turn: Int, slot: Int, firstSlot: Int, type: Int): ByteArray {
            val lew = PacketLittleEndianWriter(6)
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.SELECT_CARD.code)
            lew.byte(turn)
            if (turn == 1) {
                lew.byte(slot)
            } else if (turn == 0) {
                lew.byte(slot)
                lew.byte(firstSlot)
                lew.byte(type)
            }
            return lew.getPacket()
        }

        fun getMatchCardStart(game: MiniGame, loser: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.START.code)
            lew.byte(loser)
            lew.byte(0x0C)
            var last = 13
            if (game.matchesToWin > 10) {
                last = 31
            } else if (game.matchesToWin > 6) {
                last = 21
            }
            for (i in 1 until last) {
                lew.int(game.getCardId(i))
            }
            return lew.getPacket()
        }

        fun getMatchCardOwnerWin(game: MiniGame): ByteArray {
            return getMiniGameResult(game, 1, 0, 0, 1, 0, false)
        }

        fun getMatchCardVisitorWin(game: MiniGame): ByteArray {
            return getMiniGameResult(game, 0, 1, 0, 2, 0, false)
        }

        fun getMatchCardTie(game: MiniGame): ByteArray {
            return getMiniGameResult(game, 0, 0, 1, 3, 0, false)
        }

        fun getMiniGame(c: Client?, miniGame: MiniGame, owner: Boolean, piece: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.ROOM.code)
            lew.byte(1)
            lew.byte(0)
            lew.byte(if (owner) 0 else 1)
            lew.byte(0)
            CharacterPacket.addCharLook(lew, miniGame.owner, false)
            lew.gameASCIIString(miniGame.owner.name)
            if (miniGame.visitor != null) {
                val visitor = miniGame.visitor ?: return ByteArray(0)
                lew.byte(1)
                CharacterPacket.addCharLook(lew, visitor, false)
                lew.gameASCIIString(visitor.name)
            }
            lew.byte(0xFF)
            lew.byte(0)
            lew.int(1)
            lew.int(miniGame.owner.getMiniGamePoints("wins", true))
            lew.int(miniGame.owner.getMiniGamePoints("ties", true))
            lew.int(miniGame.owner.getMiniGamePoints("losses", true))
            lew.int(
                2000 + miniGame.owner.getMiniGamePoints(
                    "wins",
                    true
                ) - miniGame.owner.getMiniGamePoints("losses", true)
            )
            if (miniGame.visitor != null) {
                val visitor = miniGame.visitor
                lew.byte(1)
                lew.int(1)
                lew.int(visitor!!.getMiniGamePoints("wins", true))
                lew.int(visitor.getMiniGamePoints("ties", true))
                lew.int(visitor.getMiniGamePoints("losses", true))
                lew.int(
                    2000 + visitor.getMiniGamePoints("wins", true) - visitor.getMiniGamePoints(
                        "losses",
                        true
                    )
                )
            }
            lew.byte(0xFF)
            lew.gameASCIIString(miniGame.description)
            lew.byte(piece)
            lew.byte(0)
            return lew.getPacket()
        }

        fun getMiniGameClose(close: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter(5)
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.EXIT.code)
            lew.byte(1)
            lew.byte(if (close) 3 else 5)
            return lew.getPacket()
        }

        fun getMiniGameFull(): ByteArray {
            val lew = PacketLittleEndianWriter(5)
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.ROOM.code)
            lew.byte(0)
            lew.byte(2)
            return lew.getPacket()
        }

        fun addMiniGameInfo(lew: PacketLittleEndianWriter, chr: Character) {
            lew.short(0)
        }

        fun getMiniGameNewVisitor(c: Character, slot: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.VISIT.code)
            lew.byte(slot)
            CharacterPacket.addCharLook(lew, c, false)
            lew.gameASCIIString(c.name)
            lew.int(1)
            lew.int(c.getMiniGamePoints("wins", true))
            lew.int(c.getMiniGamePoints("ties", true))
            lew.int(c.getMiniGamePoints("losses", true))
            lew.int(2000 + c.getMiniGamePoints("wins", true) - c.getMiniGamePoints("losses", true))
            return lew.getPacket()
        }

        fun getMiniGameMoveOmok(game: MiniGame, move1: Int, move2: Int, move3: Int): ByteArray {
            val lew = PacketLittleEndianWriter(12)
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.MOVE_OMOK.code)
            lew.int(move1)
            lew.int(move2)
            lew.byte(move3)
            return lew.getPacket()
        }

        fun getMiniGameOwnerForfeit(game: MiniGame): ByteArray {
            return getMiniGameResult(game, 0, 1, 0, 2, 1, game.gameType == "omok")
        }

        fun getMiniGameOwnerWin(game: MiniGame): ByteArray {
            return getMiniGameResult(game, 1, 0, 0, 1, 0, game.gameType == "omok")
        }

        fun getMiniGameReady(): ByteArray {
            val lew = PacketLittleEndianWriter(3)
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.READY.code)
            return lew.getPacket()
        }

        fun getMiniGameRemoveVisitor(): ByteArray {
            val lew = PacketLittleEndianWriter(3)
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.EXIT.code)
            lew.byte(1)
            return lew.getPacket()
        }

        fun getMiniGameRequestTie(): ByteArray {
            val lew = PacketLittleEndianWriter(3)
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.REQUEST_TIE.code)
            return lew.getPacket()
        }

        private fun getMiniGameResult(
            game: MiniGame,
            win: Int,
            lose: Int,
            tie: Int,
            result: Int,
            forfeit: Int,
            omok: Boolean
        ): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.GET_RESULT.code)
            if (tie == 0 && forfeit != 1) {
                lew.byte(0)
            } else if (tie == 1) {
                lew.byte(1)
            } else if (forfeit == 1) {
                lew.byte(2)
            }
            if (tie != 1) {
                lew.byte(result - 1) // owner
            }
            lew.int(1) // unknown
            val ownerWin = game.owner.getMiniGamePoints("wins", omok) + win
            val ownerTie = game.owner.getMiniGamePoints("ties", omok) + tie
            val ownerLose = game.owner.getMiniGamePoints("losses", omok) + lose
            val ownerPoint = 2000 + ownerWin - ownerLose
            lew.int(ownerWin)
            lew.int(ownerTie)
            lew.int(ownerLose)
            lew.int(ownerPoint)
            lew.int(1) // start of visitor; unknown
            val visitor = game.visitor ?: return ByteArray(0)
            val visitorWin = visitor.getMiniGamePoints("wins", omok) + lose
            val visitorTie = visitor.getMiniGamePoints("ties", omok) + tie
            val visitorLose = visitor.getMiniGamePoints("losses", omok) + win
            val visitorPoint = 2000 + visitorWin - visitorLose
            lew.int(visitorWin)
            lew.int(visitorTie)
            lew.int(visitorLose)
            lew.int(visitorPoint)
            game.owner.setMiniGamePoints(visitor, result, omok)
            return lew.getPacket()
        }

        fun getMiniGameStart(loser: Int): ByteArray {
            val lew = PacketLittleEndianWriter(4)
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.START.code)
            lew.byte(loser)
            return lew.getPacket()
        }

        fun getMiniGameSkipOwner(): ByteArray {
            val lew = PacketLittleEndianWriter(4)
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.SKIP.code)
            lew.byte(0x01)
            return lew.getPacket()
        }

        fun getMiniGameSkipVisitor(): ByteArray {
            val lew = PacketLittleEndianWriter(4)
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.short(PlayerInteractionHandler.Action.SKIP.code)
            return lew.getPacket()
        }

        fun getMiniGameTie(game: MiniGame): ByteArray {
            return getMiniGameResult(game, 0, 0, 1, 3, 0, game.gameType == "omok")
        }

        fun getMiniGameDenyTie(game: MiniGame): ByteArray {
            val lew = PacketLittleEndianWriter(3)
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.ANSWER_TIE.code)
            return lew.getPacket()
        }

        fun getMiniGameUnReady(): ByteArray {
            val lew = PacketLittleEndianWriter(3)
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.UN_READY.code)
            return lew.getPacket()
        }

        fun getMiniGameVisitorForfeit(game: MiniGame): ByteArray {
            return getMiniGameResult(game, 1, 0, 0, 1, 1, game.gameType == "omok")
        }

        fun getMiniGameVisitorWin(game: MiniGame): ByteArray {
            return getMiniGameResult(game, 0, 1, 0, 2, 0, game.gameType == "omok")
        }

        fun hitCoconut(spawn: Boolean, id: Int, type: Int): ByteArray {
            val lew = PacketLittleEndianWriter(7)
            lew.byte(SendPacketOpcode.COCONUT_HIT.value)
            if (spawn) {
                lew.short(-1)
                lew.short(5000)
                lew.byte(0)
            } else {
                lew.short(id)
                lew.short(1000) //delay till you can attack again!
                lew.byte(type) // What action to do for the coconut.
            }
            return lew.getPacket()
        }

        fun hitSnowBall(what: Int, damage: Int): ByteArray {
            val lew = PacketLittleEndianWriter(7)
            lew.byte(SendPacketOpcode.HIT_SNOWBALL.value)
            lew.byte(what)
            lew.int(damage)
            return lew.getPacket()
        }

        fun removeOmokBox(c: Character): ByteArray {
            val lew = PacketLittleEndianWriter(7)
            lew.byte(SendPacketOpcode.UPDATE_CHAR_BOX.value)
            lew.int(c.id)
            lew.byte(0)
            return lew.getPacket()
        }

        fun removeMatchCardBox(c: Character): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.UPDATE_CHAR_BOX.value)
            lew.int(c.id)
            lew.byte(0)
            return lew.getPacket()
        }

        fun rollSnowBall(enterMap: Boolean, state: Int, ball0: Snowball?, ball1: Snowball?): ByteArray {
            if (ball0 == null || ball1 == null) return ByteArray(0)
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SNOWBALL_STATE.value)
            if (enterMap) {
                lew.skip(21)
            } else {
                lew.byte(state) // 0 = move, 1 = roll, 2 is down disappear, 3 is up disappear
                lew.int(ball0.snowmanHp / 75)
                lew.int(ball1.snowmanHp / 75)
                lew.short(ball0.position) //distance snowball down, 84 03 = max
                lew.byte(-1)
                lew.short(ball1.position) //distance snowball up, 84 03 = max
                lew.byte(-1)
            }
            return lew.getPacket()
        }

        fun showOXQuiz(questionSet: Int, questionId: Int, askQuestion: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter(6)
            lew.byte(SendPacketOpcode.OX_QUIZ.value)
            lew.byte(if (askQuestion) 1 else 0)
            lew.byte(questionSet)
            lew.short(questionId)
            return lew.getPacket()
        }

        /**
         * Sends a Snowball Message<br></br>
         *
         *
         * Possible values for
         * `message`:<br></br> 1: ... Team's snowball has passed the stage
         * 1.<br></br> 2: ... Team's snowball has passed the stage 2.<br></br> 3: ... Team's
         * snowball has passed the stage 3.<br></br> 4: ... Team is attacking the
         * snowman, stopping the progress<br></br> 5: ... Team is moving again<br></br>
         *
         * @param message
         */
        fun snowballMessage(team: Int, message: Int): ByteArray {
            val lew = PacketLittleEndianWriter(7)
            lew.byte(SendPacketOpcode.SNOWBALL_MESSAGE.value)
            lew.byte(team) // 0 is down, 1 is up
            lew.int(message)
            return lew.getPacket()
        }
    }
}