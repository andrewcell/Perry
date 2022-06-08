package provider.wz

import provider.DataEntity
import provider.DataEntry

open class WZEntry(
    override val name: String,
    override val size: Int,
    override val checksum: Int,
    override val parent: DataEntity?
) : DataEntry {
    override var offset: Int = 0
}