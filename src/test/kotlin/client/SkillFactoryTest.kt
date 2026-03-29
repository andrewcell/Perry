package client

import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import provider.Data
import provider.DataDirectoryEntry
import provider.DataFileEntry
import provider.DataProvider
import provider.DataProviderFactory
import provider.DataTool
import server.StatEffect
import tools.ServerJSON
import tools.StringXmlParser
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SkillFactoryTest {

    @BeforeEach
    fun setUp() {
        // Clear skills map before each test
        SkillFactory.skills.clear()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // -------------------------------------------------------
    // getSkill
    // -------------------------------------------------------

    @Test
    fun `getSkill returns null when skill map is empty`() {
        assertNull(SkillFactory.getSkill(1001))
    }

    @Test
    fun `getSkill returns null for null id`() {
        assertNull(SkillFactory.getSkill(null))
    }

    @Test
    fun `getSkill returns skill when it exists in map`() {
        val skill = Skill(1001)
        SkillFactory.skills[1001] = skill

        val result = SkillFactory.getSkill(1001)

        assertNotNull(result)
        assertEquals(1001, result?.id)
    }

    @Test
    fun `getSkill returns null when id not present in map`() {
        val skill = Skill(1001)
        SkillFactory.skills[1001] = skill

        assertNull(SkillFactory.getSkill(9999))
    }

    // -------------------------------------------------------
    // getSkillName
    // -------------------------------------------------------

    @Test
    fun `getSkillName returns name when data has name child`() {
        val nameData = mockk<Data>()
        val strData = mockk<Data>()
        val rootData = mockk<Data>()
        val dataProvider = mockk<DataProvider>()

        mockkObject(DataProviderFactory)
        mockkObject(ServerJSON)
        every { ServerJSON.settings.wzPath } returns "/wz"
        every { DataProviderFactory.getDataProvider(any<File>()) } returns dataProvider
        every { dataProvider.getData("Skill.img") } returns rootData
        every { rootData.getChildByPath("1001") } returns strData
        every { strData.children } returns listOf(nameData)
        every { nameData.name } returns "name"
        every { DataTool.getStringNullable(nameData, null) } returns "Magic Arrow"

        mockkObject(DataTool)

        val result = SkillFactory.getSkillName(1001)

        assertEquals("Magic Arrow", result)
    }

    @Test
    fun `getSkillName returns null when strData is null`() {
        val rootData = mockk<Data>()
        val dataProvider = mockk<DataProvider>()

        mockkObject(DataProviderFactory)
        mockkObject(ServerJSON)
        every { ServerJSON.settings.wzPath } returns "/wz"
        every { DataProviderFactory.getDataProvider(any<File>()) } returns dataProvider
        every { dataProvider.getData("Skill.img") } returns rootData
        every { rootData.getChildByPath(any()) } returns null

        val result = SkillFactory.getSkillName(1001)

        assertNull(result)
    }

    @Test
    fun `getSkillName returns null when no child named name exists`() {
        val otherData = mockk<Data>()
        val strData = mockk<Data>()
        val rootData = mockk<Data>()
        val dataProvider = mockk<DataProvider>()

        mockkObject(DataProviderFactory)
        mockkObject(ServerJSON)
        every { ServerJSON.settings.wzPath } returns "/wz"
        every { DataProviderFactory.getDataProvider(any<File>()) } returns dataProvider
        every { dataProvider.getData("Skill.img") } returns rootData
        every { rootData.getChildByPath("1001") } returns strData
        every { strData.children } returns listOf(otherData)
        every { otherData.name } returns "desc"

        val result = SkillFactory.getSkillName(1001)

        assertNull(result)
    }

    // -------------------------------------------------------
    // getSkillDataByJobId
    // -------------------------------------------------------

    @Test
    fun `getSkillDataByJobId returns data for matching job file`() {
        val file1 = mockk<DataFileEntry>()
        val file2 = mockk<DataFileEntry>()
        val expectedData = mockk<Data>()
        val root = mockk<DataDirectoryEntry>()
        val dataProvider = mockk<DataProvider>()

        every { file1.name } returns "100.img"
        every { file2.name } returns "200.img"
        every { root.files } returns listOf(file1, file2)
        every { dataProvider.root } returns root
        every { dataProvider.getData("100.img") } returns expectedData

        // Inject the mocked dataSource via reflection
        val field = SkillFactory::class.java.getDeclaredField("dataSource")
        field.isAccessible = true
        field.set(SkillFactory, dataProvider)

        val result = SkillFactory.getSkillDataByJobId(100)

        assertEquals(expectedData, result)
        verify { dataProvider.getData("100.img") }
    }

    // -------------------------------------------------------
    // loadAllSkills
    // -------------------------------------------------------

    @Test
    fun `loadAllSkills skips entries with name longer than 8 characters`() {
        mockkObject(ServerJSON)
        every { ServerJSON.settings } returns mockk(relaxed = true)
        val longNameFile = mockk<DataFileEntry>()
        val root = mockk<DataDirectoryEntry>()
        val dataProvider = mockk<DataProvider>()

        every { longNameFile.name } returns "123456789.img" // length > 8
        every { root.files } returns listOf(longNameFile)
        every { dataProvider.root } returns root

        val field = SkillFactory::class.java.getDeclaredField("dataSource")
        field.isAccessible = true
        field.set(SkillFactory, dataProvider)

        SkillFactory.loadAllSkills()

        assertTrue(SkillFactory.skills.isEmpty())
        verify(exactly = 0) { dataProvider.getData(any()) }
    }

    @Test
    fun `loadAllSkills loads skills and populates skills map`() {
        mockkObject(DataTool)
        mockkObject(StringXmlParser)
        mockkObject(ServerJSON)

        val skillData = mockk<Data>()
        val skillEntry = mockk<Data>()
        val levelData = mockk<Data>()
        val statEffect = mockk<StatEffect>()
        val topFile = mockk<DataFileEntry>()
        val root = mockk<DataDirectoryEntry>()
        val dataProvider = mockk<DataProvider>()
        val skillContainerData = mockk<Data>()
        val nameData = mockk<Data>()
        val nameChildData = mockk<Data>()
        val stringDataProvider = mockk<DataProvider>()
        val stringRootData = mockk<Data>()

        every { topFile.name } returns "100.img"
        every { root.files } returns listOf(topFile)
        every { dataProvider.root } returns root
        every { dataProvider.getData("100.img") } returns skillData

        // skillData iteration: one child named "skill"
        every { skillData.forEach(any()) } answers {
            (arg<(Data) -> Unit>(0))(skillContainerData)
        }
        every { skillContainerData.name } returns "skill"
        every { skillContainerData.forEach(any()) } answers {
            (arg<(Data) -> Unit>(0))(skillEntry)
        }
        every { skillEntry.name } returns "1001"

        // loadFromData internals
        every { DataTool.getInt("skillType", skillEntry, -1) } returns 2 // isBuff = true
        every { DataTool.getStringNullable("elemAttr", skillEntry, null) } returns null
        every { skillEntry.getChildByPath("effect") } returns null
        every { skillEntry.getChildByPath("level") } returns null
        every { DataTool.getInt("cooltime", skillEntry, 0) } returns 0

        // getSkillName internals
        every { ServerJSON.settings.wzPath } returns "/wz"
        every { DataProviderFactory.getDataProvider(any<File>()) } returns stringDataProvider
        every { stringDataProvider.getData("Skill.img") } returns stringRootData
        every { stringRootData.getChildByPath("1001") } returns nameChildData
        every { nameChildData.children } returns listOf(nameData)
        every { nameData.name } returns "name"
        every { DataTool.getStringNullable(nameData, null) } returns "Arrow"

        mockkObject(DataProviderFactory)
        every { StringXmlParser.addSkillEntry(any(), any()) } just Runs

        val field = SkillFactory::class.java.getDeclaredField("dataSource")
        field.isAccessible = true
        field.set(SkillFactory, dataProvider)

        SkillFactory.loadAllSkills()

        assertTrue(SkillFactory.skills.containsKey(1001))
        assertEquals(1001, SkillFactory.skills[1001]?.id)
    }
}