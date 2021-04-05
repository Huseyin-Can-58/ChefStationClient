package com.example.chefstation.ui.comment

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.chefstation.Model.CommentModel

class CommentViewModel : ViewModel() {

    val mutableLiveDataCommentList:MutableLiveData<List<CommentModel>>

    init{
        mutableLiveDataCommentList = MutableLiveData()
    }

    fun setCommentList(commentList:List<CommentModel>){

        mutableLiveDataCommentList.value = commentList
    }
}