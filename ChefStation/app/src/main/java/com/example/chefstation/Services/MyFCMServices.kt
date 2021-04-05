package com.example.chefstation.Services

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.chefstation.Common.Common
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.*

class MyFCMServices : FirebaseMessagingService() {

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        Common.updateToken(this,p0)
    }

    override fun onMessageReceived(remotemessage: RemoteMessage) {
        super.onMessageReceived(remotemessage)
        val dataRecv = remotemessage.data
        if(dataRecv != null){

            if(dataRecv[Common.IS_SEND_IMAGE] != null && dataRecv[Common.IS_SEND_IMAGE].equals("true")){

                Glide.with(this)
                    .asBitmap()
                    .load(dataRecv[Common.IMAGE_URL])
                    .into(object:CustomTarget<Bitmap>(){
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            Common.showNotification(this@MyFCMServices, Random().nextInt(),
                                dataRecv[Common.NOTI_TITLE],
                                dataRecv[Common.NOTI_CONTENT],
                                resource,
                                null)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {

                        }


                    })
            }
            else{

                Common.showNotification(this, Random().nextInt(),
                    dataRecv[Common.NOTI_TITLE],
                    dataRecv[Common.NOTI_CONTENT],
                    null)
            }
        }
    }
}