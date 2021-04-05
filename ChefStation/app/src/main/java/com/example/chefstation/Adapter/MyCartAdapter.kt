package com.example.chefstation.Adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton
import com.example.chefstation.Common.Common
import com.example.chefstation.Database.CartDataSource
import com.example.chefstation.Database.CartDatabase
import com.example.chefstation.Database.CartItem
import com.example.chefstation.Database.LocalCartDataSource
import com.example.chefstation.EventBus.UpdateItemInCart
import com.example.chefstation.Model.AddonModel
import com.example.chefstation.Model.SizeModel
import com.example.chefstation.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.disposables.CompositeDisposable
import org.greenrobot.eventbus.EventBus
import java.lang.StringBuilder

class MyCartAdapter (internal var context: Context,
                     internal var cartItems:List<CartItem>) :
    RecyclerView.Adapter<MyCartAdapter.MyViewHolder>(){

    internal var compositeDisposable:CompositeDisposable
    internal var cartDataSource:CartDataSource
    val gson:Gson

    init {
        compositeDisposable = CompositeDisposable()
        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context).cartDAO())
        gson=Gson()
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

        lateinit var img_cart:ImageView
        lateinit var txt_food_name:TextView
        lateinit var txt_food_price:TextView
        lateinit var txt_food_size:TextView
        lateinit var txt_food_addon:TextView
        lateinit var number_button:ElegantNumberButton

        init {
            img_cart = itemView.findViewById(R.id.img_cart) as ImageView
            txt_food_name = itemView.findViewById(R.id.txt_food_name) as TextView
            txt_food_price = itemView.findViewById(R.id.txt_food_price) as TextView
            txt_food_size = itemView.findViewById(R.id.txt_food_size) as TextView
            txt_food_addon = itemView.findViewById(R.id.txt_food_addon) as TextView
            number_button = itemView.findViewById(R.id.number_button) as ElegantNumberButton
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_cart_item,parent,false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(cartItems[position].foodImage)
            .into(holder.img_cart)
        holder.txt_food_name.text=StringBuilder(cartItems[position].foodName!!)
        holder.txt_food_price.text=StringBuilder("").append(cartItems[position].foodPrice + cartItems[position].foodExtraPrice)

        if(cartItems[position].foodSize != null){

            if(cartItems[position].foodSize.equals("Default")) holder.txt_food_price.text = StringBuilder("Porsiyon: Varsayılan")
            else{
                val sizeModel = gson.fromJson<SizeModel>(cartItems[position].foodSize,object:TypeToken<SizeModel>(){}.type)
                holder.txt_food_size.text = StringBuilder("Porsiyon: ").append(sizeModel.name)
            }
        }

        if(cartItems[position].foodAddon != null){

            if(cartItems[position].foodAddon.equals("Default")) holder.txt_food_addon.text = StringBuilder("Eklenti: Varsayılan")
            else{
                val addonModels = gson.fromJson<List<AddonModel>>(cartItems[position].foodAddon,
                    object:TypeToken<List<AddonModel>>(){}.type)
                holder.txt_food_addon.text = StringBuilder("Eklenti: ").append(Common.getListAddon(addonModels))
            }
        }

        holder.number_button.number=cartItems[position].foodQuantity.toString()
        //Event
        holder.number_button.setOnValueChangeListener{ view, oldValue, newValue ->
            cartItems[position].foodQuantity = newValue
            EventBus.getDefault().postSticky(UpdateItemInCart(cartItems[position]))
        }
    }

    override fun getItemCount(): Int {
        return cartItems.size
    }

    fun getItemAtPosition(pos: Int): CartItem {
        return cartItems[pos]
    }

}