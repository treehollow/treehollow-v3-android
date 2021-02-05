package com.github.treehollow.repository

import android.util.Log
import com.github.treehollow.base.Result
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.database.PushMessageDao
import com.github.treehollow.network.ApiResponse

class PushMessageRepository(
    private val token: String
) :
    Fetcher() {

    val dao: PushMessageDao = TreeHollowApplication.dbInstance.pushMessagesDao()

    suspend fun getMaxId(): Int? {
        return dao.getMaxPushId()
    }

    suspend fun getMessages(
        page: Int,
        pushOnly: Int,
        since: Int
    ): Result<List<ApiResponse.PushMessage>> {
        val result: List<ApiResponse.PushMessage>
        try {

            val response = service.myMessages(token, page, pushOnly, since)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    if (body.code < 0)
                        throw Exception("Error: " + body.msg)
                    else
                        result = body.data!!.toList()
                } else {
                    throw Exception("Network response body is null")
                }
            } else {
                throw Exception("Network error ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("PushMessagesRepository", "cannot retrieve missing messages $e")
            return Result.Error(e)
        }
        //        Collections.reverse(result);

        val maxId = getMaxId()
        if (maxId != null)
            dao.savePushMessage(result.filter { it.id > maxId })
        else
            dao.savePushMessage(result)

        return Result.Success(result)
    }
}