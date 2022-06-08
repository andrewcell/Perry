package provider

import provider.wz.DataType

interface Data : DataEntity, Iterable<Data> {
    override val name: String
    val type: DataType
    val children: List<Data>
    val data: Any?
    fun getChildByPath(path: String): Data?
}