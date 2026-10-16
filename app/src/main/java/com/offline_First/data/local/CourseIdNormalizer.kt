package com.offline_First.data.local

import com.offline_First.domain.model.CourseDetail
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe registry that normalizes course identifiers and resolves aliases
 * between local course IDs (e.g. "c-cpp"), canonical slugs (e.g. "cpp-programming"),
 * and actual backend UUIDs discovered at runtime.
 *
 * Safeguards:
 * - Never guesses or hardcodes arbitrary UUIDs.
 * - Aliases are registered dynamically from actual local repository records and remote API DTOs.
 */
object CourseIdNormalizer {

    // Maps each known alias / ID to a shared cluster of equivalent identifiers.
    private val aliasClusters = ConcurrentHashMap<String, MutableSet<String>>()

    init {
        // Pre-register canonical local course ID and slug pairs
        registerPair("c-cpp", "cpp-programming")
        registerPair("c-cpp", "cpp")
        registerPair("c-py", "python-for-beginners")
        registerPair("c-py", "python")
        registerPair("c-dsa", "data-structures-and-algorithms")
        registerPair("c-dsa", "dsa")
        registerPair("c-fs", "full-stack-web-development")
        registerPair("c-fs", "full-stack")
        registerPair("c-ml", "machine-learning-essentials")
        registerPair("c-ml", "machine-learning")
        registerPair("c-devops", "devops-and-cloud-engineering")
        registerPair("c-devops", "devops")
        registerPair("c-sde", "sde-system-design-and-architecture")
        registerPair("c-sde", "sde")
    }

    /**
     * Converts a title or name into a canonical slug format.
     */
    fun toSlug(name: String): String {
        return name.lowercase(Locale.US)
            .replace("+", "p")
            .replace("&", "and")
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
    }

    /**
     * Registers a bidirectional alias between two identifiers.
     */
    fun registerPair(idA: String, idB: String) {
        val a = idA.trim()
        val b = idB.trim()
        if (a.isEmpty() || b.isEmpty() || a.equals(b, ignoreCase = true)) {
            if (a.isNotEmpty()) {
                aliasClusters.computeIfAbsent(a) { ConcurrentHashMap.newKeySet() }.add(a)
            }
            return
        }

        synchronized(aliasClusters) {
            val clusterA = aliasClusters[a]
            val clusterB = aliasClusters[b]

            val merged = when {
                clusterA != null && clusterB != null -> {
                    clusterA.addAll(clusterB)
                    clusterA
                }
                clusterA != null -> {
                    clusterA.add(b)
                    clusterA
                }
                clusterB != null -> {
                    clusterB.add(a)
                    clusterB
                }
                else -> {
                    val newSet = ConcurrentHashMap.newKeySet<String>()
                    newSet.add(a)
                    newSet.add(b)
                    newSet
                }
            }

            for (member in merged) {
                aliasClusters[member] = merged
            }
        }
    }

    /**
     * Registers an actual local CourseDetail record.
     */
    fun registerLocalCourse(detail: CourseDetail) {
        val localId = detail.id
        registerPair(localId, toSlug(detail.name))
        if (detail.category.isNotBlank()) {
            registerPair(localId, toSlug(detail.category))
        }
    }

    /**
     * Registers an actual remote course record dynamically from API response / cache.
     * Guarantees no guessed or fabricated UUIDs are used.
     */
    fun registerRemoteCourse(id: String, slug: String?, name: String?) {
        val trimmedId = id.trim()
        if (trimmedId.isEmpty()) return

        if (!slug.isNullOrBlank()) {
            registerPair(trimmedId, slug.trim())
        }
        if (!name.isNullOrBlank()) {
            registerPair(trimmedId, toSlug(name))
        }
    }

    /**
     * Retrieves all known aliases for the given identifier, including the identifier itself.
     */
    fun getAllAliases(id: String): Set<String> {
        val trimmed = id.trim()
        val cluster = aliasClusters[trimmed]
        return if (cluster != null) {
            cluster.toSet()
        } else {
            setOf(trimmed)
        }
    }

    /**
     * Resolves an identifier (such as a slug or backend UUID) to its corresponding
     * local repository ID starting with "c-", if one is registered.
     */
    fun resolveToLocalId(id: String): String? {
        val aliases = getAllAliases(id)
        return aliases.firstOrNull { it.startsWith("c-") }
    }

    /**
     * Determines whether two identifiers represent the exact same course.
     */
    fun isSameCourse(id1: String, id2: String): Boolean {
        if (id1.equals(id2, ignoreCase = true)) return true
        val aliases1 = getAllAliases(id1)
        return aliases1.contains(id2.trim())
    }

    /**
     * Clears dynamic alias clusters (useful for testing).
     */
    internal fun resetForTesting() {
        aliasClusters.clear()
        registerPair("c-cpp", "cpp-programming")
        registerPair("c-cpp", "cpp")
        registerPair("c-py", "python-for-beginners")
        registerPair("c-py", "python")
        registerPair("c-dsa", "data-structures-and-algorithms")
        registerPair("c-dsa", "dsa")
        registerPair("c-fs", "full-stack-web-development")
        registerPair("c-fs", "full-stack")
        registerPair("c-ml", "machine-learning-essentials")
        registerPair("c-ml", "machine-learning")
        registerPair("c-devops", "devops-and-cloud-engineering")
        registerPair("c-devops", "devops")
        registerPair("c-sde", "sde-system-design-and-architecture")
        registerPair("c-sde", "sde")
    }
}
