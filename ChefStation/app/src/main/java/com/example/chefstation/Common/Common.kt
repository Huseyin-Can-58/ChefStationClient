package com.example.chefstation.Common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.chefstation.Model.*
import com.example.chefstation.R
import com.example.chefstation.Services.MyFCMServices
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.collection.LLRBNode
import java.lang.StringBuilder
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*

object Common {
    fun formatPrice(price: Double): String {

        if(price != 0.toDouble()){

            val df = DecimalFormat("#,##0.00")
            df.roundingMode = RoundingMode.HALF_UP
            val finalPrice = StringBuilder(df.format(price)).toString()
            return finalPrice.replace(".",",")
        }
        else
            return "0,00"
    }

    fun calculateExtraPrice(
        userSelectedSize: SizeModel?,
        userSelectedAddon: MutableList<AddonModel>?
    ): Double {
        var result:Double=0.0
        if(userSelectedSize == null && userSelectedAddon == null)
            return 0.0
        else if(userSelectedSize == null){

            for(addonModel in userSelectedAddon!!)
                result += addonModel.price!!.toDouble()
            return result
        }
        else if(userSelectedAddon == null){

            result = userSelectedSize!!.price.toDouble()
            return result
        }
        else{
            result = userSelectedSize!!.price.toDouble()
            for(addonModel in userSelectedAddon!!)
                result += addonModel.price!!.toDouble()
            return result
        }
    }

    fun setSpanString(welcome: String, name: String?, txtUser: TextView?) {
        val builder = SpannableStringBuilder()
        builder.append(welcome)
        val txtSpannable = SpannableString(name)
        val boldSpan = StyleSpan(Typeface.BOLD)
        txtSpannable.setSpan(boldSpan,0,name!!.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.append(txtSpannable)
        txtUser!!.setText(builder,TextView.BufferType.SPANNABLE)
    }

    fun createOrderNumber(): String {
        return StringBuilder()
            .append(System.currentTimeMillis())
            .append(Math.abs(Random().nextInt()))
            .toString()
    }

    fun getDateOfWeek(i: Int): String {

        when(i){
            1 -> return "Pazartesi"
            2 -> return "Salı"
            3 -> return "Çarşamba"
            4 -> return "Perşembe"
            5 -> return "Cuma"
            6 -> return "Cumartesi"
            7 -> return "Pazar"
            else -> return "Bilinmeyen"
        }

    }

    fun convertStatusToText(orderStatus: Int): String {

        when(orderStatus){

            0 -> return "Onaylandı"
            1 -> return "Hazırlanıyor"
            2 -> return "Teslimatta"
            -1 -> return "İptal Edildi"
            else -> return "Bilinmeyen"
        }

    }

    fun updateToken(context: Context, token: String) {
        FirebaseDatabase.getInstance()
            .getReference(Common.TOKEN_REF)
            .child(Common.currentUser!!.uid!!)
            .setValue(TokenModel(Common.currentUser!!.phone!!,token))
            .addOnFailureListener { e -> Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show() }
    }

    fun showNotification(context: Context, id: Int, title: String?, content: String?,intent: Intent?) {
        var pendingIntent: PendingIntent?=null
        if(intent != null)
            pendingIntent = PendingIntent.getActivity(context,id,intent,PendingIntent.FLAG_UPDATE_CURRENT)
        val NOTIFICATION_CHANNEL_ID = "com.example.chefstation"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID,
            "ChefStation",NotificationManager.IMPORTANCE_DEFAULT)

            notificationChannel.description = "ChefStation"
            notificationChannel.enableLights(true)
            notificationChannel.enableVibration(true)
            notificationChannel.lightColor = (Color.RED)
            notificationChannel.vibrationPattern = longArrayOf(0,1000,500,1000)
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val builder = NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID)

        builder.setContentTitle(title!!).setContentText(content!!).setAutoCancel(true)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources,R.drawable.ic_baseline_restaurant_menu_24))
                .setStyle(NotificationCompat.BigTextStyle())
        if(pendingIntent != null)
            builder.setContentIntent(pendingIntent)

        val notification = builder.build()

        notificationManager.notify(id,notification)

    }

    fun showNotification(context: Context, id: Int, title: String?, content: String?, bitmap: Bitmap, intent: Intent?) {
        var pendingIntent: PendingIntent?=null
        if(intent != null)
            pendingIntent = PendingIntent.getActivity(context,id,intent,PendingIntent.FLAG_UPDATE_CURRENT)
        val NOTIFICATION_CHANNEL_ID = "com.example.chefstation"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID,
                "ChefStation",NotificationManager.IMPORTANCE_DEFAULT)

            notificationChannel.description = "ChefStation"
            notificationChannel.enableLights(true)
            notificationChannel.enableVibration(true)
            notificationChannel.lightColor = (Color.RED)
            notificationChannel.vibrationPattern = longArrayOf(0,1000,500,1000)
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val builder = NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID)

        builder.setContentTitle(title!!).setContentText(content!!).setAutoCancel(true)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setLargeIcon(bitmap)
            .setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
        if(pendingIntent != null)
            builder.setContentIntent(pendingIntent)

        val notification = builder.build()

        notificationManager.notify(id,notification)

    }

    fun getNewOrderTopic(): String {
        return StringBuilder("/topics/new_order").toString()
    }

    fun findFoodInListById(category: CategoryModel, foodId: String): FoodModel? {
        return if(category!!.foods != null && category.foods!!.size > 0){

            for(foodModel in category!!.foods!!) if(foodModel.id.equals(foodId))
                return foodModel
            null
        } else null
    }

    fun getListAddon(addonModels: List<AddonModel>): String {

        val result = StringBuilder()
        for(addonModel in addonModels)
            result.append(addonModel.name).append(",")
        if(!result.isEmpty())
            return result.substring(0,result.length-1) // Son virgülü silme
        else
            return "Varsayılan"
    }

    var discountApply: DiscountModel?=null
    val DISCOUNT: String = "Discount"
    val QR_CODE_TAG: String? = "QRCODE"
    val IMAGE_URL: String="IMAGE_URL"
    val IS_SEND_IMAGE: String="IS_SEND_IMAGE"
    val NEWS_TOPIC: String="news"
    val IS_SUBSCRIBE_NEWS: String = "IS_SUBSCRIBE_NEWS"
    const val NOTI_TITLE = "title"
    const val NOTI_CONTENT = "content"
    const val COMMENT_REF: String = "Comments"
    const val ORDER_REF: String = "Order"
    var foodSelected: FoodModel?=null
    var categorySelected: CategoryModel?=null
    const val CATEGORY_REF: String = "Category"
    val FULL_WIDTH_COLUMN: Int = 1
    val DEFAULT_COLUMN_COUNT: Int=0
    const val BEST_DEALS_REF: String = "BestDeals"
    const val POPULAR_REF: String="MostPopular"
    const val USER_REFERENCE="Users"
    var currentUser: UserModel?=null

    const val TOKEN_REF = "Tokens"

}