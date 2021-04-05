package com.example.chefstation.CallBack

import com.example.chefstation.Model.OrderModel

interface ILoadOrderCallbackListener {

    fun onLoadOrderSuccess(orderList:List<OrderModel>)
    fun onLoadOrderFailed(message:String)
}