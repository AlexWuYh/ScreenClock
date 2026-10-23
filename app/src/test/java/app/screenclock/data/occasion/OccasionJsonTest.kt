package app.screenclock.data.occasion

import app.screenclock.domain.occasion.Occasion
import app.screenclock.domain.occasion.OccasionIcon
import app.screenclock.domain.occasion.OccasionKind
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OccasionJsonTest {
    @Test
    fun roundTripKeepsLunarLeapAsABoolean() {
        val item = Occasion(
            id = "id-1",
            title = "妈妈\"生日",
            kind = OccasionKind.LunarYearly,
            year = null,
            month = 2,
            day = 30,
            leap = true,
            message = "到了",
            pinned = true,
        )
        val text = encodeOccasions(listOf(item))
        assertFalse(text.contains("-2"))
        assertTrue(text.contains("\"leap\":true"))
        assertFalse(text.contains("\"year\""))
        assertEquals(listOf(item), decodeOccasions(text))
    }

    @Test
    fun outOfRangeFieldsDropTheRowButKeepTheOthers() {
        val text = """
            {"items":[
                {"id":"bad-month","title":"x","kind":"solar_yearly","month":0,"day":1,"leap":false,"message":"","pinned":false},
                {"id":"bad-day","title":"y","kind":"lunar_yearly","month":1,"day":31,"leap":false,"message":"","pinned":false},
                {"id":"good","title":"z","kind":"solar_yearly","month":5,"day":28,"leap":false,"message":"","pinned":false}
            ]}
        """.trimIndent()
        assertEquals(listOf("good"), decodeOccasions(text).map { it.id })
    }

    @Test
    fun solarOnceKeepsItsYearAndYearlyOmitsIt() {
        val once = Occasion(
            id = "once",
            title = "考试",
            kind = OccasionKind.SolarOnce,
            year = 2026,
            month = 1,
            day = 1,
            leap = false,
            message = "到了",
            pinned = false,
        )
        val decoded = decodeOccasions(encodeOccasions(listOf(once)))
        assertEquals(listOf(once), decoded)
    }

    @Test
    fun aBrokenFileOrANegativeMonthDoesNotCrash() {
        assertEquals(emptyList<Occasion>(), decodeOccasions("{"))
        assertEquals(emptyList<Occasion>(), decodeOccasions(""))
        val negative = """
            {"items":[{"id":"bad","title":"闰","kind":"lunar_yearly","month":-2,"day":1,"leap":true,"message":"到了","pinned":false}]}
        """.trimIndent()
        assertEquals(emptyList<Occasion>(), decodeOccasions(negative))
    }

    @Test
    fun impossibleSolarDatesAreDroppedAndOnlyTheFirstPinSurvives() {
        val text = """{"items":[
            {"id":"a","title":"A","kind":"solar_yearly","month":4,"day":31,"pinned":false},
            {"id":"b","title":"B","kind":"solar_yearly","month":2,"day":29,"pinned":true},
            {"id":"c","title":"C","kind":"solar_yearly","month":3,"day":1,"pinned":true}
        ]}"""
        val decoded = decodeOccasions(text)
        assertEquals(listOf("b", "c"), decoded.map { it.id })
        assertEquals(listOf(true, false), decoded.map { it.pinned })
    }

    @Test
    fun theIconSurvivesARoundTripAndOldFilesGetTheDefault() {
        val item = Occasion(
            id = "i", title = "毕业", kind = OccasionKind.SolarYearly, year = null, month = 6, day = 30,
            leap = false, message = "到了", pinned = false, icon = OccasionIcon.Cap,
        )
        assertEquals(OccasionIcon.Cap, decodeOccasions(encodeOccasions(listOf(item))).single().icon)
        val old = """{"items":[{"id":"o","title":"老数据","kind":"solar_yearly","month":1,"day":1,"pinned":false}]}"""
        assertEquals(OccasionIcon.Default, decodeOccasions(old).single().icon)
    }

    @Test
    fun theFileReplacesItself() {
        val dir = File(createTempDir(), "occasions")
        val repository = OccasionRepository(dir)
        val item = Occasion(
            id = "file",
            title = "跨年",
            kind = OccasionKind.SolarYearly,
            year = null,
            month = 1,
            day = 1,
            leap = false,
            message = "到了",
            pinned = true,
        )
        repository.save(listOf(item))
        repository.save(emptyList())
        assertEquals(emptyList<Occasion>(), repository.load())
        repository.save(listOf(item))
        assertEquals(listOf(item), repository.load())
        dir.deleteRecursively()
    }
}

private fun createTempDir(): File {
    val dir = kotlin.io.path.createTempDirectory("screenclock-occasions").toFile()
    return dir
}
