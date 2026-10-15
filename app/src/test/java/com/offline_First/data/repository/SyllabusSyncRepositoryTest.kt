package com.offline_First.data.repository

import com.offline_First.data.local.SyllabusChunkRecord
import com.offline_First.data.local.SyllabusDatabase
import com.offline_First.data.remote.SyllabusChunkDto
import com.offline_First.data.remote.SyllabusChunksResponseDto
import com.offline_First.data.remote.SyllabusManifestDto
import com.offline_First.data.remote.SyllabusPackSummaryDto
import com.offline_First.data.remote.SyllabusSyncClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fake implementation of SyllabusSyncClient that provides deterministic mock data
 * without requiring network calls.
 */
class FakeSyllabusSyncClient(
    var packsToReturn: List<SyllabusPackSummaryDto> = emptyList(),
    var manifestToReturn: SyllabusManifestDto? = null,
    var chunksToReturn: SyllabusChunksResponseDto? = null
) : SyllabusSyncClient {
    override suspend fun fetchAvailablePacks(): List<SyllabusPackSummaryDto> = packsToReturn

    override suspend fun fetchPackManifest(packId: String): SyllabusManifestDto =
        manifestToReturn ?: throw NoSuchElementException("Manifest not configured in fake")

    override suspend fun fetchPackChunks(
        packId: String,
        cursor: String?,
        limit: Int
    ): SyllabusChunksResponseDto =
        chunksToReturn ?: throw NoSuchElementException("Chunks not configured in fake")
}

/**
 * Fake in-memory syllabus database to test repository orchestration.
 */
class FakeSyllabusDatabase : SyllabusDatabase(
    android.content.ContextWrapper(null)
) {
    val importedRecords = mutableListOf<SyllabusChunkRecord>()

    override fun importSyllabusRecords(records: List<SyllabusChunkRecord>) {
        importedRecords.addAll(records)
    }
}

class SyllabusSyncRepositoryTest {

    @Test
    fun testRejectsIncompatibleEmbeddingModel() = runBlocking {
        val fakeClient = FakeSyllabusSyncClient(
            packsToReturn = listOf(
                SyllabusPackSummaryDto(
                    packId = "cbse-10-science",
                    board = "CBSE",
                    classGrade = "10",
                    subject = "Science",
                    chunkCount = 50,
                    embeddingModel = "openai/text-embedding-3-small"
                )
            ),
            manifestToReturn = SyllabusManifestDto(
                packId = "cbse-10-science",
                board = "CBSE",
                classGrade = "10",
                subject = "Science",
                chunkCount = 50,
                corpusVersion = "1.0",
                embeddingModel = "openai/text-embedding-3-small", // Incompatible model
                embeddingDimension = 1536
            )
        )
        val fakeDb = FakeSyllabusDatabase()

        val repo = SyllabusSyncRepository(fakeClient, fakeDb)
        val result = repo.syncCurriculumPack("CBSE", "10", "Science")

        assertTrue(result.isFailure)
        val state = repo.syncState.value
        assertTrue(state is SyllabusSyncState.IncompatibleModel)
        val incomp = state as SyllabusSyncState.IncompatibleModel
        assertEquals("intfloat/multilingual-e5-small", incomp.expectedModel)
    }

    @Test
    fun testRejectsIncompatibleEmbeddingDimension() = runBlocking {
        val fakeClient = FakeSyllabusSyncClient(
            packsToReturn = listOf(
                SyllabusPackSummaryDto(
                    packId = "cbse-10-science",
                    board = "CBSE",
                    classGrade = "10",
                    subject = "Science",
                    chunkCount = 50,
                    embeddingModel = "intfloat/multilingual-e5-small"
                )
            ),
            manifestToReturn = SyllabusManifestDto(
                packId = "cbse-10-science",
                board = "CBSE",
                classGrade = "10",
                subject = "Science",
                chunkCount = 50,
                corpusVersion = "1.0",
                embeddingModel = "intfloat/multilingual-e5-small",
                embeddingDimension = 512 // Incompatible dimension
            )
        )
        val fakeDb = FakeSyllabusDatabase()

        val repo = SyllabusSyncRepository(fakeClient, fakeDb)
        val result = repo.syncCurriculumPack("CBSE", "10", "Science")

        assertTrue(result.isFailure)
        val state = repo.syncState.value
        assertTrue(state is SyllabusSyncState.IncompatibleModel)
    }

    @Test
    fun testSyncCurriculumPackSuccess() = runBlocking {
        val dummyVector = List(384) { 0.05f }
        val fakeClient = FakeSyllabusSyncClient(
            packsToReturn = listOf(
                SyllabusPackSummaryDto(
                    packId = "cbse-10-science",
                    board = "CBSE",
                    classGrade = "10",
                    subject = "Science",
                    chunkCount = 2,
                    embeddingModel = "intfloat/multilingual-e5-small",
                    embeddingVersion = "1.0"
                )
            ),
            manifestToReturn = SyllabusManifestDto(
                packId = "cbse-10-science",
                board = "CBSE",
                classGrade = "10",
                subject = "Science",
                chunkCount = 2,
                corpusVersion = "1.0",
                embeddingModel = "intfloat/multilingual-e5-small",
                embeddingVersion = "1.0",
                embeddingDimension = 384
            ),
            chunksToReturn = SyllabusChunksResponseDto(
                packId = "cbse-10-science",
                chunks = listOf(
                    SyllabusChunkDto(
                        chunkId = "c1",
                        content = "Chemical Reactions and Equations",
                        board = "CBSE",
                        classGrade = "10",
                        subject = "Science",
                        chapter = "Chemical Reactions",
                        embedding = dummyVector
                    ),
                    SyllabusChunkDto(
                        chunkId = "c2",
                        content = "Acids, Bases and Salts",
                        board = "CBSE",
                        classGrade = "10",
                        subject = "Science",
                        chapter = "Acids, Bases and Salts",
                        embedding = dummyVector
                    )
                ),
                nextCursor = null,
                hasMore = false,
                totalChunks = 2,
                embeddingModel = "intfloat/multilingual-e5-small",
                embeddingVersion = "1.0",
                embeddingDimension = 384
            )
        )
        val fakeDb = FakeSyllabusDatabase()

        val repo = SyllabusSyncRepository(fakeClient, fakeDb)
        val result = repo.syncCurriculumPack("CBSE", "10", "Science")

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
        val state = repo.syncState.value
        assertTrue(state is SyllabusSyncState.Success)
        assertEquals(2, (state as SyllabusSyncState.Success).totalChunks)
        assertEquals(2, fakeDb.importedRecords.size)
    }
}
