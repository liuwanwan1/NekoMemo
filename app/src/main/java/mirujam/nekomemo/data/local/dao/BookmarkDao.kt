package mirujam.nekomemo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mirujam.nekomemo.data.local.entity.BookmarkEntity

@Dao
interface BookmarkDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE questionId = :questionId")
    suspend fun deleteByQuestionId(questionId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE questionId = :questionId)")
    fun isBookmarked(questionId: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE questionId = :questionId)")
    suspend fun isBookmarkedSync(questionId: Long): Boolean

    @Query("SELECT questionId FROM bookmarks")
    suspend fun getAllBookmarkedQuestionIds(): List<Long>

    @Query("SELECT questionId FROM bookmarks")
    fun getAllBookmarkedQuestionIdsFlow(): Flow<List<Long>>

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAll()
}
