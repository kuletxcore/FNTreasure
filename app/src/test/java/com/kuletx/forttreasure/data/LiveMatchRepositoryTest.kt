package com.kuletx.forttreasure.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kuletx.forttreasure.data.local.AppDatabase
import com.kuletx.forttreasure.data.local.ChestCounterRepository
import com.kuletx.forttreasure.data.local.SettingsRepository
import com.kuletx.forttreasure.domain.OverlayModel
import com.kuletx.forttreasure.domain.Profiles
import com.kuletx.forttreasure.domain.TreasureCatalog
import com.kuletx.forttreasure.domain.TreasureFindDto
import com.kuletx.forttreasure.overlay.OverlayPermissionHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LiveMatchRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var repo: ChestCounterRepository

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        TreasureCatalog.init(context)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settings = SettingsRepository(context)
        settings.resetAll()
        repo = ChestCounterRepository(context, db, settings)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `embedded content json loads all 7 loot kinds and weeks`() {
        val data = TreasureCatalog.data
        assertTrue(data.revision >= 3)
        assertEquals(7, data.odds.size)
        assertTrue(data.weeks.isNotEmpty())
        assertNotNull(TreasureCatalog.find("DelilahPrize:dpid_item3"))
    }

    @Test
    fun `overlay permission intent targets runtime package`() {
        val intent = OverlayPermissionHelper.createManageOverlayIntent(context)
        assertEquals("package:${context.packageName}", intent.dataString)
    }

    @Test
    fun `live match counts 15 chests records Jam Track and Gizmo as Match 1 Session 1`() = runBlocking {
        repeat(15) {
            repo.incrementLiveChest(1, "en")
        }
        repo.addLiveTreasure(
            itemName = "No Trespassing",
            prizeId = "DelilahPrize:dpid_item3",
            fromGoldenChest = true,
            profileKey = "",
            lang = "en",
        )
        repo.addLiveTreasure(
            itemName = "Gizmo",
            prizeId = null,
            fromGoldenChest = false,
            profileKey = "",
            lang = "en",
        )

        val liveBeforeFinish = settings.liveMatchFlow.first()
        assertTrue(liveBeforeFinish.active)
        assertEquals(15, liveBeforeFinish.chests)
        assertEquals(2, liveBeforeFinish.treasures.size)

        val saved = repo.finishLiveMatch(
            profileKey = "",
            playerName = "KuletXCore",
            lang = "en",
        )
        assertNotNull(saved)
        assertEquals(1, saved!!.matchNumber)
        assertEquals(1, saved.sessionNumber)
        assertEquals(15, saved.chestsOpened)
        assertEquals(2, saved.treasureFinds.size)

        val liveAfterFinish = settings.liveMatchFlow.first()
        assertFalse(liveAfterFinish.active)
        assertEquals(0, liveAfterFinish.chests)
        assertTrue(liveAfterFinish.treasures.isEmpty())

        val obtained = repo.obtainedPrizesFlow.first()
        assertTrue(obtained.values.flatten().contains("w1-jam"))

        val matches = repo.matchesFlow.first()
        assertEquals(1, matches.size)
        val view = Profiles.buildView(matches, emptyMap(), obtained, null, "en")
        val cfg = settings.configFlow.first()
        val ov = OverlayModel.buildLive(view, liveAfterFinish, sessionNumber = cfg.sessionNumber, sessionStartedEpochMs = cfg.sessionStartedEpochMs, lang = "en")
        assertEquals("15", ov.last)
        assertEquals("15", ov.session)
        assertEquals("2", ov.treasures)

        // Start Session 2 -> Match resets back to Match 1 in Session 2
        repo.startNewSession("en")
        val cfg2 = settings.configFlow.first()
        assertEquals(2, cfg2.sessionNumber)
        repo.incrementLiveChest(5, "en")
        val savedS2 = repo.finishLiveMatch(profileKey = "", playerName = "KuletXCore", lang = "en")
        assertNotNull(savedS2)
        assertEquals(2, savedS2!!.sessionNumber)
        assertEquals(1, savedS2.matchNumber)
        assertEquals(5, savedS2.chestsOpened)
    }

    @Test
    fun `updateMatchDetails lets user fix chest count and treasures in log`() = runBlocking {
        repo.incrementLiveChest(9, "en")
        val saved = repo.finishLiveMatch(profileKey = "", playerName = "KuletXCore", lang = "en")!!

        repo.updateMatchDetails(
            sessionId = saved.sessionId,
            chestsOpened = 15,
            treasureFinds = listOf(
                TreasureFindDto(item = "No Trespassing", prizeId = "DelilahPrize:dpid_item3", fromGoldenChest = true),
                TreasureFindDto(item = "Gizmo"),
            ),
            matchNumber = 1,
            sessionNumber = 1,
            profileKey = "KuletXCore",
            lang = "en",
        )

        val updated = repo.matchesFlow.first().first { it.sessionId == saved.sessionId }
        assertEquals(15, updated.chestsOpened)
        assertEquals(2, updated.treasureFinds.size)
        assertEquals(1, updated.matchNumber)
        assertEquals(1, updated.sessionNumber)
    }

    @Test
    fun `importDataJson and exportDataJson preserve desktop schema without replay support`() = runBlocking<Unit> {
        val desktopJson = """
            {
              "Version": 1,
              "AnalyzerVersion": 17,
              "CreatedUtc": "2026-04-01T12:00:00Z",
              "Matches": [
                {
                  "SessionId": "pc_sess_1",
                  "ReplayFile": "UnsavedReplay-2026.04.01.replay",
                  "AnalyzedAtUtc": "2026-04-01T12:00:00Z",
                  "MatchStartUtc": "2026-04-01T11:30:00Z",
                  "Playlist": "experience_br",
                  "PlayerName": "KuletXCore",
                  "IsReplayOwner": true,
                  "ChestsOpened": 11
                }
              ],
              "ProcessedFiles": {
                "UnsavedReplay-2026.04.01.replay|4096": "pc_sess_1"
              },
              "ManualChests": {
                "KuletXCore": 4
              },
              "ManualHistory": {
                "KuletXCore": [
                  {
                    "Id": "m1",
                    "Chests": 4,
                    "AddedUtc": "2026-04-01T12:05:00Z",
                    "Kind": "hand"
                  }
                ]
              },
              "Obtained": {
                "KuletXCore": ["w1-jam"]
              }
            }
        """.trimIndent()

        val tempFile = java.io.File(context.cacheDir, "desktop_data.json").apply { writeText(desktopJson) }
        try {
            val importedCount = repo.importDataJson(android.net.Uri.fromFile(tempFile), "en")
            assertEquals(1, importedCount)

            val matches = repo.matchesFlow.first()
            assertEquals(1, matches.size)
            assertEquals("pc_sess_1", matches.first().sessionId)
            assertEquals("UnsavedReplay-2026.04.01.replay", matches.first().replayFile)
            assertEquals(11, matches.first().chestsOpened)

            val exported = repo.exportDataJson()
            assertTrue(exported.contains("\"SessionId\": \"pc_sess_1\""))
            assertTrue(exported.contains("\"ReplayFile\": \"UnsavedReplay-2026.04.01.replay\""))
            assertTrue(exported.contains("\"ProcessedFiles\""))
            assertTrue(exported.contains("\"w1-jam\""))
        } finally {
            tempFile.delete()
        }
    }
}
