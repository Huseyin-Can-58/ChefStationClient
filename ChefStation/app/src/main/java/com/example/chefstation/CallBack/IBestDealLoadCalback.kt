package com.example.chefstation.CallBack

import com.example.chefstation.Model.BestDealModel
import com.example.chefstation.Model.PopularCategoryModel

interface IBestDealLoadCalback {

    fun onBestDealLoadSuccess(bestDealList:List<BestDealModel>)

    fun onBestDealLoadFailed(message:String)
}