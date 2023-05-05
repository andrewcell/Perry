package tools.packet

import client.Character
import client.Client
import net.SendPacketOpcode
import net.server.channel.handlers.PlayerInteractionHandler
import server.MiniGame
import server.events.gm.Snowball
import tools.PacketCreator
import tools.PacketCreator.Companion.packetWriter
import tools.data.output.PacketLittleEndianWriter

class MiniGamePacket {
    companion object {
        fun addOmokBox(c: Character, locker: Int, ammount: Int, type: Int) = packetWriter(SendPacketOpcode.UPDATE_CHAR_BOX) {
            int(c.id)
            PacketCreator.addAnnounceBox(c.miniGame, 1, locker, 0, ammount, type)
        }

        fun coconutScore(team1: Int, team2: Int) = packetWriter(SendPacketOpcode.COCONUT_SCORE, 6) {
            short(team1)
            short(team2)
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

        fun addMatchCardBox(c: Character, locker: Int, ammount: Int, type: Int) = packetWriter(SendPacketOpcode.UPDATE_CHAR_BOX) {
            int(c.id)
            PacketCreator.addAnnounceBox(c.miniGame, 2, locker, 0, ammount, type)
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

        fun getMatchCardSelect(game: MiniGame, turn: Int, slot: Int, firstSlot: Int, type: Int) = packetWriter(SendPacketOpcode.PLAYER_INTERACTION, 6) {
            byte(PlayerInteractionHandler.Action.SELECT_CARD.code)
            byte(turn)
            if (turn == 1) {
                byte(slot)
            } else if (turn == 0) {
                byte(slot)
                byte(firstSlot)
                byte(type)
            }
        }

        fun getMatchCardStart(game: MiniGame, loser: Int) = packetWriter(SendPacketOpcode.PLAYER_INTERACTION) {
            byte(PlayerInteractionHandler.Action.START.code)
            byte(loser)
            byte(0x0C)
            var last = 13
            if (game.matchesToWin > 10) {
                last = 31
            } else if (game.matchesToWin > 6) {
                last = 21
            }
            for (i in 1 until last) {
                int(game.getCardId(i))
            }
        }

        fun getMatchCardOwnerWin(game: MiniGame) = getMiniGameResult(game, 1, 0, 0, 1, 0, false)

        fun getMatchCardVisitorWin(game: MiniGame) = getMiniGameResult(game, 0, 1, 0, 2, 0, false)

        fun getMatchCardTie(game: MiniGame) = getMiniGameResult(game, 0, 0, 1, 3, 0, false)

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

        fun getMiniGameClose(close: Boolean) = packetWriter(SendPacketOpcode.PLAYER_INTERACTION, 5) {
            byte(PlayerInteractionHandler.Action.EXIT.code)
            byte(1)
            byte(if (close) 3 else 5)
        }

        fun getMiniGameFull() = packetWriter(SendPacketOpcode.PLAYER_INTERACTION, 5) {
            byte(PlayerInteractionHandler.Action.ROOM.code)
            byte(0)
            byte(2)
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

        fun getMiniGameMoveOmok(game: MiniGame, move1: Int, move2: Int, move3: Int) = packetWriter(SendPacketOpcode.PLAYER_INTERACTION, 12) {
            byte(PlayerInteractionHandler.Action.MOVE_OMOK.code)
            int(move1)
            int(move2)
            byte(move3)
        }

        fun getMiniGameOwnerForfeit(game: MiniGame) = getMiniGameResult(game, 0, 1, 0, 2, 1, game.gameType == "omok")

        fun getMiniGameOwnerWin(game: MiniGame) = getMiniGameResult(game, 1, 0, 0, 1, 0, game.gameType == "omok")

        fun getMiniGameReady() = packetWriter(SendPacketOpcode.PLAYER_INTERACTION, 3) {
            byte(PlayerInteractionHandler.Action.READY.code)
        }

        fun getMiniGameRemoveVisitor() = packetWriter(SendPacketOpcode.PLAYER_INTERACTION, 3) {
            byte(PlayerInteractionHandler.Action.EXIT.code)
            byte(1)
        }

        fun getMiniGameRequestTie() = packetWriter(SendPacketOpcode.PLAYER_INTERACTION, 3) {
            byte(PlayerInteractionHandler.Action.REQUEST_TIE.code)
        }

        private fun getMiniGameResult(
            game: MiniGame,
            win: Int,
            lose: Int,
            tie: Int,
            result: Int,
            forfeit: Int,
            omok: Boolean
        ) = packetWriter(SendPacketOpcode.PLAYER_INTERACTION) {
            byte(PlayerInteractionHandler.Action.GET_RESULT.code)
            if (tie == 0 && forfeit != 1) {
                byte(0)
            } else if (tie == 1) {
                byte(1)
            } else if (forfeit == 1) {
                byte(2)
            }
            if (tie != 1) {
                byte(result - 1) // owner
            }
            int(1) // unknown
            val ownerWin = game.owner.getMiniGamePoints("wins", omok) + win
            val ownerTie = game.owner.getMiniGamePoints("ties", omok) + tie
            val ownerLose = game.owner.getMiniGamePoints("losses", omok) + lose
            val ownerPoint = 2000 + ownerWin - ownerLose
            int(ownerWin)
            int(ownerTie)
            int(ownerLose)
            int(ownerPoint)
            int(1) // start of visitor; unknown
            val visitor = game.visitor ?: return@packetWriter
            val visitorWin = visitor.getMiniGamePoints("wins", omok) + lose
            val visitorTie = visitor.getMiniGamePoints("ties", omok) + tie
            val visitorLose = visitor.getMiniGamePoints("losses", omok) + win
            val visitorPoint = 2000 + visitorWin - visitorLose
            int(visitorWin)
            int(visitorTie)
            int(visitorLose)
            int(visitorPoint)
            game.owner.setMiniGamePoints(visitor, result, omok)
        }

        fun getMiniGameStart(loser: Int) = packetWriter(SendPacketOpcode.PLAYER_INTERACTION, 4) {
            byte(PlayerInteractionHandler.Action.START.code)
            byte(loser)
        }

        fun getMiniGameSkipOwner() = packetWriter(SendPacketOpcode.PLAYER_INTERACTION, 4) {
            byte(PlayerInteractionHandler.Action.SKIP.code)
            byte(0x01)
        }

        fun getMiniGameSkipVisitor() = packetWriter(SendPacketOpcode.PLAYER_INTERACTION, 4) {
            short(PlayerInteractionHandler.Action.SKIP.code)
        }

        fun getMiniGameTie(game: MiniGame) = getMiniGameResult(game, 0, 0, 1, 3, 0, game.gameType == "omok")

        fun getMiniGameDenyTie(game: MiniGame) = packetWriter(SendPacketOpcode.PLAYER_INTERACTION, 3) {
            byte(PlayerInteractionHandler.Action.ANSWER_TIE.code)
        }

        fun getMiniGameUnReady() = packetWriter(SendPacketOpcode.PLAYER_INTERACTION, 3) {
            byte(PlayerInteractionHandler.Action.UN_READY.code)
        }

        fun getMiniGameVisitorForfeit(game: MiniGame) = getMiniGameResult(game, 1, 0, 0, 1, 1, game.gameType == "omok")

        fun getMiniGameVisitorWin(game: MiniGame) = getMiniGameResult(game, 0, 1, 0, 2, 0, game.gameType == "omok")

        fun hitCoconut(spawn: Boolean, id: Int, type: Int) = packetWriter(SendPacketOpcode.COCONUT_HIT, 7) {
            if (spawn) {
                short(-1)
                short(5000)
                byte(0)
            } else {
                short(id)
                short(1000) //delay till you can attack again!
                byte(type) // What action to do for the coconut.
            }
        }

        fun hitSnowBall(what: Int, damage: Int) = packetWriter(SendPacketOpcode.HIT_SNOWBALL, 7) {
            byte(what)
            int(damage)
        }

        fun removeOmokBox(c: Character) = packetWriter(SendPacketOpcode.UPDATE_CHAR_BOX, 7) {
            int(c.id)
            byte(0)
        }

        fun removeMatchCardBox(c: Character) = packetWriter(SendPacketOpcode.UPDATE_CHAR_BOX) {
            int(c.id)
            byte(0)
        }

        fun rollSnowBall(enterMap: Boolean, state: Int, ball0: Snowball?, ball1: Snowball?) = packetWriter(SendPacketOpcode.SNOWBALL_STATE) {
            if (ball0 == null || ball1 == null) return@packetWriter
            byte(SendPacketOpcode.SNOWBALL_STATE.value)
            if (enterMap) {
                skip(21)
            } else {
                byte(state) // 0 = move, 1 = roll, 2 is down disappear, 3 is up disappear
                int(ball0.snowmanHp / 75)
                int(ball1.snowmanHp / 75)
                short(ball0.position) //distance snowball down, 84 03 = max
                byte(-1)
                short(ball1.position) //distance snowball up, 84 03 = max
                byte(-1)
            }
        }

        fun showOXQuiz(questionSet: Int, questionId: Int, askQuestion: Boolean) = packetWriter(SendPacketOpcode.OX_QUIZ, 6) {
            byte(if (askQuestion) 1 else 0)
            byte(questionSet)
            short(questionId)
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
         * @param team 0 is down, 1 is up
         * @param message
         */
        fun snowballMessage(team: Int, message: Int) = packetWriter(SendPacketOpcode.SNOWBALL_MESSAGE, 7) {
            byte(team) // 0 is down, 1 is up
            int(message)
        }
    }
}