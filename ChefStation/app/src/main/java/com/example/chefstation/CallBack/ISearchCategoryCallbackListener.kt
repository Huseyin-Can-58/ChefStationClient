package com.example.chefstation.CallBack

import com.example.chefstation.Database.CartItem
import com.example.chefstation.Model.CategoryModel

interface ISearchCategoryCallbackListener {

    fun onSearchFound(category:CategoryModel,cartItem:CartItem)
    fun onSearchNotFound(message:String)
}