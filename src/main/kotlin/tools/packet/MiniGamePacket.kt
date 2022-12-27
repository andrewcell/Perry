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

        fun getMatchCard(c: Client?, minigame: MiniGame, owner: Boolean, piece: Int) = packetWriter(SendPacketOpcode.PLAYER_INTERACTION) {
            byte(PlayerInteractionHandler.Action.ROOM.code)
            byte(2)
            byte(2)
            byte(if (owner) 0 else 1)
            byte(0)
            CharacterPacket.addCharLook(minigame.owner, false)
            gameASCIIString(minigame.owner.name)
            if (minigame.visitor != null) {
                val visitor = minigame.visitor ?: return@packetWriter
                byte(1)
                CharacterPacket.addCharLook(visitor, false)
                gameASCIIString(visitor.name)
            }
            byte(0xFF)
            byte(0)
            int(2)
            int(minigame.owner.getMiniGamePoints("wins", false))
            int(minigame.owner.getMiniGamePoints("ties", false))
            int(minigame.owner.getMiniGamePoints("losses", false))
            int(
                2000 + minigame.owner.getMiniGamePoints(
                    "wins",
                    false
                ) - minigame.owner.getMiniGamePoints("losses", false)
            )
            if (minigame.visitor != null) {
                val visitor = minigame.visitor ?: return@packetWriter// ByteArray(0)
                byte(1)
                int(2)
                int(visitor.getMiniGamePoints("wins", false))
                int(visitor.getMiniGamePoints("ties", false))
                int(visitor.getMiniGamePoints("losses", false))
                int(
                    2000 + visitor.getMiniGamePoints("wins", false) - visitor.getMiniGamePoints(
                        "losses",
                        false
                    )
                )
            }
            byte(0xFF)
            gameASCIIString(minigame.description)
            byte(piece)
            byte(0)
        }

        fun addMatchCardBox(c: Character, locker: Int, ammount: Int, type: Int) = packetWriter(SendPacketOpcode.UPDATE_CHAR_BOX) {
            int(c.id)
            PacketCreator.addAnnounceBox(c.miniGame, 2, locker, 0, ammount, type)
        }


        fun getMatchCardNewVisitor(c: Character, slot: Int) = packetWriter(SendPacketOpcode.PLAYER_INTERACTION) {
            byte(PlayerInteractionHandler.Action.VISIT.code)
            byte(slot)
            CharacterPacket.addCharLook(c, false)
            gameASCIIString(c.name)
            int(1)
            int(c.getMiniGamePoints("wins", false))
            int(c.getMiniGamePoints("ties", false))
            int(c.getMiniGamePoints("losses", false))
            int(2000 + c.getMiniGamePoints("wins", false) - c.getMiniGamePoints("losses", false))
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

        fun getMiniGame(c: Client?, miniGame: MiniGame, owner: Boolean, piece: Int) = packetWriter(SendPacketOpcode.PLAYER_INTERACTION) {
            byte(PlayerInteractionHandler.Action.ROOM.code)
            byte(1)
            byte(0)
            byte(if (owner) 0 else 1)
            byte(0)
            CharacterPacket.addCharLook(miniGame.owner, false)
            gameASCIIString(miniGame.owner.name)
            if (miniGame.visitor != null) {
                val visitor = miniGame.visitor ?: return@packetWriter
                byte(1)
                CharacterPacket.addCharLook(visitor, false)
                gameASCIIString(visitor.name)
            }
            byte(0xFF)
            byte(0)
            int(1)
            int(miniGame.owner.getMiniGamePoints("wins", true))
            int(miniGame.owner.getMiniGamePoints("ties", true))
            int(miniGame.owner.getMiniGamePoints("losses", true))
            int(
                2000 + miniGame.owner.getMiniGamePoints(
                    "wins",
                    true
                ) - miniGame.owner.getMiniGamePoints("losses", true)
            )
            if (miniGame.visitor != null) {
                val visitor = miniGame.visitor
                byte(1)
                int(1)
                int(visitor!!.getMiniGamePoints("wins", true))
                int(visitor.getMiniGamePoints("ties", true))
                int(visitor.getMiniGamePoints("losses", true))
                int(
                    2000 + visitor.getMiniGamePoints("wins", true) - visitor.getMiniGamePoints(
                        "losses",
                        true
                    )
                )
            }
            byte(0xFF)
            gameASCIIString(miniGame.description)
            byte(piece)
            byte(0)
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

        fun addMiniGameInfo(chr: Character) = packetWriter {
            short(0)
        }

        fun getMiniGameNewVisitor(c: Character, slot: Int) = packetWriter(SendPacketOpcode.PLAYER_INTERACTION) {
            byte(PlayerInteractionHandler.Action.VISIT.code)
            byte(slot)
            CharacterPacket.addCharLook(c, false)
            gameASCIIString(c.name)
            int(1)
            int(c.getMiniGamePoints("wins", true))
            int(c.getMiniGamePoints("ties", true))
            int(c.getMiniGamePoints("losses", true))
            int(2000 + c.getMiniGamePoints("wins", true) - c.getMiniGamePoints("losses", true))
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