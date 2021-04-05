package com.example.chefstation.Adapter

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.FtsOptions
import com.bumptech.glide.Glide
import com.example.chefstation.CallBack.IRecyclerItemClickListener
import com.example.chefstation.Common.Common
import com.example.chefstation.Database.CartItem
import com.example.chefstation.Model.OrderModel
import com.example.chefstation.R
import java.text.SimpleDateFormat
import java.util.*

class MyOrderAdapter (private val context:Context,
                      private val orderList:MutableList<OrderModel>):
RecyclerView.Adapter<MyOrderAdapter.MyViewHolder>(){

    internal var calendar: Calendar
    internal var simpleDateFormat: SimpleDateFormat

    init {
        calendar= Calendar.getInstance()
        simpleDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
    }

    inner class MyViewHolder(itemView:View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        internal var img_order:ImageView?=null
        internal var txt_order_date:TextView?=null
        internal var txt_order_status:TextView?=null
        internal var txt_order_number:TextView?=null
        internal var txt_order_comment:TextView?=null

        internal var iRecyclerItemClickListener:IRecyclerItemClickListener?=null

        fun setListener(iRecyclerItemClickListener: IRecyclerItemClickListener){

            this.iRecyclerItemClickListener = iRecyclerItemClickListener
        }

        init {
            img_order = itemView.findViewById(R.id.img_order) as ImageView
            txt_order_date = itemView.findViewById(R.id.txt_order_date) as TextView
            txt_order_status = itemView.findViewById(R.id.txt_order_status) as TextView
            txt_order_number = itemView.findViewById(R.id.txt_order_number) as TextView
            txt_order_comment = itemView.findViewById(R.id.txt_order_comment) as TextView

            itemView.setOnClickListener(this)

        }

        override fun onClick(p0: View?) {
            iRecyclerItemClickListener!!.onItemClick(p0!!,adapterPosition)
        }

    }

    override fun getItemCount(): Int {
        return orderList.size
    }

    fun getItemAtPosition(position:Int):OrderModel{
        return orderList[position]
    }

    fun setItemAtPosition(position: Int,orderModel:OrderModel){

        orderList[position] = orderModel
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context!!)
                .inflate(R.layout.layout_order_item,parent,false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context!!)
                .load(orderList[position].cartItemList!![0].foodImage)
                .into(holder.img_order!!)

        calendar.timeInMillis = orderList[position].createDate
        val date = Date(orderList[position].createDate)
        holder.txt_order_date!!.text = StringBuilder(Common.getDateOfWeek(calendar.get(Calendar.DAY_OF_WEEK)))
                .append(" ")
                .append(simpleDateFormat.format(date))
        holder.txt_order_number!!.text = StringBuilder("Sipariş Numarası: ").append(orderList[position].orderNumber)
        holder.txt_order_comment!!.text = StringBuilder("Sipariş Notu: ").append(orderList[position].comment)
        holder.txt_order_status!!.text = StringBuilder("Sipariş Durumu: ").append(Common.convertStatusToText(orderList[position].orderStatus))

        holder.setListener(object:IRecyclerItemClickListener{
            override fun onItemClick(view: View, pos: Int) {

                showDialog(orderList[pos].cartItemList)
            }

        })

    }

    private fun showDialog(cartItemList: List<CartItem>?) {

        val layout_dialog = LayoutInflater.from(context).inflate(R.layout.layout_dialog_order_detail,null)
        val builder = AlertDialog.Builder(context)
        builder.setView(layout_dialog)

        val btn_ok = layout_dialog.findViewById<View>(R.id.btn_ok) as Button
        val recycler_order_detail = layout_dialog.findViewById<View>(R.id.recycler_order_detail) as RecyclerView
        recycler_order_detail.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)
        recycler_order_detail.layoutManager = layoutManager
        recycler_order_detail.addItemDecoration(DividerItemDecoration(context,layoutManager.orientation))
        val adapter = MyOrderDetailAdapter(context,cartItemList!!.toMutableList())
        recycler_order_detail.adapter = adapter

        val dialog = builder.create()
        dialog.show()

        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.setGravity(Gravity.CENTER)

        btn_ok.setOnClickListener{ dialog.dismiss() }
    }
}