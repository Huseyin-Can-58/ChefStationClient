package com.example.chefstation.ui.foodlist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.chefstation.Common.Common
import com.example.chefstation.Model.FoodModel

class FoodListViewModel :ViewModel(){

    private var mutableFoodModelListData : MutableLiveData<List<FoodModel>>?=null

            fun getMutableFoodModelListData():MutableLiveData<List<FoodModel>>{

                if(mutableFoodModelListData == null)
                    mutableFoodModelListData = MutableLiveData()
                mutableFoodModelListData!!.value = Common.categorySelected!!.foods
                return mutableFoodModelListData!!
            }
}