package com.ctoadi.notstore.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Local SQLite store. This is the only copy of the data; nothing is synced anywhere. */
class NotificationDb(context: Context) :
    SQLiteOpenHelper(context, "notifications.db", null, 2) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.enableWriteAheadLogging()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE notifications (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sbn_key TEXT NOT NULL,
                pkg TEXT NOT NULL,
                app_label TEXT NOT NULL,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                posted_at INTEGER NOT NULL,
                link TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_posted_at ON notifications(posted_at)")
        db.execSQL("CREATE INDEX idx_pkg_posted_at ON notifications(pkg, posted_at)")
        db.execSQL("CREATE INDEX idx_sbn_key ON notifications(sbn_key, id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migrate step by step. Never drop user data.
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE notifications ADD COLUMN link TEXT NOT NULL DEFAULT ''")
        }
    }

    fun insert(r: NotificationRecord): Long {
        val values = ContentValues().apply {
            if (r.id > 0) put("id", r.id)
            put("sbn_key", r.key)
            put("pkg", r.pkg)
            put("app_label", r.appLabel)
            put("title", r.title)
            put("body", r.body)
            put("posted_at", r.postedAt)
            put("link", r.link)
        }
        return writableDatabase.insertWithOnConflict(
            "notifications", null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun updateContent(id: Long, title: String, body: String, postedAt: Long, link: String) {
        val values = ContentValues().apply {
            put("link", link)
            put("title", title)
            put("body", body)
            put("posted_at", postedAt)
        }
        writableDatabase.update("notifications", values, "id = ?", arrayOf(id.toString()))
    }

    fun delete(id: Long) {
        writableDatabase.delete("notifications", "id = ?", arrayOf(id.toString()))
    }

    fun deleteAll() {
        writableDatabase.delete("notifications", null, null)
    }

    fun deleteOlderThan(timestamp: Long): Int =
        writableDatabase.delete("notifications", "posted_at < ?", arrayOf(timestamp.toString()))

    /** The most recent row saved for a status-bar key, used to detect re-posts. */
    fun lastForKey(key: String): NotificationRecord? =
        readableDatabase.rawQuery(
            "SELECT $COLUMNS FROM notifications WHERE sbn_key = ? ORDER BY id DESC LIMIT 1",
            arrayOf(key),
        ).use { c -> if (c.moveToFirst()) c.toRecord() else null }

    /** Rewrites the file so space freed by deletions is returned to the phone. */
    fun compact() {
        writableDatabase.execSQL("VACUUM")
        writableDatabase.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
    }

    /** Per-app counts and saved text size, largest first. */
    fun textBytesByApp(): List<Pair<AppCount, Long>> =
        readableDatabase.rawQuery(
            """
            SELECT pkg, MAX(app_label), COUNT(*), MAX(posted_at),
                   SUM(LENGTH(CAST(title AS BLOB)) + LENGTH(CAST(body AS BLOB)) +
                       LENGTH(CAST(link AS BLOB)) + LENGTH(sbn_key) + LENGTH(pkg) + 48) AS bytes
            FROM notifications GROUP BY pkg ORDER BY bytes DESC
            """.trimIndent(),
            null,
        ).use { c ->
            val out = ArrayList<Pair<AppCount, Long>>(c.count)
            while (c.moveToNext()) {
                out += AppCount(c.getString(0), c.getString(1), c.getInt(2), c.getLong(3)) to c.getLong(4)
            }
            out
        }

    fun count(): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM notifications", null)
            .use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    fun timestampsSince(since: Long): LongArray =
        readableDatabase.rawQuery(
            "SELECT posted_at FROM notifications WHERE posted_at >= ?",
            arrayOf(since.toString()),
        ).use { c ->
            val out = LongArray(c.count)
            var i = 0
            while (c.moveToNext()) out[i++] = c.getLong(0)
            out
        }

    /** Per-app totals inside [range], busiest first. */
    fun appCounts(range: DateRange): List<AppCount> {
        val where = ArrayList<String>()
        val args = ArrayList<String>()
        addRange(range, where, args)
        val sql = buildString {
            append("SELECT pkg, MAX(app_label), COUNT(*) AS n, MAX(posted_at) AS last_at FROM notifications")
            if (where.isNotEmpty()) append(" WHERE ").append(where.joinToString(" AND "))
            append(" GROUP BY pkg ORDER BY n DESC, last_at DESC")
        }
        return readableDatabase.rawQuery(sql, args.toTypedArray()).use { c ->
            val out = ArrayList<AppCount>(c.count)
            while (c.moveToNext()) {
                out += AppCount(c.getString(0), c.getString(1), c.getInt(2), c.getLong(3))
            }
            out
        }
    }

    private fun addRange(range: DateRange, where: MutableList<String>, args: MutableList<String>) {
        range.from?.let { where += "posted_at >= ?"; args += it.toString() }
        range.until?.let { where += "posted_at < ?"; args += it.toString() }
    }

    /** Newest first. [query] matches title, text or app name; [pkg] limits to one app. */
    fun list(query: String, pkg: String?, range: DateRange, limit: Int): List<NotificationRecord> {
        val where = ArrayList<String>()
        val args = ArrayList<String>()
        addRange(range, where, args)
        if (pkg != null) {
            where += "pkg = ?"
            args += pkg
        }
        val q = query.trim()
        if (q.isNotEmpty()) {
            where += "(title LIKE ? ESCAPE '\\' OR body LIKE ? ESCAPE '\\' OR app_label LIKE ? ESCAPE '\\')"
            val pattern = "%" + q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
            repeat(3) { args += pattern }
        }
        val sql = buildString {
            append("SELECT $COLUMNS FROM notifications")
            if (where.isNotEmpty()) append(" WHERE ").append(where.joinToString(" AND "))
            append(" ORDER BY posted_at DESC, id DESC LIMIT ").append(limit)
        }
        return readableDatabase.rawQuery(sql, args.toTypedArray()).use { c ->
            val out = ArrayList<NotificationRecord>(c.count)
            while (c.moveToNext()) out += c.toRecord()
            out
        }
    }

    private fun Cursor.toRecord() = NotificationRecord(
        id = getLong(0),
        key = getString(1),
        pkg = getString(2),
        appLabel = getString(3),
        title = getString(4),
        body = getString(5),
        postedAt = getLong(6),
        link = getString(7),
    )

    private companion object {
        const val COLUMNS = "id, sbn_key, pkg, app_label, title, body, posted_at, link"
    }
}
