package com.example.chefstation.Remote

import com.example.chefstation.Model.FCMResponse
import com.example.chefstation.Model.FCMSendData
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface IFCMService {

    @Headers(
        "Content-Type:application/json",
        "Authorization:key=AAAAvNjsLkE:APA91bGJ62aw2yKf3zghg7QnrXG22xAPkTq9fOAwGXLQgM-RpObBvEYumxTPSsJvNOsd38mwcaKT7kTsvh8KTLeIFS_v4OgO39JCbq-QXy4SfxEAfciJJcliw1LVkaaKfgjuB9VmB3qL"
    )
    @POST("fcm/send")
    fun sendNotification(@Body body: FCMSendData):Observable<FCMResponse>

}