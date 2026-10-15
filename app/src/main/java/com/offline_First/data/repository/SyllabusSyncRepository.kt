package com.offline_First.data.repository

import android.util.Log
import com.offline_First.data.local.SyllabusChunkRecord
import com.offline_First.data.local.SyllabusDatabase
import com.offline_First.data.remote.SyllabusSyncClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

private const val TAG = "SyllabusSyncRepo"
private const val EXPECTED_EMBEDDING_MODEL = "intfloat/multilingual-e5-small"
private const val EXPECTED_EMBEDDING_DIMENSION = 384

sealed class SyllabusSyncState {
    object Idle : SyllabusSyncState()
    object Checking : SyllabusSyncState()
    data class Syncing(
        val packId: String,
        val downloadedChunks: Int,
        val totalChunks: Int,
        val progress: Float
    ) : SyllabusSyncState()
    data class Success(val packId: String, val totalChunks: Int) : SyllabusSyncState()
    data class IncompatibleModel(
        val packId: String,
        val expectedModel: String,
        val receivedModel: String,
        val message: String
    ) : SyllabusSyncState()
    data class Error(val message: String) : SyllabusSyncState()
}

/**
 * Orchestrates downloading precomputed syllabus packs from the backend into
 * the local SQLite database for offline RAG retrieval.
 */
class SyllabusSyncRepository(
    private val syncClient: SyllabusSyncClient,
    private val database: SyllabusDatabase
) {
    private val _syncState = MutableStateFlow<SyllabusSyncState>(SyllabusSyncState.Idle)
    val syncState: StateFlow<SyllabusSyncState> = _syncState.asStateFlow()

    /**
     * Synchronizes a syllabus curriculum pack matching user's board, class grade, and subject.
     * Enforces model version verification, downloads precomputed 384-dimensional vectors,
     * and persists everything into local SQLite.
     */
    suspend fun syncCurriculumPack(
        board: String,
        classGrade: String,
        subject: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        _syncState.value = SyllabusSyncState.Checking
        try {
            val packs = syncClient.fetchAvailablePacks()
            val matchedPack = packs.firstOrNull { p ->
                p.board.equals(board.trim(), ignoreCase = true) &&
                p.classGrade.equals(classGrade.trim(), ignoreCase = true) &&
                p.subject.contains(subject.trim(), ignoreCase = true)
            }

            if (matchedPack == null) {
                val msg = "No available syllabus pack for $board Class $classGrade $subject"
                Log.i(TAG, msg)
                _syncState.value = SyllabusSyncState.Error(msg)
                return@withContext Result.failure(NoSuchElementException(msg))
            }

            val packId = matchedPack.packId

            // 1. Fetch manifest and verify model compatibility
            val manifest = syncClient.fetchPackManifest(packId)
            if (!manifest.embeddingModel.equals(EXPECTED_EMBEDDING_MODEL, ignoreCase = true)) {
                val err = "Incompatible embedding model: expected '$EXPECTED_EMBEDDING_MODEL', received '${manifest.embeddingModel}'"
                Log.e(TAG, err)
                _syncState.value = SyllabusSyncState.IncompatibleModel(
                    packId = packId,
                    expectedModel = EXPECTED_EMBEDDING_MODEL,
                    receivedModel = manifest.embeddingModel,
                    message = err
                )
                return@withContext Result.failure(IllegalStateException(err))
            }

            if (manifest.embeddingDimension != EXPECTED_EMBEDDING_DIMENSION) {
                val err = "Incompatible embedding dimension: expected $EXPECTED_EMBEDDING_DIMENSION, received ${manifest.embeddingDimension}"
                Log.e(TAG, err)
                _syncState.value = SyllabusSyncState.IncompatibleModel(
                    packId = packId,
                    expectedModel = "$EXPECTED_EMBEDDING_MODEL ($EXPECTED_EMBEDDING_DIMENSION-dim)",
                    receivedModel = "${manifest.embeddingModel} (${manifest.embeddingDimension}-dim)",
                    message = err
                )
                return@withContext Result.failure(IllegalStateException(err))
            }

            // 2. Paginated chunk download
            var cursor: String? = null
            var downloadedCount = 0
            val totalCount = manifest.chunkCount

            _syncState.value = SyllabusSyncState.Syncing(
                packId = packId,
                downloadedChunks = 0,
                totalChunks = totalCount,
                progress = 0.0f
            )

            do {
                val page = syncClient.fetchPackChunks(packId = packId, cursor = cursor, limit = 200)

                val records = page.chunks.map { dto ->
                    SyllabusChunkRecord(
                        chunkId = dto.chunkId,
                        board = dto.board,
                        classGrade = dto.classGrade,
                        subject = dto.subject,
                        chapterTitle = dto.chapter ?: "General",
                        sectionTitle = dto.section,
                        pageReference = dto.pageReference,
                        content = dto.content,
                        sourceId = dto.sourceId,
                        corpusVersion = dto.corpusVersion,
                        language = dto.language,
                        contentHash = dto.contentHash,
                        embedding = dto.embedding?.toFloatArray(),
                        embeddingModel = dto.embeddingModel,
                        embeddingVersion = dto.embeddingVersion
                    )
                }

                database.importSyllabusRecords(records)
                downloadedCount += records.size
                cursor = page.nextCursor

                val progress = if (totalCount > 0) downloadedCount.toFloat() / totalCount else 1.0f
                _syncState.value = SyllabusSyncState.Syncing(
                    packId = packId,
                    downloadedChunks = downloadedCount,
                    totalChunks = totalCount,
                    progress = progress.coerceIn(0.0f, 1.0f)
                )

            } while (page.hasMore && !cursor.isNullOrBlank())

            Log.i(TAG, "Successfully synced syllabus pack '$packId' ($downloadedCount chunks)")
            _syncState.value = SyllabusSyncState.Success(packId = packId, totalChunks = downloadedCount)
            Result.success(downloadedCount)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync syllabus pack: ${e.message}", e)
            _syncState.value = SyllabusSyncState.Error(e.message ?: "Unknown sync error")
            Result.failure(e)
        }
    }
}
