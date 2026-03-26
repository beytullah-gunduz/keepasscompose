package org.github.keepasscompose.core.common

import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxGroup
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * Analyzes password health across a KeePass database.
 *
 * Detects:
 * - Weak passwords (low entropy / common patterns)
 * - Reused passwords across multiple entries
 * - Expired entries
 * - Entries expiring soon (configurable threshold)
 */
object PasswordHealthAnalyzer {

    /**
     * An individual entry's health issues.
     */
    data class EntryHealth(
        val entry: KdbxEntry,
        val groupPath: List<String>,
        val strength: PasswordStrength.Result,
        val isWeak: Boolean,
        val isReused: Boolean,
        val reusedCount: Int,
        val isExpired: Boolean,
        val isExpiringSoon: Boolean,
    )

    /**
     * Summary report for the entire database.
     */
    data class HealthReport(
        val totalEntries: Int,
        val entriesWithPasswords: Int,
        val weakPasswords: List<EntryHealth>,
        val reusedPasswords: Map<String, List<EntryHealth>>,
        val expiredEntries: List<EntryHealth>,
        val expiringSoonEntries: List<EntryHealth>,
        val averageStrength: PasswordStrength.Level,
        val entries: List<EntryHealth>,
    )

    /**
     * Analyze all entries in the database and produce a health report.
     *
     * @param rootGroup The root group to analyze recursively.
     * @param now The current time for expiry checks.
     * @param expiringSoonThreshold How far ahead to look for upcoming expiries.
     * @param weakThreshold Strength level at or below which a password is considered weak.
     */
    fun analyze(
        rootGroup: KdbxGroup,
        now: Instant = kotlin.time.Clock.System.now(),
        expiringSoonThreshold: Duration = 30.days,
        weakThreshold: PasswordStrength.Level = PasswordStrength.Level.WEAK,
    ): HealthReport {
        // Collect all entries with group paths
        val allEntries = collectEntries(rootGroup, emptyList())

        // Build password → entries map for reuse detection
        val passwordMap = mutableMapOf<String, MutableList<Pair<KdbxEntry, List<String>>>>()
        for ((entry, path) in allEntries) {
            val password = entry.password
            if (password.isNotEmpty()) {
                passwordMap.getOrPut(password) { mutableListOf() }.add(entry to path)
            }
        }

        val reusedPasswords = passwordMap.filter { it.value.size > 1 }
        val reusedEntryUuids = reusedPasswords.values.flatten().map { it.first.uuid }.toSet()

        // Analyze each entry
        val entryHealthList = allEntries.map { (entry, path) ->
            val password = entry.password
            val strength = if (password.isNotEmpty()) {
                PasswordStrength.estimate(password)
            } else {
                PasswordStrength.Result(0.0, PasswordStrength.Level.VERY_WEAK, 0.0, "instant", emptyList())
            }

            val isWeak = password.isNotEmpty() && strength.level <= weakThreshold
            val isReused = entry.uuid in reusedEntryUuids
            val reusedCount = if (isReused) {
                passwordMap[password]?.size ?: 0
            } else 0

            val isExpired = entry.expires && entry.expiryTime != null && entry.expiryTime <= now
            val isExpiringSoon = entry.expires && entry.expiryTime != null &&
                !isExpired && entry.expiryTime <= now + expiringSoonThreshold

            EntryHealth(
                entry = entry,
                groupPath = path,
                strength = strength,
                isWeak = isWeak,
                isReused = isReused,
                reusedCount = reusedCount,
                isExpired = isExpired,
                isExpiringSoon = isExpiringSoon,
            )
        }

        // Build reused passwords map for the report
        val reusedReport = reusedPasswords.mapValues { (_, entries) ->
            entries.map { (entry, path) ->
                entryHealthList.first { it.entry.uuid == entry.uuid }
            }
        }

        // Average strength
        val withPasswords = entryHealthList.filter { it.entry.password.isNotEmpty() }
        val avgLevel = if (withPasswords.isEmpty()) {
            PasswordStrength.Level.VERY_WEAK
        } else {
            val avgOrdinal = withPasswords.map { it.strength.level.ordinal }.average()
            PasswordStrength.Level.entries[avgOrdinal.toInt().coerceIn(0, PasswordStrength.Level.entries.size - 1)]
        }

        return HealthReport(
            totalEntries = allEntries.size,
            entriesWithPasswords = withPasswords.size,
            weakPasswords = entryHealthList.filter { it.isWeak },
            reusedPasswords = reusedReport,
            expiredEntries = entryHealthList.filter { it.isExpired },
            expiringSoonEntries = entryHealthList.filter { it.isExpiringSoon },
            averageStrength = avgLevel,
            entries = entryHealthList,
        )
    }

    private fun collectEntries(
        group: KdbxGroup,
        path: List<String>,
    ): List<Pair<KdbxEntry, List<String>>> {
        val currentPath = path + group.name
        val results = mutableListOf<Pair<KdbxEntry, List<String>>>()
        for (entry in group.entries) {
            results.add(entry to currentPath)
        }
        for (subgroup in group.groups) {
            results.addAll(collectEntries(subgroup, currentPath))
        }
        return results
    }
}
