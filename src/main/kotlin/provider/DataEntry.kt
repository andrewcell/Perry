package provider

interface DataEntry : DataEntity {
    override val name: String
    val size: Int
    val checksum: Int
    val offset: Int
}