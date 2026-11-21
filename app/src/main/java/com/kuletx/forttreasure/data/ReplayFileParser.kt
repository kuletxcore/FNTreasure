package com.kuletx.forttreasure.data

import com.kuletx.forttreasure.domain.GameModes
import com.kuletx.forttreasure.domain.MatchRecordDto
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

sealed interface ReplayParseOutcome {
    data class Parsed(
        val match: MatchRecordDto,
        val hasCompressedDataChunks: Boolean,
        val isPartial: Boolean,
        val sessionId: String,
    ) : ReplayParseOutcome

    data class Skipped(val reason: String) : ReplayParseOutcome
    data class Failed(val error: String) : ReplayParseOutcome
}

/**
 * Binary Unreal Engine `.replay` parser ported from `lib/Unreal.Core/ReplayReader.cs` and
 * `lib/FortniteReplayReader/FortniteReplayReader.cs`.
 *
 * Parses ReplayInfo (`FileMagic 0x1CA2E27F`), Header chunk (`NetworkMagic 0x2CF5A13D`, branch,
 * UE version, map list via `LevelNamesAndTimes`), and AES-ECB encrypted Event chunks (`teamStats`,
 * `playerStats`) while detecting live/partial replays and validating `GameModes.counts` (including
 * `experience_br` and `/Delilah/Maps/Delilah_Terrain`).
 */
object ReplayFileParser {
    private const val FILE_MAGIC = 0x1CA2E27F
    private const val NETWORK_MAGIC = 0x2CF5A13D

    private const val CHUNK_HEADER = 0
    private const val CHUNK_REPLAY_DATA = 1
    private const val CHUNK_CHECKPOINT = 2
    private const val CHUNK_EVENT = 3

    // ReplayVersionHistory ordinals
    private const val HISTORY_RECORDED_TIMESTAMP = 5
    private const val HISTORY_COMPRESSION = 6
    private const val HISTORY_ENCRYPTION = 7
    private const val HISTORY_CUSTOM_VERSIONS = 8

    // NetworkVersionHistory ordinals
    private const val NET_HISTORY_EXTRA_VERSION = 1
    private const val NET_HISTORY_SAVE_FULL_ENGINE_VERSION = 9
    private const val NET_HISTORY_HEADER_GUID = 11
    private const val NET_HISTORY_MULTIPLE_LEVELS = 14
    private const val NET_HISTORY_HEADER_FLAGS = 16
    private const val NET_HISTORY_SAVE_PACKAGE_VERSION_UE = 21
    private const val NET_HISTORY_RECORDING_METADATA = 22
    private const val NET_HISTORY_USE_CUSTOM_VERSION = 23

    fun parse(fileName: String, bytes: ByteArray, lang: String): ReplayParseOutcome {
        return try {
            val r = Reader(bytes)
            if (r.remaining() < 16) return ReplayParseOutcome.Failed("File too small")
            val magic = r.readInt32()
            if (magic != FILE_MAGIC) {
                return ReplayParseOutcome.Failed("Invalid replay magic (0x${magic.toUInt().toString(16)})")
            }
            val fileVersion = r.readInt32()
            if (fileVersion >= HISTORY_CUSTOM_VERSIONS) {
                val customCount = r.readInt32()
                if (customCount < 0 || customCount > 4096) return ReplayParseOutcome.Failed("Corrupt custom versions")
                r.skip(customCount * 20)
            }
            val lengthInMs = r.readUInt32()
            /* networkVersion = */ r.readInt32()
            /* changelist = */ r.readInt32()
            val friendlyName = r.readFString()
            val isLive = r.readUInt32() != 0L

            var timestampUtc: Instant = Instant.now()
            if (fileVersion >= HISTORY_RECORDED_TIMESTAMP) {
                val dotnetTicks = r.readInt64()
                timestampUtc = dotNetBinaryToInstant(dotnetTicks) ?: Instant.now()
            }
            var isCompressed = false
            if (fileVersion >= HISTORY_COMPRESSION) {
                isCompressed = r.readUInt32() != 0L
            }
            var isEncrypted = false
            var encryptionKey = ByteArray(0)
            if (fileVersion >= HISTORY_ENCRYPTION) {
                isEncrypted = r.readUInt32() != 0L
                val keyLen = r.readInt32()
                if (keyLen < 0 || keyLen > 256) return ReplayParseOutcome.Failed("Invalid key size")
                encryptionKey = r.readBytes(keyLen)
            }

            if (isEncrypted && encryptionKey.isEmpty()) {
                return ReplayParseOutcome.Failed("Replay marked encrypted with no key")
            }

            var branch: String? = null
            var mapName: String? = null
            var placement: Int? = null
            var totalPlayers: Int? = null
            var eliminations: Int? = null
            var headerSessionId: String? = null
            var hasCompressedDataChunks = false
            var isPartial = false

            while (r.remaining() > 0) {
                if (r.remaining() < 8) {
                    isPartial = true
                    break
                }
                val chunkType = r.readInt32()
                val chunkSize = r.readInt32()
                val chunkStart = r.pos
                if (chunkSize <= 0) {
                    isPartial = true
                    break
                }
                if (chunkStart + chunkSize > bytes.size) {
                    isPartial = true
                    break
                }

                when (chunkType) {
                    CHUNK_HEADER -> {
                        val hr = Reader(bytes, chunkStart, chunkStart + chunkSize)
                        val netMagic = hr.readInt32()
                        if (netMagic == NETWORK_MAGIC) {
                            val netVersion = hr.readInt32()
                            if (netVersion > NET_HISTORY_EXTRA_VERSION) {
                                if (netVersion >= NET_HISTORY_USE_CUSTOM_VERSION) {
                                    val cCount = hr.readInt32()
                                    if (cCount in 0..4096) hr.skip(cCount * 20)
                                }
                                /* netChecksum = */ hr.readInt32()
                                /* engNetVer = */ hr.readInt32()
                                /* gameNetProto = */ hr.readInt32()
                                if (netVersion >= NET_HISTORY_HEADER_GUID) {
                                    headerSessionId = hr.readGuidHex()
                                }
                                if (netVersion >= NET_HISTORY_SAVE_FULL_ENGINE_VERSION) {
                                    hr.readUInt16() // major
                                    hr.readUInt16() // minor
                                    hr.readUInt16() // patch
                                    hr.readInt32() // changelist
                                    branch = hr.readFString()
                                } else {
                                    hr.readInt32()
                                }
                                if (netVersion >= NET_HISTORY_RECORDING_METADATA) {
                                    hr.skip(12)
                                }
                                if (netVersion > NET_HISTORY_MULTIPLE_LEVELS) {
                                    val levelCount = hr.readInt32()
                                    if (levelCount in 0..128) {
                                        for (i in 0 until levelCount) {
                                            val level = hr.readFString()
                                            hr.readInt32()
                                            if (mapName == null && level.isNotBlank()) {
                                                mapName = level
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    CHUNK_EVENT -> {
                        val er = Reader(bytes, chunkStart, chunkStart + chunkSize)
                        /* id = */ er.readFString()
                        val group = er.readFString()
                        val metadata = er.readFString()
                        /* startTime = */ er.readInt32()
                        /* endTime = */ er.readInt32()
                        val eventBytesLen = er.readInt32()
                        if (eventBytesLen > 0 && eventBytesLen <= er.remaining()) {
                            val payload = er.readBytes(eventBytesLen)
                            val decrypted = if (isEncrypted && encryptionKey.isNotEmpty()) {
                                decryptAesEcb(payload, encryptionKey)
                            } else payload
                            if (decrypted != null) {
                                val dr = Reader(decrypted)
                                when (metadata) {
                                    "AthenaTeamStats" -> if (dr.remaining() >= 12) {
                                        dr.readInt32() // unknown
                                        placement = dr.readInt32()
                                        totalPlayers = dr.readInt32()
                                    }
                                    "AthenaMatchStats" -> if (dr.remaining() >= 16) {
                                        dr.readInt32() // unknown
                                        dr.readInt32() // accuracy float
                                        dr.readInt32() // assists
                                        eliminations = dr.readInt32()
                                    }
                                }
                                if (group == "PlayerElim") {
                                    // Eliminations parsed when present
                                }
                            }
                        }
                    }

                    CHUNK_REPLAY_DATA -> {
                        if (isCompressed) {
                            hasCompressedDataChunks = true
                        }
                    }
                }
                r.pos = chunkStart + chunkSize
            }

            val gameVersion = parseBranchVersion(branch)
            val inferredPlaylist = when {
                friendlyName.contains("experience_br", ignoreCase = true) -> "experience_br"
                friendlyName.contains("Zero Build", ignoreCase = true) -> "Playlist_NoBuildBR_Solo"
                else -> "Playlist_DefaultSolo"
            }

            val sessionId = headerSessionId ?: sha256Short(fileName + "_" + timestampUtc.toEpochMilli())
            val record = MatchRecordDto(
                sessionId = sessionId,
                replayFile = fileName,
                analyzedAtUtc = Instant.now().toString(),
                matchStartUtc = timestampUtc.toString(),
                durationSeconds = (lengthInMs / 1000L).toInt(),
                playlist = inferredPlaylist,
                gameVersion = gameVersion,
                playerName = friendlyName.takeIf { it.isNotBlank() && !it.startsWith("Unsaved", ignoreCase = true) },
                placement = placement,
                totalPlayers = totalPlayers,
                eliminations = eliminations,
                chestsOpened = 0,
                analyzerVersion = 17,
                isBattleRoyale = GameModes.isBattleRoyalePlaylist(inferredPlaylist),
                map = mapName,
                isCustomMatch = false,
                notes = if (hasCompressedDataChunks) {
                    "Oodle Mermaid ReplayData chunk detected (desktop desktop ChestCounter extracts chest actors; edit chests or import data.json)"
                } else null,
            )

            if (!GameModes.counts(record)) {
                val why = GameModes.whyNot(record, lang) ?: "Non-BR mode"
                return ReplayParseOutcome.Skipped(why)
            }

            ReplayParseOutcome.Parsed(
                match = record,
                hasCompressedDataChunks = hasCompressedDataChunks,
                isPartial = isPartial || isLive,
                sessionId = sessionId,
            )
        } catch (e: Exception) {
            ReplayParseOutcome.Failed(e.message ?: "Parse error")
        }
    }

    private fun decryptAesEcb(cipherText: ByteArray, key: ByteArray): ByteArray? {
        return try {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
            cipher.doFinal(cipherText)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseBranchVersion(branch: String?): String? {
        if (branch.isNullOrBlank()) return null
        val match = Regex("""\+\+Fortnite\+Release-(\d+)\.(\d+)""").find(branch) ?: return branch
        return "${match.groupValues[1]}.${match.groupValues[2]}"
    }

    private fun dotNetBinaryToInstant(binary: Long): Instant? {
        // .NET DateTime.ToBinary stores kind in top 2 bits and 100ns ticks since 0001-01-01T00:00:00Z in lower 62 bits
        val ticks = binary and 0x3FFFFFFFFFFFFFFFL
        if (ticks <= 0L) return null
        val epochTicks = 621355968000000000L // 1970-01-01 in .NET ticks
        val unixSeconds = (ticks - epochTicks) / 10_000_000L
        val nanos = ((ticks - epochTicks) % 10_000_000L) * 100L
        return if (unixSeconds in 1_400_000_000L..4_000_000_000L) {
            Instant.ofEpochSecond(unixSeconds, nanos)
        } else null
    }

    private fun sha256Short(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(StandardCharsets.UTF_8))
        return digest.take(16).joinToString("") { "%02x".format(it) }
    }

    private class Reader(
        private val bytes: ByteArray,
        start: Int = 0,
        private val limit: Int = bytes.size,
    ) {
        var pos: Int = start

        fun remaining(): Int = (limit - pos).coerceAtLeast(0)

        fun skip(count: Int) {
            pos = (pos + count).coerceAtMost(limit)
        }

        fun readInt32(): Int {
            if (remaining() < 4) return 0
            val v = ByteBuffer.wrap(bytes, pos, 4).order(ByteOrder.LITTLE_ENDIAN).int
            pos += 4
            return v
        }

        fun readUInt32(): Long = readInt32().toLong() and 0xFFFFFFFFL

        fun readUInt16(): Int {
            if (remaining() < 2) return 0
            val v = ByteBuffer.wrap(bytes, pos, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
            pos += 2
            return v
        }

        fun readInt64(): Long {
            if (remaining() < 8) return 0L
            val v = ByteBuffer.wrap(bytes, pos, 8).order(ByteOrder.LITTLE_ENDIAN).long
            pos += 8
            return v
        }

        fun readBytes(len: Int): ByteArray {
            if (len <= 0 || remaining() < len) return ByteArray(0)
            val out = bytes.copyOfRange(pos, pos + len)
            pos += len
            return out
        }

        fun readGuidHex(): String {
            val raw = readBytes(16)
            return raw.joinToString("") { "%02x".format(it) }
        }

        fun readFString(): String {
            if (remaining() < 4) return ""
            val length = readInt32()
            if (length == 0) return ""
            if (length > 0) {
                val safeLen = length.coerceAtMost(remaining()).coerceAtMost(65536)
                val raw = readBytes(safeLen)
                return String(raw, StandardCharsets.UTF_8).trimEnd('\u0000')
            } else {
                val charCount = (-length).coerceAtMost(remaining() / 2).coerceAtMost(32768)
                val raw = readBytes(charCount * 2)
                return String(raw, StandardCharsets.UTF_16LE).trimEnd('\u0000')
            }
        }
    }
}
