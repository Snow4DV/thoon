package com.mvlog.agent.impl.di

import com.mvlog.agent.impl.util.AgentClock
import com.mvlog.agent.impl.util.IdGenerator
import com.mvlog.common.network.di.KtorClientComponentHolder
import com.mvlog.common.serialization.di.JsonComponentHolder
import com.mvlog.coroutines.scope.ProcessScope
import com.mvlog.database.dao.DaoFactory
import com.mvlog.database.di.ThoonDatabaseComponentHolder
import kotlinx.coroutines.CoroutineScope
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

/**
 * The graph's inputs: what the module is handed rather than what it builds.
 *
 * Most members are a lookup against another subsystem's holder; the rest are ambient sources — time
 * and identity — that tests replace to make behaviour deterministic. Nothing here is part of the
 * agent's own wiring, so this file stays a readable list of what the module depends on.
 *
 * Resolution happens per access rather than up front, so simply building a component does not force
 * another subsystem to initialise.
 */
internal interface ThoonAgentComponentDependencies {

    val processScope: CoroutineScope
        get() = ProcessScope

    val daoFactory: DaoFactory
        get() = ThoonDatabaseComponentHolder.get().daoFactory()

    val clock: AgentClock
        get() = AgentClock.System

    val idGenerator: IdGenerator
        get() = IdGenerator.Random

    val json: Json
        get() = JsonComponentHolder.get().json()

    val httpClient: HttpClient
        get() = KtorClientComponentHolder.get().httpClient()

    class Impl : ThoonAgentComponentDependencies
}
