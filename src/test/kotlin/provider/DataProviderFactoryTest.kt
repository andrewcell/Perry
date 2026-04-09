package provider

import io.mockk.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import provider.wz.WZFile
import provider.wz.XMLWZFile
import tools.ServerJSON
import tools.settings.Settings
import java.io.File
import java.nio.file.Path

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataProviderFactoryTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var mockSettings: Settings
    private val testWzPath = "/test/wz/path"

    @BeforeAll
    fun setupAll() {
        mockSettings = mockk(relaxed = true)
        every { mockSettings.wzPath } returns testWzPath

        mockkObject(ServerJSON)
        every { ServerJSON.settings } returns mockSettings
    }

    @AfterAll
    fun tearDownAll() {
        unmockkObject(ServerJSON)
        unmockkAll()
    }

    @AfterEach
    fun tearDown() {
        unmockkConstructor(WZFile::class, XMLWZFile::class)
    }

    @Nested
    @DisplayName("getWz() method tests")
    inner class GetWzTests {

        @Test
        @DisplayName("Should return WZFile when valid .wz file is provided with provideImages=true")
        fun testGetWz_ValidWzFile_WithImages() {
            val wzFile = tempDir.resolve("test.wz").toFile()
            wzFile.createNewFile()

            val mockWZFile = mockk<WZFile>(relaxed = true)
            mockkConstructor(WZFile::class)
            every { anyConstructed<WZFile>() } returns mockWZFile

            val result = DataProviderFactory.getWz(wzFile, true)

            assertNotNull(result)
            verify { WZFile(wzFile, true) }
        }

        @Test
        @DisplayName("Should return WZFile when valid .wz file is provided with provideImages=false")
        fun testGetWz_ValidWzFile_WithoutImages() {
            val wzFile = tempDir.resolve("data.WZ").toFile()
            wzFile.createNewFile()

            val mockWZFile = mockk<WZFile>(relaxed = true)
            mockkConstructor(WZFile::class)
            every { anyConstructed<WZFile>() } returns mockWZFile

            val result = DataProviderFactory.getWz(wzFile, false)

            assertNotNull(result)
            verify { WZFile(wzFile, false) }
        }

        @Test
        @DisplayName("Should return XMLWZFile when .wz file loading fails")
        fun testGetWz_WzFileLoadingFails_ReturnsXMLWZFile() {
            val wzFile = tempDir.resolve("corrupted.wz").toFile()
            wzFile.createNewFile()

            mockkConstructor(WZFile::class)
            every { anyConstructed<WZFile>() } throws RuntimeException("Failed to load WZ")

            val mockXMLWZ = mockk<XMLWZFile>(relaxed = true)
            mockkConstructor(XMLWZFile::class)
            every { anyConstructed<XMLWZFile>() } returns mockXMLWZ

            val result = DataProviderFactory.getWz(wzFile, true)

            assertNotNull(result)
            verify { XMLWZFile(wzFile) }
        }

        @Test
        @DisplayName("Should return XMLWZFile when file is a directory")
        fun testGetWz_Directory_ReturnsXMLWZFile() {
            val directory = tempDir.resolve("testDir").toFile()
            directory.mkdir()

            val mockXMLWZ = mockk<XMLWZFile>(relaxed = true)
            mockkConstructor(XMLWZFile::class)
            every { anyConstructed<XMLWZFile>() } returns mockXMLWZ

            val result = DataProviderFactory.getWz(directory, false)

            assertNotNull(result)
            verify { XMLWZFile(directory) }
        }

        @Test
        @DisplayName("Should return XMLWZFile when file doesn't have .wz extension")
        fun testGetWz_NonWzExtension_ReturnsXMLWZFile() {
            val txtFile = tempDir.resolve("data.txt").toFile()
            txtFile.createNewFile()

            val mockXMLWZ = mockk<XMLWZFile>(relaxed = true)
            mockkConstructor(XMLWZFile::class)
            every { anyConstructed<XMLWZFile>() } returns mockXMLWZ

            val result = DataProviderFactory.getWz(txtFile, true)

            assertNotNull(result)
            verify { XMLWZFile(txtFile) }
        }

        @Test
        @DisplayName("Should handle .wz extension case-insensitively")
        fun testGetWz_CaseInsensitiveExtension() {
            val wzFile1 = tempDir.resolve("test1.WZ").toFile()
            val wzFile2 = tempDir.resolve("test2.Wz").toFile()
            val wzFile3 = tempDir.resolve("test3.wZ").toFile()

            listOf(wzFile1, wzFile2, wzFile3).forEach { it.createNewFile() }

            val mockWZFile = mockk<WZFile>(relaxed = true)
            mockkConstructor(WZFile::class)
            every { anyConstructed<WZFile>() } returns mockWZFile

            listOf(wzFile1, wzFile2, wzFile3).forEach {
                assertNotNull(DataProviderFactory.getWz(it, false))
            }

            verify(exactly = 3) { WZFile(any(), any()) }
        }

        @Test
        @DisplayName("Should handle different exception types when loading WZ")
        fun testGetWz_DifferentExceptionTypes() {
            val wzFile = tempDir.resolve("error.wz").toFile()
            wzFile.createNewFile()

            val mockXMLWZ = mockk<XMLWZFile>(relaxed = true)

            // Test RuntimeException
            mockkConstructor(WZFile::class, XMLWZFile::class)
            every { anyConstructed<WZFile>() } throws RuntimeException("Runtime error")
            every { anyConstructed<XMLWZFile>() } returns mockXMLWZ

            assertNotNull(DataProviderFactory.getWz(wzFile, true))

            // Test generic Exception
         //   clearMocks(answers = false)
            every { anyConstructed<WZFile>() } throws Exception("Generic error")
            every { anyConstructed<XMLWZFile>() } returns mockXMLWZ

            assertNotNull(DataProviderFactory.getWz(wzFile, false))
        }
    }

    @Nested
    @DisplayName("getDataProvider() method tests")
    inner class GetDataProviderTests {

        @Test
        @DisplayName("Should call getWz with provideImages=false for .wz file")
        fun testGetDataProvider_WzFile() {
            val inputFile = tempDir.resolve("test.wz").toFile()
            inputFile.createNewFile()

            val mockWZFile = mockk<WZFile>(relaxed = true)
            mockkConstructor(WZFile::class)
            every { anyConstructed<WZFile>() } returns mockWZFile

            val result = DataProviderFactory.getDataProvider(inputFile)

            assertNotNull(result)
            verify { WZFile(inputFile, false) }
        }

        @Test
        @DisplayName("Should return XMLWZFile for non-wz file")
        fun testGetDataProvider_NonWzFile() {
            val xmlFile = tempDir.resolve("data.xml").toFile()
            xmlFile.createNewFile()

            val mockXMLWZ = mockk<XMLWZFile>(relaxed = true)
            mockkConstructor(XMLWZFile::class)
            every { anyConstructed<XMLWZFile>() } returns mockXMLWZ

            val result = DataProviderFactory.getDataProvider(xmlFile)

            assertNotNull(result)
            verify { XMLWZFile(xmlFile) }
        }
    }

    @Nested
    @DisplayName("getImageProvidingDataProvider() method tests")
    inner class GetImageProvidingDataProviderTests {

        @Test
        @DisplayName("Should call getWz with provideImages=true for .wz file")
        fun testGetImageProvidingDataProvider_WzFile() {
            val inputFile = tempDir.resolve("images.wz").toFile()
            inputFile.createNewFile()

            val mockWZFile = mockk<WZFile>(relaxed = true)
            mockkConstructor(WZFile::class)
            every { anyConstructed<WZFile>() } returns mockWZFile

            val result = DataProviderFactory.getImageProvidingDataProvider(inputFile)

            assertNotNull(result)
            verify { WZFile(inputFile, true) }
        }

        @Test
        @DisplayName("Should return XMLWZFile for directory")
        fun testGetImageProvidingDataProvider_Directory() {
            val directory = tempDir.resolve("imageDir").toFile()
            directory.mkdir()

            val mockXMLWZ = mockk<XMLWZFile>(relaxed = true)
            mockkConstructor(XMLWZFile::class)
            every { anyConstructed<XMLWZFile>() } returns mockXMLWZ

            val result = DataProviderFactory.getImageProvidingDataProvider(directory)

            assertNotNull(result)
            verify { XMLWZFile(directory) }
        }
    }

    @Nested
    @DisplayName("fileInWzPath() method tests")
    inner class FileInWzPathTests {

        @Test
        @DisplayName("Should create File with wzPath and filename")
        fun testFileInWzPath_NormalFilename() {
            val result = DataProviderFactory.fileInWzPath("data.wz")

            assertNotNull(result)
            assertEquals(File(testWzPath, "data.wz"), result)
            assertEquals("data.wz", result.name)
        }

        @Test
        @DisplayName("Should handle empty filename")
        fun testFileInWzPath_EmptyFilename() {
            val result = DataProviderFactory.fileInWzPath("")

            assertNotNull(result)
            assertEquals(File(testWzPath, ""), result)
        }

        @Test
        @DisplayName("Should handle filename with path separators")
        fun testFileInWzPath_FilenameWithPath() {
            val result = DataProviderFactory.fileInWzPath("subdir/data.wz")

            assertNotNull(result)
            assertTrue(result.path.contains("subdir"))
            assertTrue(result.path.endsWith("data.wz"))
        }

        @Test
        @DisplayName("Should handle special characters in filename")
        fun testFileInWzPath_SpecialCharacters() {
            val result = DataProviderFactory.fileInWzPath("data-file_01.wz")

            assertNotNull(result)
            assertEquals("data-file_01.wz", result.name)
        }

        @Test
        @DisplayName("Should handle long filenames")
        fun testFileInWzPath_LongFilename() {
            val longFilename = "a".repeat(200) + ".wz"

            val result = DataProviderFactory.fileInWzPath(longFilename)

            assertNotNull(result)
            assertEquals(longFilename, result.name)
        }
    }

    @Nested
    @DisplayName("wzPath property tests")
    inner class WzPathTests {

        @Test
        @DisplayName("Should retrieve wzPath from settings")
        fun testWzPath_RetrievesFromSettings() {
            val result = DataProviderFactory.wzPath

            assertEquals(testWzPath, result)
            verify(atLeast = 1) { ServerJSON.settings.wzPath }
        }

        @Test
        @DisplayName("Should return consistent wzPath value")
        fun testWzPath_Consistency() {
            val result1 = DataProviderFactory.wzPath
            val result2 = DataProviderFactory.wzPath

            assertEquals(result1, result2)
            assertEquals(testWzPath, result1)
            assertEquals(testWzPath, result2)
        }
    }

    @Nested
    @DisplayName("Edge cases and integration tests")
    inner class EdgeCasesTests {

        @Test
        @DisplayName("Should handle directory with .wz extension")
        fun testGetWz_DirectoryWithWzExtension() {
            val wzDirectory = tempDir.resolve("data.wz").toFile()
            wzDirectory.mkdir()

            val mockXMLWZ = mockk<XMLWZFile>(relaxed = true)
            mockkConstructor(XMLWZFile::class)
            every { anyConstructed<XMLWZFile>() } returns mockXMLWZ

            val result = DataProviderFactory.getWz(wzDirectory, false)

            assertNotNull(result)
            verify { XMLWZFile(wzDirectory) }
            verify(exactly = 0) { WZFile(any(), any()) }
        }

        @Test
        @DisplayName("Should handle mixed case .wz extension")
        fun testGetWz_MixedCaseExtension() {
            val testFiles = listOf("test.wZ", "test.Wz", "test.WZ")

            testFiles.forEach { fileName ->
                val file = tempDir.resolve(fileName).toFile()
                file.createNewFile()

                val mockWZFile = mockk<WZFile>(relaxed = true)
                mockkConstructor(WZFile::class)
                every { anyConstructed<WZFile>() } returns mockWZFile

                val result = DataProviderFactory.getWz(file, false)

                assertNotNull(result)
                unmockkConstructor(WZFile::class)
            }
        }

        @Test
        @DisplayName("Should handle null parent path in fileInWzPath")
        fun testFileInWzPath_WithNullParent() {
            val result = DataProviderFactory.fileInWzPath("test.wz")

            assertNotNull(result)
            assertTrue(result.path.contains(testWzPath))
        }
    }
}