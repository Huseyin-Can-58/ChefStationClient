package com.example.chefstation.CallBack

import com.example.chefstation.Model.OrderModel

interface ILoadTimeFromFirebaseCallback {

    fun onLoadTimeSuccess(order: OrderModel, estimatedTimeMs:Long)
    fun onLoadTimeFailed(message:String)
}