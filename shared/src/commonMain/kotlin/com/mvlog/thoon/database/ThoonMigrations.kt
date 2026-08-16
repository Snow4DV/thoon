package com.mvlog.thoon.database

import androidx.room.migration.Migration

/**
 * Migrations for [ThoonDatabase], in version order.
 *
 * Shared across every feature: because all tables live in one file, a missing migration breaks the
 * whole app rather than one feature. Destructive fallback is deliberately not configured, so an
 * omission fails loudly instead of discarding unrelated features' data.
 */
object ThoonMigrations {

    val ALL: List<Migration> = emptyList()
}
