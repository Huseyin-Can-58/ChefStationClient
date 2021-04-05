package com.example.chefstation.CallBack

import com.example.chefstation.Model.PopularCategoryModel

interface IPopularLoadCallback {

    fun onPopularLoadSuccess(popularModelList:List<PopularCategoryModel>)

    fun onPopularLoadFailed(message:String)
}