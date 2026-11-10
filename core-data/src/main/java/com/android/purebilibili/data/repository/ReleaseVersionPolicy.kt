package com.android.purebilibili.data.repository

/** Shared semantic version and prerelease ordering; independent version codes stay package-scoped. */
object ReleaseVersionPolicy {
    fun normalizeVersion(version: String): String {
        return version
            .trim()
            .removePrefix("v")
            .removePrefix("V")
            .trim()
    }

    fun isRemoteNewer(localVersion: String, remoteVersion: String): Boolean {
        return compareVersions(
            localVersion = normalizeVersion(localVersion),
            remoteVersion = normalizeVersion(remoteVersion)
        ) < 0
    }

    fun parseVersionParts(version: String): List<Int> {
        if (version.isBlank()) return emptyList()
        return version
            .split('.')
            .mapNotNull { part -> part.toIntOrNull() }
    }

    private data class ParsedVersion(
        val numericParts: List<Int>,
        val stabilityRank: Int,
        val qualifierNumber: Int
    )

    private fun parseComparableVersion(version: String): ParsedVersion {
        val normalized = normalizeVersion(version)
        val match = Regex(
            pattern = """^(\d+(?:\.\d+)*)(?:[\s._-]*(alpha|beta|rc)[\s._-]*(\d+)?)?$""",
            option = RegexOption.IGNORE_CASE
        ).matchEntire(normalized)
        if (match != null) {
            val numeric = parseVersionParts(match.groupValues[1])
            val qualifier = match.groupValues[2].lowercase()
            val qualifierNumber = match.groupValues[3].toIntOrNull() ?: 0
            val stabilityRank = when (qualifier) {
                "alpha" -> 0
                "beta" -> 1
                "rc" -> 2
                else -> 3
            }
            return ParsedVersion(
                numericParts = numeric,
                stabilityRank = stabilityRank,
                qualifierNumber = qualifierNumber
            )
        }

        val numericPrefix = normalized
            .takeWhile { it.isDigit() || it == '.' }
            .trimEnd('.')
        return ParsedVersion(
            numericParts = parseVersionParts(numericPrefix),
            stabilityRank = 3,
            qualifierNumber = 0
        )
    }

    private fun compareVersions(localVersion: String, remoteVersion: String): Int {
        val local = parseComparableVersion(localVersion)
        val remote = parseComparableVersion(remoteVersion)
        val maxSize = maxOf(local.numericParts.size, remote.numericParts.size)
        for (index in 0 until maxSize) {
            val localPart = local.numericParts.getOrElse(index) { 0 }
            val remotePart = remote.numericParts.getOrElse(index) { 0 }
            if (localPart != remotePart) {
                return localPart.compareTo(remotePart)
            }
        }
        if (local.stabilityRank != remote.stabilityRank) {
            return local.stabilityRank.compareTo(remote.stabilityRank)
        }
        return local.qualifierNumber.compareTo(remote.qualifierNumber)
    }

}
