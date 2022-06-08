package net.server.guild

import client.Character

class GuildCharacter {
    var level: Int
    val id: Int
    var world: Int = -1
    var channel :Int = -1
    var jobId: Int
    var guildRank: Int
    var guildId: Int
    var online: Boolean
    val name: String

    constructor(c: Character) {
        name = c.name
        level = c.level.toInt()
        id = c.id
        channel = c.client.channel
        world = c.world
        jobId = c.job.id
        guildRank = c.guildRank
        guildId = c.guildId
        online = true
    }

    constructor(id: Int, level: Int, name: String, channel: Int, world: Int, job: Int, rank: Int, gid: Int, on: Boolean) {
        this.id = id
        this.level = level
        this.name = name
        if (on) {
            this.channel = channel
            this.world = world
        }
        jobId = job
        online = on
        guildRank = rank
        guildId = gid
    }
}