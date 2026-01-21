package client

/**
 * Represents a ring item that can be equipped by a character.
 * Commented out : Ring was not available yet in target client.
 * @param ringId The unique identifier for this ring.
 * @param partnerRingId The unique identifier for the partner's ring.
 * @param partnerId The unique identifier for the partner character.
 * @param itemId The item identifier for the ring.
 * @param partnerName The name of the partner character.
 * @constructor Creates a new Ring instance.
 * @author Seungyeon Choi (git@vxz.me)
 * @version 1.0
 *
 */
class Ring(val ringId: Int, val partnerRingId: Int, val partnerId: Int, val itemId: Int, val partnerName: String) : Comparable<Ring> {
    var equipped = false

    companion object {
        fun loadFromDatabase(ringId: Int): Ring? {
            /*try {
                transaction {
                }
                val con = DatabaseConnection.getConnection()
                val ps = con.prepareStatement("SELECT * FROM rings WHERE id = ?")
                ps.setInt(1, ringId)
                val rs = ps.executeQuery()
                if (rs.next()) {
                    return Ring(ringId, rs.getInt("partnerRingId"), rs.getInt("partnerChrId"), rs.getInt("itemid"), rs.getString("partnerName"))
                }
                rs.close()
                ps.close()
            } catch (e: SQLException) {
                log(e.message, "Ring", Logger.Type.CRITICAL, e.stackTrace)
            }*/
            return null
        }

        fun createRing(itemId: Int, partner1: Character?, partner2: Character?): Int {
            /*try {
                if (partner1 == null) return -2
                if (partner2 == null) return -1
                val ringId = arrayOf(-1, -1)
                val con = DatabaseConnection.getConnection()
                var ps = con.prepareStatement("INSERT INTO rings (itemid, partnerChrId, partnername) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)
                ps.setInt(1, itemId)
                ps.setInt(2, partner2.id)
                ps.setString(3, partner2.name)
                ps.executeUpdate()
                var rs = ps.generatedKeys
                rs.next()
                ringId[0] = rs.getInt(1)
                rs.close()
                ps.close()
                ps = con.prepareStatement("INSERT INTO rings (itemid, partnerRingId, partnerChrId, partnername) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)
                ps.setInt(1, itemId)
                ps.setInt(2, ringId[0])
                ps.setInt(3, partner1.id)
                ps.setString(4, partner1.name)
                ps.executeUpdate()
                rs = ps.generatedKeys
                rs.next()
                ringId[1] = rs.getInt(1)
                rs.close()
                ps.close()
                ps = con.prepareStatement("UPDATE rings SET partnerRingId = ? WHERE id = ?")
                ps.setInt(1, ringId[1])
                ps.setInt(2, ringId[0])
                ps.executeUpdate()
                ps.close()
                return ringId[0]
            } catch (e: SQLException) {
                log(e.message, "Ring", Logger.Type.CRITICAL, e.stackTrace)
            }*/
            return -1
        }
    }

    override fun compareTo(other: Ring): Int {
        return if (ringId < other.ringId) -1
        else if (ringId == other.ringId) 0
        else 1
    }
}