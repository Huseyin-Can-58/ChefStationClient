package com.example.chefstation.ui.view_orders

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.chefstation.Model.OrderModel

class ViewOrderModel : ViewModel() {
    val mutableLiveDataOrderList:MutableLiveData<List<OrderModel>>
    init{
        mutableLiveDataOrderList = MutableLiveData()
    }
    fun setMutableLiveDataOrderList(orderList:List<OrderModel>){

        mutableLiveDataOrderList.value = orderList
    }
}