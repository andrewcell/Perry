package provider

interface DataEntity {
    val name: String
    val parent: DataEntity?
}