package com.example.chefstation.ui.view_orders

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chefstation.Adapter.MyOrderAdapter
import com.example.chefstation.CallBack.ILoadOrderCallbackListener
import com.example.chefstation.CallBack.IMyButtonCallBack
import com.example.chefstation.Common.Common
import com.example.chefstation.Common.MySwipeHelper
import com.example.chefstation.Database.CartDataSource
import com.example.chefstation.Database.CartDatabase
import com.example.chefstation.Database.LocalCartDataSource
import com.example.chefstation.EventBus.CountCartEvent
import com.example.chefstation.EventBus.MenuItemBack
import com.example.chefstation.Model.OrderModel
import com.example.chefstation.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dmax.dialog.SpotsDialog
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ViewOrderFragment : Fragment(), ILoadOrderCallbackListener {

    private var viewOrderModel : ViewOrderModel?=null

    lateinit var cartDataSource: CartDataSource
    var compositeDisposable = CompositeDisposable()

    internal lateinit var dialog:AlertDialog
    internal lateinit var recycler_order:RecyclerView
    internal lateinit var listener:ILoadOrderCallbackListener

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewOrderModel = ViewModelProviders.of(this).get(ViewOrderModel::class.java!!)
        val root = inflater.inflate(R.layout.fragment_view_orders,container,false)
        initViews(root)
        loadOrderFromFirebase()

        viewOrderModel!!.mutableLiveDataOrderList.observe(this, Observer {
          Collections.reverse(it!!)
          val adapter = MyOrderAdapter(context!!,it!!.toMutableList())
          recycler_order!!.adapter = adapter
        })

        return root
    }

    private fun loadOrderFromFirebase() {
        dialog.show()
        val orderList = ArrayList<OrderModel>()

        FirebaseDatabase.getInstance().getReference(Common.ORDER_REF)
                .orderByChild("userId")
                .equalTo(Common.currentUser!!.uid!!)
                .limitToLast(100)
                .addListenerForSingleValueEvent(object:ValueEventListener{
                    override fun onDataChange(p0: DataSnapshot) {
                        for(orderSnapShot in p0.children){

                            val order = orderSnapShot.getValue(OrderModel::class.java)
                            order!!.orderNumber = orderSnapShot.key                    // Ürünün key değerini sipariş numarasına eklemek
                            orderList.add(order!!)
                        }
                        listener.onLoadOrderSuccess(orderList)
                    }

                    override fun onCancelled(p0: DatabaseError) {
                        listener.onLoadOrderFailed(p0.message!!)
                    }

                })
    }

    private fun initViews(root: View?) {

        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context!!).cartDAO())

        listener = this
        dialog = SpotsDialog.Builder().setContext(context!!).setCancelable(false).build()
        recycler_order = root!!.findViewById(R.id.recycler_order) as RecyclerView
        recycler_order.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context!!)
        recycler_order.layoutManager = layoutManager
        recycler_order.addItemDecoration(DividerItemDecoration(context!!,layoutManager.orientation))

        val swipe = object: MySwipeHelper(context!!, recycler_order!!, 250){
            override fun instantiateMyButton(
                    viewHolder: RecyclerView.ViewHolder,
                    buffer: MutableList<MyButton>
            ) {
                buffer.add(
                        MyButton(context!!,
                                "İptal Et",
                                30,
                                0,
                                Color.parseColor("#FF3C30"),
                                object : IMyButtonCallBack {
                                    override fun onClick(pos: Int) {

                                        val orderModel = (recycler_order.adapter as MyOrderAdapter).getItemAtPosition(pos)

                                        if(orderModel.orderStatus == 0){

                                            val builder = androidx.appcompat.app.AlertDialog.Builder(context!!)
                                            builder.setTitle("Sipariş İptali")
                                                    .setMessage("Bu siparişi gerçekten iptal etmek istiyor musunuz?")
                                                    .setNegativeButton("HAYIR"){ dialogInterface, i ->
                                                        dialogInterface.dismiss()
                                                    }
                                                    .setPositiveButton("EVET"){ dialogInterface, i ->

                                                        val update_data = HashMap<String,Any>()
                                                        update_data.put("orderStatus",-1) // Siparişi iptal durumuna güncelleme
                                                        FirebaseDatabase.getInstance()
                                                                .getReference(Common.ORDER_REF)
                                                                .child(orderModel.orderNumber!!)
                                                                .updateChildren(update_data)
                                                                .addOnFailureListener{ e ->
                                                                    Toast.makeText(context!!,""+e.message,Toast.LENGTH_SHORT).show()
                                                                }
                                                                .addOnSuccessListener {

                                                                    orderModel.orderStatus = -1 // Local Güncelleme
                                                                    (recycler_order.adapter as MyOrderAdapter).setItemAtPosition(pos,orderModel)
                                                                    (recycler_order.adapter as MyOrderAdapter).notifyItemChanged(pos) // Güncelleme
                                                                    Toast.makeText(context!!,"Siparişiniz başarıyla iptal edildi!",Toast.LENGTH_SHORT).show()
                                                                }
                                                    }

                                            val dialog = builder.create()
                                            dialog.show()

                                        }else{

                                            Toast.makeText(context!!,StringBuilder("Sipariş durumunuz ").append(Common.convertStatusToText(orderModel.orderStatus))
                                                    .append(" olarak güncellendiği için siparişinizi iptal edemezsiniz!"),Toast.LENGTH_SHORT).show()
                                        }

                                    }

                                })


                )

                buffer.add(
                        MyButton(context!!,
                                "Tekrarla",
                                30,
                                0,
                                Color.parseColor("#5d4037"),
                                object : IMyButtonCallBack {
                                    override fun onClick(pos: Int) {

                                        val orderModel = (recycler_order.adapter as MyOrderAdapter).getItemAtPosition(pos)

                                        dialog.show()

                                        // Sepeti Temizleme

                                        cartDataSource.cleanCart(Common.currentUser!!.uid!!)
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe(object:SingleObserver<Int>{
                                                    override fun onSubscribe(d: Disposable) {

                                                    }

                                                    override fun onSuccess(t: Int) {

                                                        val cartItems = orderModel.cartItemList!!.toTypedArray()

                                                        compositeDisposable.add(
                                                                cartDataSource.insertOrReplaceAll(*cartItems)
                                                                        .subscribeOn(Schedulers.io())
                                                                        .observeOn(AndroidSchedulers.mainThread())
                                                                        .subscribe({
                                                                            dialog.dismiss()
                                                                            EventBus.getDefault().postSticky(CountCartEvent(true))
                                                                            Toast.makeText(context!!,"Bütün yemekler sepete başarıyla eklendi!",Toast.LENGTH_SHORT).show()
                                                                        },{
                                                                            t:Throwable? ->
                                                                                dialog.dismiss()
                                                                            Toast.makeText(context!!,t!!.message!!,Toast.LENGTH_SHORT).show()

                                                                        })
                                                        )
                                                    }

                                                    override fun onError(e: Throwable) {
                                                        dialog.dismiss()
                                                        Toast.makeText(context!!,e.message!!,Toast.LENGTH_SHORT).show()
                                                    }


                                                })
                                    }

                                })


                )
            }
        }
    }

    override fun onLoadOrderSuccess(orderList: List<OrderModel>) {
        dialog.dismiss()
        viewOrderModel!!.setMutableLiveDataOrderList(orderList)
    }

    override fun onLoadOrderFailed(message: String) {
        dialog.dismiss()
        Toast.makeText(context!!,message,Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        compositeDisposable.clear()
        super.onDestroy()
    }
}