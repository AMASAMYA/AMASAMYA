package com.example.amasamya.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class AuditSession(
    val id: Long = 0,
    val name: String,
    val date: Long,
    val packageName: String,
    val deviceInfo: String,
    val wcagLevel: String
)

data class ElementIssue(
    val id: Long = 0,
    val sessionId: Long,
    val screenName: String,
    val className: String,
    val bounds: String,
    val text: String,
    val contentDescription: String,
    val issueType: String, // e.g. "Target Size", "Missing Label", "Redundant Label", "Focus Noise"
    val severity: String,  // e.g. "Critical", "Warning", "Info"
    val description: String,
    val wcagSc: String      // WCAG Success Criterion, e.g. "2.5.5", "1.1.1"
)

data class FocusPathNode(
    val id: Long = 0,
    val sessionId: Long,
    val screenName: String,
    val className: String,
    val bounds: String,
    val text: String,
    val contentDescription: String,
    val focusOrder: Int
)

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "amasamya_audit.db"
        private const val DATABASE_VERSION = 2

        // Sessions Table
        private const val TABLE_SESSIONS = "sessions"
        private const val KEY_SESSION_ID = "id"
        private const val KEY_SESSION_NAME = "name"
        private const val KEY_SESSION_DATE = "date"
        private const val KEY_SESSION_PACKAGE = "package_name"
        private const val KEY_SESSION_DEVICE = "device_info"
        private const val KEY_SESSION_WCAG = "wcag_level"

        // Issues Table
        private const val TABLE_ISSUES = "issues"
        private const val KEY_ISSUE_ID = "id"
        private const val KEY_ISSUE_SESSION_ID = "session_id"
        private const val KEY_ISSUE_SCREEN = "screen_name"
        private const val KEY_ISSUE_CLASS = "class_name"
        private const val KEY_ISSUE_BOUNDS = "bounds"
        private const val KEY_ISSUE_TEXT = "text"
        private const val KEY_ISSUE_DESC = "content_description"
        private const val KEY_ISSUE_TYPE = "issue_type"
        private const val KEY_ISSUE_SEVERITY = "severity"
        private const val KEY_ISSUE_MESSAGE = "description"
        private const val KEY_ISSUE_SC = "wcag_sc"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createSessionsTable = """
            CREATE TABLE $TABLE_SESSIONS (
                $KEY_SESSION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_SESSION_NAME TEXT NOT NULL,
                $KEY_SESSION_DATE INTEGER NOT NULL,
                $KEY_SESSION_PACKAGE TEXT NOT NULL,
                $KEY_SESSION_DEVICE TEXT NOT NULL,
                $KEY_SESSION_WCAG TEXT NOT NULL
            )
        """.trimIndent()

        val createIssuesTable = """
            CREATE TABLE $TABLE_ISSUES (
                $KEY_ISSUE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_ISSUE_SESSION_ID INTEGER NOT NULL,
                $KEY_ISSUE_SCREEN TEXT NOT NULL,
                $KEY_ISSUE_CLASS TEXT NOT NULL,
                $KEY_ISSUE_BOUNDS TEXT NOT NULL,
                $KEY_ISSUE_TEXT TEXT NOT NULL,
                $KEY_ISSUE_DESC TEXT NOT NULL,
                $KEY_ISSUE_TYPE TEXT NOT NULL,
                $KEY_ISSUE_SEVERITY TEXT NOT NULL,
                $KEY_ISSUE_MESSAGE TEXT NOT NULL,
                $KEY_ISSUE_SC TEXT NOT NULL,
                FOREIGN KEY($KEY_ISSUE_SESSION_ID) REFERENCES $TABLE_SESSIONS($KEY_SESSION_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        val createFocusPathTable = """
            CREATE TABLE IF NOT EXISTS focus_path_nodes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id INTEGER NOT NULL,
                screen_name TEXT NOT NULL,
                class_name TEXT NOT NULL,
                bounds TEXT NOT NULL,
                text TEXT NOT NULL,
                content_description TEXT NOT NULL,
                focus_order INTEGER NOT NULL,
                FOREIGN KEY(session_id) REFERENCES $TABLE_SESSIONS($KEY_SESSION_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        db.execSQL(createSessionsTable)
        db.execSQL(createIssuesTable)
        db.execSQL(createFocusPathTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            val createFocusPathTable = """
                CREATE TABLE IF NOT EXISTS focus_path_nodes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_id INTEGER NOT NULL,
                    screen_name TEXT NOT NULL,
                    class_name TEXT NOT NULL,
                    bounds TEXT NOT NULL,
                    text TEXT NOT NULL,
                    content_description TEXT NOT NULL,
                    focus_order INTEGER NOT NULL,
                    FOREIGN KEY(session_id) REFERENCES sessions(id) ON DELETE CASCADE
                )
            """.trimIndent()
            db.execSQL(createFocusPathTable)
        }
    }

    // CRUD - Session
    fun insertSession(session: AuditSession): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_SESSION_NAME, session.name)
            put(KEY_SESSION_DATE, session.date)
            put(KEY_SESSION_PACKAGE, session.packageName)
            put(KEY_SESSION_DEVICE, session.deviceInfo)
            put(KEY_SESSION_WCAG, session.wcagLevel)
        }
        val id = db.insert(TABLE_SESSIONS, null, values)
        return id
    }

    fun getAllSessions(): List<AuditSession> {
        val sessionList = mutableListOf<AuditSession>()
        val selectQuery = "SELECT * FROM $TABLE_SESSIONS ORDER BY $KEY_SESSION_DATE DESC"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val session = AuditSession(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_SESSION_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SESSION_NAME)),
                    date = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_SESSION_DATE)),
                    packageName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SESSION_PACKAGE)),
                    deviceInfo = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SESSION_DEVICE)),
                    wcagLevel = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SESSION_WCAG))
                )
                sessionList.add(session)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return sessionList
    }

    fun getSession(id: Long): AuditSession? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_SESSIONS, null, "$KEY_SESSION_ID=?", arrayOf(id.toString()),
            null, null, null
        )
        var session: AuditSession? = null
        if (cursor.moveToFirst()) {
            session = AuditSession(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_SESSION_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SESSION_NAME)),
                date = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_SESSION_DATE)),
                packageName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SESSION_PACKAGE)),
                deviceInfo = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SESSION_DEVICE)),
                wcagLevel = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SESSION_WCAG))
            )
        }
        cursor.close()
        return session
    }

    fun deleteSession(sessionId: Long) {
        val db = this.writableDatabase
        db.delete("focus_path_nodes", "session_id=?", arrayOf(sessionId.toString()))
        db.delete(TABLE_ISSUES, "$KEY_ISSUE_SESSION_ID=?", arrayOf(sessionId.toString()))
        db.delete(TABLE_SESSIONS, "$KEY_SESSION_ID=?", arrayOf(sessionId.toString()))
    }

    // CRUD - Issues
    fun insertIssue(issue: ElementIssue): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_ISSUE_SESSION_ID, issue.sessionId)
            put(KEY_ISSUE_SCREEN, issue.screenName)
            put(KEY_ISSUE_CLASS, issue.className)
            put(KEY_ISSUE_BOUNDS, issue.bounds)
            put(KEY_ISSUE_TEXT, issue.text)
            put(KEY_ISSUE_DESC, issue.contentDescription)
            put(KEY_ISSUE_TYPE, issue.issueType)
            put(KEY_ISSUE_SEVERITY, issue.severity)
            put(KEY_ISSUE_MESSAGE, issue.description)
            put(KEY_ISSUE_SC, issue.wcagSc)
        }
        val id = db.insert(TABLE_ISSUES, null, values)
        return id
    }

    fun getIssuesForSession(sessionId: Long): List<ElementIssue> {
        val issueList = mutableListOf<ElementIssue>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_ISSUES, null, "$KEY_ISSUE_SESSION_ID=?", arrayOf(sessionId.toString()),
            null, null, null
        )

        if (cursor.moveToFirst()) {
            do {
                val issue = ElementIssue(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ISSUE_ID)),
                    sessionId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ISSUE_SESSION_ID)),
                    screenName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ISSUE_SCREEN)),
                    className = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ISSUE_CLASS)),
                    bounds = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ISSUE_BOUNDS)),
                    text = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ISSUE_TEXT)),
                    contentDescription = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ISSUE_DESC)),
                    issueType = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ISSUE_TYPE)),
                    severity = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ISSUE_SEVERITY)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ISSUE_MESSAGE)),
                    wcagSc = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ISSUE_SC))
                )
                issueList.add(issue)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return issueList
    }

    // CRUD - Focus Path Nodes
    fun insertFocusNode(node: FocusPathNode): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("session_id", node.sessionId)
            put("screen_name", node.screenName)
            put("class_name", node.className)
            put("bounds", node.bounds)
            put("text", node.text)
            put("content_description", node.contentDescription)
            put("focus_order", node.focusOrder)
        }
        return db.insert("focus_path_nodes", null, values)
    }

    fun getFocusNodesForSession(sessionId: Long): List<FocusPathNode> {
        val nodeList = mutableListOf<FocusPathNode>()
        val db = this.readableDatabase
        val cursor = db.query(
            "focus_path_nodes", null, "session_id=?", arrayOf(sessionId.toString()),
            null, null, "focus_order ASC"
        )

        if (cursor.moveToFirst()) {
            do {
                val node = FocusPathNode(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    sessionId = cursor.getLong(cursor.getColumnIndexOrThrow("session_id")),
                    screenName = cursor.getString(cursor.getColumnIndexOrThrow("screen_name")),
                    className = cursor.getString(cursor.getColumnIndexOrThrow("class_name")),
                    bounds = cursor.getString(cursor.getColumnIndexOrThrow("bounds")),
                    text = cursor.getString(cursor.getColumnIndexOrThrow("text")),
                    contentDescription = cursor.getString(cursor.getColumnIndexOrThrow("content_description")),
                    focusOrder = cursor.getInt(cursor.getColumnIndexOrThrow("focus_order"))
                )
                nodeList.add(node)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return nodeList
    }
}
