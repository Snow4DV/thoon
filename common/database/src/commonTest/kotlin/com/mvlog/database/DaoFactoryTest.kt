package com.mvlog.database

import com.mvlog.database.dao.MutableDaoFactory
import com.mvlog.database.dao.ThoonDao
import com.mvlog.database.dao.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class DaoFactoryTest {

    private interface FirstDao : ThoonDao
    private interface SecondDao : ThoonDao

    private class FirstDaoImpl : FirstDao
    private class SecondDaoImpl : SecondDao

    @Test
    fun resolvesEachDaoByItsOwnType() {
        val factory = MutableDaoFactory()
        val first = FirstDaoImpl()
        val second = SecondDaoImpl()
        factory.register(FirstDao::class) { first }
        factory.register(SecondDao::class) { second }

        assertSame(first, factory.get<FirstDao>())
        assertSame(second, factory.get<SecondDao>())
    }

    @Test
    fun registrationDoesNotBuildTheDao() {
        val factory = MutableDaoFactory()
        var built = 0
        factory.register(FirstDao::class) { built++; FirstDaoImpl() }

        assertEquals(0, built, "registering must not open the database")

        factory.get<FirstDao>()
        factory.get<FirstDao>()

        assertEquals(1, built, "the resolved DAO should be reused, not rebuilt")
    }

    @Test
    fun missingRegistrationFailsWithTheDaoName() {
        val factory = MutableDaoFactory()

        val error = assertFailsWith<IllegalStateException> { factory.get<FirstDao>() }

        assertEquals(true, error.message?.contains("FirstDao"))
    }
}
