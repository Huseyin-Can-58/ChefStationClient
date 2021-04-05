package com.example.chefstation.CallBack

import com.example.chefstation.Model.CategoryModel
import com.example.chefstation.Model.PopularCategoryModel

interface ICategoryCallBackListener {

    fun onCategoryLoadSuccess(categoriesList:List<CategoryModel>)

    fun onCategoryLoadFailed(message:String)
}