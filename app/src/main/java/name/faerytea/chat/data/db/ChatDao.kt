package name.faerytea.chat.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {


    @Insert(onConflict = OnConflictStrategy.IGNORE)  // ignore если уже есть с таким id
    suspend fun insertMessage(message: CachedMessage)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessages(messages: List<CachedMessage>)

    @Query("SELECT * FROM messages WHERE channelName = :channelName ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getMessagesForChannel(channelName: String, limit: Int = 20): List<CachedMessage>


    @Query("SELECT * FROM messages WHERE channelName = :channelName ORDER BY timestamp ASC")
    fun getMessagesForChannel(channelName: String): Flow<List<CachedMessage>>

    @Query("SELECT * FROM messages WHERE channelName = :channelName ORDER BY timestamp DESC")
    fun getMessagesForChannelFlow(channelName: String): Flow<List<CachedMessage>>

    @Query("DELETE FROM messages WHERE channelName = :channelName")
    suspend fun deleteMessagesForChannel(channelName: String)

    @Query("SELECT COUNT(*) FROM messages WHERE channelName = :channelName")
    suspend fun getMessageCountForChannel(channelName: String): Int




    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: DraftMessage): Long

    @Query("SELECT * FROM drafts WHERE channelName = :channelName ORDER BY createdAt ASC")
    suspend fun getDraftsForChannel(channelName: String): List<DraftMessage>

    @Delete
    suspend fun deleteDraft(draft: DraftMessage)

    // TODO handle too many drafts
    @Query("SELECT * FROM drafts ORDER BY lastAttemptAt ASC")
    suspend fun getUnsentDrafts(): List<DraftMessage>

    @Query("UPDATE drafts SET lastAttemptAt = :now, attemptCount = attemptCount + 1 WHERE id = :id")
    suspend fun updateDraftAttempt(id: Long, now: Long)




    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<CachedChannel>)

    @Query("SELECT name FROM channels ORDER BY name ASC")
    suspend fun getCachedChannels(): List<String>

    @Query("DELETE FROM channels")
    suspend fun deleteAllChannels()



    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSyncMetadata(metadata: SyncMetadata)

    @Query("SELECT * FROM sync_metadata WHERE channelName = :channelName")
    suspend fun getSyncMetadata(channelName: String): SyncMetadata?
}
