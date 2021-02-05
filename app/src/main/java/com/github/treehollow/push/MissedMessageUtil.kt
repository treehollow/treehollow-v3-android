package com.github.treehollow.push

import android.util.Log
import com.github.treehollow.base.Result
import com.github.treehollow.network.ApiResponse.PushMessage
import com.github.treehollow.repository.PushMessageRepository

class MissedMessageUtil(
    token: String
) {
    private val repo: PushMessageRepository = PushMessageRepository(token)

    suspend fun lastReceivedMessage(): Int {
        //        Collections.reverse(result);
        return repo.getMaxId() ?: return WebSocketService.NOT_LOADED
    }

    suspend fun missingMessages(since: Int): List<PushMessage> {
        return when (val result = repo.getMessages(1, 1, since)) {
            is Result.Success -> {
                repo.dao.savePushMessage(result.data)
                result.data
            }
            is Result.Error -> {
                Log.i("MissedMessageUtil", "Error: " + result.exception.toString())
                ArrayList()
            }
        }
    }

    suspend fun saveOneMessage(msg: PushMessage) {
        repo.dao.savePushMessage(listOf(msg))
    }

//    private fun filter(messages: List<PushMessage>, till: Int): List<PushMessage?> {
//        val result: MutableList<PushMessage?> = ArrayList()
//        for (message in messages) {
//            if (message.id > till) {
//                result.add(message)
//            } else {
//                break
//            }
//        }
//        return result
//    }

//    companion object {
//        const val NO_MESSAGES: Long = 0
//    }
}