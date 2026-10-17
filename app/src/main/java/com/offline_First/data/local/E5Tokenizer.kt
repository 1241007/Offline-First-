package com.offline_First.data.local

import java.io.File
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Native SentencePiece Unigram Tokenizer for intfloat/multilingual-e5-small.
 *
 * Implements the Viterbi path-decoding algorithm matching HuggingFace tokenizers
 * using Xenova/multilingual-e5-small tokenizer.json.
 */
class E5Tokenizer(
    private val pieceToId: Map<String, Int>,
    private val pieceToScore: FloatArray
) {
    companion object {
        const val BOS_TOKEN_ID = 0L
        const val PAD_TOKEN_ID = 1L
        const val EOS_TOKEN_ID = 2L
        const val UNK_TOKEN_ID = 3L
        const val MAX_PIECE_LENGTH = 32
        const val MAX_SEQUENCE_LENGTH = 512

        /**
         * Parses vocabulary and unigram scores directly from tokenizer.json.
         */
        fun fromFile(file: File): E5Tokenizer {
            return fromInputStream(file.inputStream())
        }

        /**
         * Streaming parser for tokenizer.json that reads the "vocab" array without
         * loading the entire 17MB file into memory as a monolithic JSON object.
         */
        fun fromInputStream(inputStream: InputStream): E5Tokenizer {
            val pieceToId = HashMap<String, Int>(260000, 0.75f)
            val scores = ArrayList<Float>(260000)

            inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val vocabMarker = "\"vocab\""
                var markerIdx = 0
                var ch: Int
                while (reader.read().also { ch = it } != -1) {
                    if (ch.toChar() == vocabMarker[markerIdx]) {
                        markerIdx++
                        if (markerIdx == vocabMarker.length) break
                    } else {
                        markerIdx = if (ch.toChar() == vocabMarker[0]) 1 else 0
                    }
                }

                while (reader.read().also { ch = it } != -1) {
                    if (ch.toChar() == '[') break
                }

                var id = 0
                val sb = StringBuilder()
                while (true) {
                    while (reader.read().also { ch = it } != -1 && ch.toChar() != '[' && ch.toChar() != ']') {}
                    if (ch == -1 || ch.toChar() == ']') break

                    while (reader.read().also { ch = it } != -1 && ch.toChar() != '"') {}
                    sb.setLength(0)
                    while (reader.read().also { ch = it } != -1) {
                        val c = ch.toChar()
                        if (c == '\\') {
                            val next = reader.read()
                            if (next == -1) break
                            when (val nc = next.toChar()) {
                                'u' -> {
                                    val hex = CharArray(4)
                                    reader.read(hex)
                                    sb.append(String(hex).toInt(16).toChar())
                                }
                                'n' -> sb.append('\n')
                                'r' -> sb.append('\r')
                                't' -> sb.append('\t')
                                'b' -> sb.append('\b')
                                'f' -> sb.append('\u000c')
                                '"' -> sb.append('"')
                                '\\' -> sb.append('\\')
                                '/' -> sb.append('/')
                                else -> sb.append(nc)
                            }
                        } else if (c == '"') {
                            break
                        } else {
                            sb.append(c)
                        }
                    }
                    val piece = sb.toString()

                    while (reader.read().also { ch = it } != -1 && ch.toChar() != ',') {}

                    sb.setLength(0)
                    while (reader.read().also { ch = it } != -1) {
                        val c = ch.toChar()
                        if (c == ']') break
                        if (!c.isWhitespace()) sb.append(c)
                    }
                    val score = sb.toString().toFloatOrNull() ?: 0.0f

                    pieceToId[piece] = id
                    scores.add(score)
                    id++
                }
            }

            return E5Tokenizer(pieceToId, scores.toFloatArray())
        }
    }

    /**
     * Encodes text into token IDs including BOS (0) and EOS (2), truncated to 512 tokens.
     */
    fun encode(text: String): LongArray {
        val s = "\u2581" + text.replace(" ", "\u2581")
        val n = s.length
        val bestScore = FloatArray(n + 1) { Float.NEGATIVE_INFINITY }
        val bestPrev = IntArray(n + 1) { -1 }
        bestScore[0] = 0.0f

        for (i in 0 until n) {
            if (bestScore[i] == Float.NEGATIVE_INFINITY) continue
            val maxL = minOf(MAX_PIECE_LENGTH, n - i)
            for (l in 1..maxL) {
                val sub = s.substring(i, i + l)
                val id = pieceToId[sub]
                if (id != null) {
                    val sc = bestScore[i] + pieceToScore[id]
                    if (sc > bestScore[i + l]) {
                        bestScore[i + l] = sc
                        bestPrev[i + l] = i
                    }
                }
            }
        }

        val tokens = mutableListOf<Long>()
        var curr = n
        while (curr > 0) {
            val prev = bestPrev[curr]
            if (prev == -1) {
                curr--
                tokens.add(UNK_TOKEN_ID)
                continue
            }
            val sub = s.substring(prev, curr)
            val id = pieceToId[sub] ?: UNK_TOKEN_ID.toInt()
            tokens.add(id.toLong())
            curr = prev
        }
        tokens.reverse()

        // Truncate to MAX_SEQUENCE_LENGTH - 2 for BOS and EOS
        val maxInner = MAX_SEQUENCE_LENGTH - 2
        val truncated = if (tokens.size > maxInner) tokens.subList(0, maxInner) else tokens

        val result = LongArray(truncated.size + 2)
        result[0] = BOS_TOKEN_ID
        for (i in truncated.indices) {
            result[i + 1] = truncated[i]
        }
        result[result.size - 1] = EOS_TOKEN_ID
        return result
    }
}
