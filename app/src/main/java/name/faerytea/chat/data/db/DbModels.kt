package name.faerytea.chat.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "messages")
data class CachedMessage(
    @PrimaryKey
    val id: String,
    val channelName: String,
    val from: String,
    val to: String,
    val text: String = "",
    val imageLink: String = "",
    val isImage: Boolean,
    val timestamp: Long,
    val isFromNetwork: Boolean,
    val isPending: Boolean = false,
)


@Entity(tableName = "drafts")
data class DraftMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val channelName: String,
    val from: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAttemptAt: Long = 0,
    val attemptCount: Int = 0,
)


@Entity(tableName = "channels")
data class CachedChannel(
    @PrimaryKey
    val name: String,
    val lastFetchedAt: Long = 0,
)

@Entity(tableName = "sync_metadata")
data class SyncMetadata(
    @PrimaryKey
    val channelName: String,
    val lastMessageId: String = "0",
    val lastSyncTime: Long = 0,
)
