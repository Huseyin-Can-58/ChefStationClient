package com.example.chefstation.CallBack

import com.example.chefstation.Model.CommentModel

interface ICommentCallBack {

    fun onCommentLoadSuccess(commentList:List<CommentModel>)
    fun onCommentLoadFailed(message:String)
}