package tools.settings

import kotlinx.serialization.Serializable

//{"id":1,"dataId":9900000,"f":0,"hide":0,"fh":228,"type":"n","cy":101,"rx0":-217,"rx1":-217,"x":-217,"y":101,"mobTime":1000,"mid":100000000},
@Serializable
data class WZCustomLifeDatabase(
    val id: Int,
    val dataId: Int,
    val f: Int,
    val hide: Int,
    val fh: Int,
    val type: String,
    val cy: Int,
    val rx0: Int,
    val rx1: Int,
    val x: Int,
    val y: Int,
    val mobTime: Int,
    val mid: Int
)