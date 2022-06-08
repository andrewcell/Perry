package provider.wz

import provider.DataEntity
import provider.DataFileEntry

class WZFileEntry(name: String, size: Int, checksum: Int, parent: DataEntity) : WZEntry(name, size, checksum, parent), DataFileEntry