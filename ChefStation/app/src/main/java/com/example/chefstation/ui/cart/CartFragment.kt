package com.example.chefstation.ui.cart

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chefstation.Adapter.MyCartAdapter
import com.example.chefstation.CallBack.ILoadTimeFromFirebaseCallback
import com.example.chefstation.CallBack.IMyButtonCallBack
import com.example.chefstation.CallBack.ISearchCategoryCallbackListener
import com.example.chefstation.Common.Common
import com.example.chefstation.Common.MySwipeHelper
import com.example.chefstation.Database.CartDataSource
import com.example.chefstation.Database.CartDatabase
import com.example.chefstation.Database.CartItem
import com.example.chefstation.Database.LocalCartDataSource
import com.example.chefstation.EventBus.CountCartEvent
import com.example.chefstation.EventBus.HideFABCart
import com.example.chefstation.EventBus.MenuItemBack
import com.example.chefstation.EventBus.UpdateItemInCart
import com.example.chefstation.Model.*
import com.example.chefstation.R
import com.example.chefstation.Remote.IFCMService
import com.example.chefstation.Remote.RetrofitFCMClient
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class CartFragment : Fragment(), ILoadTimeFromFirebaseCallback, ISearchCategoryCallbackListener, TextWatcher {

    /*private var placeSelected: Place?=null
    private var places_fragment: AutocompleteSupportFragment?=null
    private lateinit var placeClient: PlacesClient
    private val placeFields = Arrays.asList(Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG)*/

    private var cartDataSource:CartDataSource?=null
    private var compositeDisposable:CompositeDisposable = CompositeDisposable()
    private var recyclerViewState: Parcelable?=null
    private lateinit var cartViewModel: CartViewModel
    private lateinit var btn_place_order:Button

    var txt_empty_cart:TextView?=null
    var txt_total_price:TextView?=null
    var group_place_holder:CardView?=null
    var recycler_cart:RecyclerView?=null
    var adapter:MyCartAdapter?=null

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var currentLocation: Location

    lateinit var ifcmService:IFCMService

    lateinit var searchCategoryCallbackListener:ISearchCategoryCallbackListener

    private lateinit var addonBottomSheetDialog: BottomSheetDialog
    private var chip_group_user_selected_addon:ChipGroup?=null
    private var chip_group_addon:ChipGroup?=null
    private var edt_search_addon:EditText?=null

    lateinit var listener:ILoadTimeFromFirebaseCallback

    override fun onResume() {
        super.onResume()
        calculateTotalPrice()
        if(fusedLocationProviderClient != null)
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        setHasOptionsMenu(true)

        EventBus.getDefault().postSticky(HideFABCart(true))

        cartViewModel =
            ViewModelProviders.of(this).get(CartViewModel::class.java)
        // cartViewModel oluşturulduktan sonra,verileri kaynaktan çekmek
        cartViewModel.initCartdataSource(context!!)
        val root = inflater.inflate(R.layout.fragment_cart, container, false)
        initViews(root)
        initLocation()
        cartViewModel.getMutableLiveDataCartItem().observe(this, Observer {
            if (it == null || it.isEmpty()) {

                recycler_cart!!.visibility = View.GONE
                group_place_holder!!.visibility = View.GONE
                txt_empty_cart!!.visibility = View.VISIBLE
            } else {
                recycler_cart!!.visibility = View.VISIBLE
                group_place_holder!!.visibility = View.VISIBLE
                txt_empty_cart!!.visibility = View.GONE

                adapter = MyCartAdapter(context!!, it)
                recycler_cart!!.adapter = adapter
            }
        })
        return root
    }

    private fun initLocation() {
        buildLocationRequest()
        buildLocationCallback()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context!!)
        fusedLocationProviderClient!!.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun buildLocationCallback() {

        locationCallback = object: LocationCallback(){

            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                currentLocation = p0!!.lastLocation
            }
        }
    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationRequest.setInterval(5000)
        locationRequest.setFastestInterval(3000)
        locationRequest.setSmallestDisplacement(10f)
    }

    private fun initViews(root: View) {

        searchCategoryCallbackListener = this

        // initPlacesClient()

        setHasOptionsMenu(true)      // Eğer eklenmezse menü doğru çalışmaz

        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService::class.java)

        listener = this

        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context!!).cartDAO())

        addonBottomSheetDialog = BottomSheetDialog(context!!,R.style.DialogStyle)
        val layout_user_selected_addon = layoutInflater.inflate(R.layout.layout_addon_display,null)
        chip_group_addon = layout_user_selected_addon.findViewById(R.id.chip_group_addon) as ChipGroup
        edt_search_addon = layout_user_selected_addon.findViewById(R.id.edt_search) as EditText
        addonBottomSheetDialog.setContentView(layout_user_selected_addon)

        addonBottomSheetDialog.setOnDismissListener{ dialogInterface ->
            displayUserSelectedAddon(chip_group_user_selected_addon)
            calculateTotalPrice()
        }

        recycler_cart=root.findViewById(R.id.recycler_cart) as RecyclerView
        recycler_cart!!.setHasFixedSize(true)
        val layoutManager=LinearLayoutManager(context)
        recycler_cart!!.layoutManager=layoutManager
        recycler_cart!!.addItemDecoration(DividerItemDecoration(context, layoutManager.orientation))

        val swipe = object:MySwipeHelper(context!!, recycler_cart!!, 200){
            override fun instantiateMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<MyButton>
            ) {
                buffer.add(
                    MyButton(context!!,
                        "Sil",
                        30,
                        0,
                        Color.parseColor("#FF3C30"),
                        object : IMyButtonCallBack {
                            override fun onClick(pos: Int) {
                                val deleteItem = adapter!!.getItemAtPosition(pos)
                                cartDataSource!!.deleteCart(deleteItem)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<Int> {
                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onSuccess(t: Int) {
                                            adapter!!.notifyItemRemoved(pos)
                                            sumCart()
                                            EventBus.getDefault().postSticky(CountCartEvent(true))
                                            Toast.makeText(
                                                context,
                                                "Sepetten Başarıyla Kaldırıldı",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }

                                        override fun onError(e: Throwable) {
                                            Toast.makeText(
                                                context,
                                                "" + e.message,
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
                                        }


                                    })

                            }

                        })
                )

                buffer.add(
                    MyButton(context!!,
                        "Güncelle",
                        30,
                        0,
                        Color.parseColor("#5d4037"),
                        object : IMyButtonCallBack {
                            override fun onClick(pos: Int) {

                                val cartItem = adapter!!.getItemAtPosition(pos)
                                FirebaseDatabase.getInstance()
                                    .getReference(Common.CATEGORY_REF)
                                    .child(cartItem.categoryId)
                                    .addListenerForSingleValueEvent(object:ValueEventListener{
                                        override fun onDataChange(dataSnapshot: DataSnapshot) {

                                            if(dataSnapshot.exists()){
                                                val categoryModel = dataSnapshot.getValue(CategoryModel::class.java)
                                                searchCategoryCallbackListener.onSearchFound(categoryModel!!,cartItem)
                                            }
                                            else{

                                                searchCategoryCallbackListener.onSearchNotFound("Kategori Bulunamadı !")
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {

                                            searchCategoryCallbackListener!!.onSearchNotFound(error.message)
                                        }


                                    })

                            }

                        })
                )
            }
        }

        txt_empty_cart=root.findViewById(R.id.txt_empty_cart) as TextView
        txt_total_price=root.findViewById(R.id.txt_total_price) as TextView
        group_place_holder=root.findViewById(R.id.group_place_holder) as CardView

        btn_place_order = root.findViewById(R.id.btn_place_order) as Button

        //Event

        btn_place_order.setOnClickListener{
            val builder = AlertDialog.Builder(context!!)
            builder.setTitle("Bir adım daha!")

            val view = LayoutInflater.from(context).inflate(R.layout.layout_place_order, null)

            val edt_comment = view.findViewById<View>(R.id.edt_comment) as EditText
            val edt_address = view.findViewById<View>(R.id.edt_address) as EditText
            val txt_address = view.findViewById<View>(R.id.txt_address_detail) as TextView
            val rdi_home = view.findViewById<View>(R.id.rdi_home_address) as RadioButton
            val rdi_other_address = view.findViewById<View>(R.id.rdi_other_address) as RadioButton
            val rdi_ship_to_this_address = view.findViewById<View>(R.id.rdi_ship_this_address) as RadioButton
            val rdi_cod = view.findViewById<View>(R.id.rdi_cod) as RadioButton

            /*  places_fragment = activity!!.supportFragmentManager.findFragmentById(R.id.places_autocomplete_fragment) as AutocompleteSupportFragment
              places_fragment!!.setPlaceFields(placeFields)
              places_fragment!!.setOnPlaceSelectedListener(object: PlaceSelectionListener {
                  override fun onPlaceSelected(p0: Place) {
                      placeSelected = p0
                      txt_address.text = placeSelected!!.address
                  }

                  override fun onError(p0: Status) {
                      Toast.makeText(context,""+p0.statusMessage,Toast.LENGTH_SHORT).show()
                  }

              })*/

            //Veri

            edt_address.setText(Common.currentUser!!.address!!)    // Varsayılan olarak ev adresi butonu işaretliyse,kullanıcının adresini görüntülemek

            //Event

            rdi_home.setOnCheckedChangeListener{ compoundButton, b ->
                if(b){
                    edt_address.setText(Common.currentUser!!.address!!)
                    txt_address.visibility=View.GONE

                }
            }
            rdi_other_address.setOnCheckedChangeListener{ compoundButton, b ->
                if(b){

                    edt_address.setText("")
                    edt_address.setHint("Adresinizi Giriniz")
                    txt_address.visibility=View.GONE

                }
            }
            rdi_ship_to_this_address.setOnCheckedChangeListener{ compoundButton, b ->
                if(b){
                    fusedLocationProviderClient!!.lastLocation
                        .addOnFailureListener{ e ->
                            txt_address.visibility = View.GONE
                            Toast.makeText(context!!, "" + e.message, Toast.LENGTH_SHORT).show()}
                        .addOnCompleteListener { task ->
                            val coordinates = StringBuilder()
                                .append(task.result!!.latitude)
                                .append("/")
                                .append(task.result!!.longitude)
                                .toString()

                            val singleAddress = Single.just(getAdressFromLatLng(task.result!!.latitude,
                                task.result!!.longitude))

                            val disposable = singleAddress.subscribeWith(object:DisposableSingleObserver<String>() {
                                override fun onSuccess(t: String) {
                                    edt_address.setText(coordinates)
                                    txt_address.visibility=View.VISIBLE
                                    txt_address.setText(t)
                                }

                                override fun onError(e: Throwable) {
                                    edt_address.setText(coordinates)
                                    txt_address.visibility=View.VISIBLE
                                    txt_address.setText(e.message!!)
                                }


                            })

                        }
                }
            }

            builder.setView(view)
            builder.setNegativeButton("İPTAL", { dialogInterface, _ -> dialogInterface.dismiss() })
                .setPositiveButton("TAMAM", { dialogInterface, _ ->
                    if(rdi_cod.isChecked)
                        paymentCOD(edt_address.text.toString(),edt_comment.text.toString())
                })

            val dialog = builder.create()
            dialog.show()
        }
    }

    private fun displayUserSelectedAddon(chipGroupUserSelectedAddon: ChipGroup?) {
        if(Common.foodSelected!!.userSelectedAddon != null && Common.foodSelected!!.userSelectedAddon!!.size > 0){

            chipGroupUserSelectedAddon!!.removeAllViews()
            for(addonModel in Common.foodSelected!!.userSelectedAddon!!){

                val chip = layoutInflater.inflate(R.layout.layout_chip_with_delete,null) as Chip
                chip.text = StringBuilder(addonModel.name!!).append("+($")
                    .append(addonModel.price).append(")")

                chip.setOnCheckedChangeListener{ compBoundButton, b ->
                    if(b)
                        if(Common.foodSelected!!.userSelectedAddon == null) Common.foodSelected!!.userSelectedAddon = ArrayList()
                    Common.foodSelected!!.userSelectedAddon!!.add(addonModel)
                }

                chipGroupUserSelectedAddon.addView(chip)
            }
        }
        else{

            chipGroupUserSelectedAddon!!.removeAllViews()
        }
    }

    /*private fun initPlacesClient() {
        Places.initialize(context!!,getString(R.string.google_maps_key))
        placeClient = Places.createClient(context!!)
    }*/

    private fun paymentCOD(address: String, comment: String) {
        compositeDisposable.add(cartDataSource!!.getAllCart(Common.currentUser!!.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ cartItemList ->

                //Sepetteki bütün veriler çekilirse,toplam fiyat da çekilmiş olur
                cartDataSource!!.sumPrice(Common.currentUser!!.uid!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object: SingleObserver<Double>{
                        override fun onSubscribe(d: Disposable) {

                        }

                        override fun onSuccess(totalPrice: Double) {
                            val finalPrice = totalPrice
                            val order = OrderModel()
                            order.userId = Common.currentUser!!.uid!!
                            order.userName = Common.currentUser!!.name!!
                            order.userPhone = Common.currentUser!!.phone!!
                            order.shippingAddress = address
                            order.comment = comment

                            if(currentLocation != null){

                                order.lat = currentLocation!!.latitude
                                order.lng = currentLocation!!.longitude
                            }

                            order.cartItemList = cartItemList
                            order.totalPayment = totalPrice
                            order.finalPayment = finalPrice
                            order.discount = 0
                            order.isCod = true
                            order.transactionId = "Kapıda Ödeme"

                            // Firebase'e kaydetmek

                            syncLocalTimeWithServerTime(order)

                        }

                        override fun onError(e: Throwable) {
                            if (!e.message!!.contains("Query returned empty"))
                                Toast.makeText(context,"[SUM CART]"+e.message,Toast.LENGTH_SHORT).show()
                        }


                    })
            },{ throwable -> Toast.makeText(context!!,""+throwable.message,Toast.LENGTH_SHORT).show() }))
    }

    private fun syncLocalTimeWithServerTime(order: OrderModel) {
        val offsetRef = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset")
        offsetRef.addListenerForSingleValueEvent(object:ValueEventListener{
            override fun onDataChange(p0: DataSnapshot) {
                val offset = p0.getValue(Long::class.java)
                val estimatedServerTimeInMs = System.currentTimeMillis()+offset!!   // Kayıp dengelenmiş zamanı sistem saatine eklemek
                val sdf = SimpleDateFormat("MMM dd yyyy, HH:mm")
                val date = Date(estimatedServerTimeInMs)
                Log.d("HCY",""+sdf.format(date))
                listener.onLoadTimeSuccess(order,estimatedServerTimeInMs)
            }

            override fun onCancelled(p0: DatabaseError) {
                listener.onLoadTimeFailed(p0.message)
            }

        })
    }

    private fun writeOrderToFirebase(order: OrderModel) {
        FirebaseDatabase.getInstance()
            .getReference(Common.ORDER_REF)
            .child(Common.createOrderNumber())
            .setValue(order)
            .addOnFailureListener { e -> Toast.makeText(context!!,""+e.message,Toast.LENGTH_SHORT).show() }
            .addOnCompleteListener { task ->
                //Sepeti Temizleme
                if(task.isSuccessful){

                    cartDataSource!!.cleanCart(Common.currentUser!!.uid!!)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object: SingleObserver<Int>{
                            override fun onSubscribe(d: Disposable) {

                            }

                            override fun onSuccess(t: Int) {

                                val dataSend = HashMap<String,String>()
                                dataSend.put(Common.NOTI_TITLE,"Yeni Sipariş")
                                dataSend.put(Common.NOTI_CONTENT,"Yeni bir siparişiniz var "+Common.currentUser!!.phone)

                                val sendData = FCMSendData(Common.getNewOrderTopic(),dataSend)

                                compositeDisposable.add(
                                    ifcmService.sendNotification(sendData)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe({t: FCMResponse? ->
                                            if(t!!.success ==0)
                                                Toast.makeText(context!!, "Sipariş Başarıyla Verildi", Toast.LENGTH_SHORT).show()
                                        },{t: Throwable? ->
                                            Toast.makeText(context!!, "Sipariş Başarıyla Verildi Fakat Bildirim Gönderilemedi", Toast.LENGTH_SHORT).show()
                                        })
                                )


                                EventBus.getDefault().postSticky(CountCartEvent(true))
                            }

                            override fun onError(e: Throwable) {
                                Toast.makeText(context!!, "" + e.message, Toast.LENGTH_SHORT).show()
                            }


                        })
                }
            }
    }

    private fun getAdressFromLatLng(latitude: Double, longitude: Double): String {
        val geoCoder = Geocoder(context!!, Locale.getDefault())
        var result:String?=null
        try{
            val addressList = geoCoder.getFromLocation(latitude,longitude,1)
            if(addressList != null && addressList.size > 0){

                val address = addressList[0]
                val sb = StringBuilder(address.getAddressLine(0))
                result = sb.toString()
            }
            else
                result="Adres bulunamadı!"
            return result
        }catch(e:IOException){
            return e.message!!
        }
    }

    private fun sumCart() {
        cartDataSource!!.sumPrice(Common.currentUser!!.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Double> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: Double) {
                    txt_total_price!!.text = StringBuilder("Toplam: ₺")
                        .append(t)
                }

                override fun onError(e: Throwable) {
                    if (!e.message!!.contains("Query returned empty"))
                        Toast.makeText(context!!, "" + e.message!!, Toast.LENGTH_SHORT).show()
                }


            })
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu!!.findItem(R.id.action_settings).setVisible(false)    // Sepetteyken Ayarlar butonunu gizlemek
        super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater!!.inflate(R.menu.cart_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item!!.itemId == R.id.action_clear_cart){
            cartDataSource!!.cleanCart(Common.currentUser!!.uid!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<Int> {
                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onSuccess(t: Int) {
                        Toast.makeText(context, "Sepeti Temizleme Başarılı", Toast.LENGTH_SHORT).show()
                        EventBus.getDefault().postSticky(CountCartEvent(true))
                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show()
                    }

                })
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        if(!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
    }

    override fun onStop() {
        cartViewModel!!.onStop()
        compositeDisposable.clear()
        EventBus.getDefault().postSticky(HideFABCart(false))
        if(EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this)
        if(fusedLocationProviderClient != null)
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onStop()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onUpdateItemInCart(event: UpdateItemInCart){
        if(event.cartItem != null){
            recyclerViewState=recycler_cart!!.layoutManager!!.onSaveInstanceState()
            cartDataSource!!.updateCart(event.cartItem)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<Int> {
                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onSuccess(t: Int) {
                        calculateTotalPrice();
                        recycler_cart!!.layoutManager!!.onRestoreInstanceState(recyclerViewState)
                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(context, "[UPDATE CART]" + e.message, Toast.LENGTH_SHORT)
                            .show()
                    }

                })
        }
    }

    private fun calculateTotalPrice() {
        cartDataSource!!.sumPrice(Common.currentUser!!.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Double> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(price: Double) {
                    txt_total_price!!.text = StringBuilder("Toplam: ₺")
                        .append(Common.formatPrice(price))
                }

                override fun onError(e: Throwable) {
                    if (!e.message!!.contains("Query returned empty"))
                        Toast.makeText(context, "[SUM CART]" + e.message, Toast.LENGTH_SHORT).show()
                }

            })
    }

    override fun onLoadTimeSuccess(order: OrderModel, estimatedTimeMs: Long) {
        order.createDate = (estimatedTimeMs)
        order.orderStatus = 0
        writeOrderToFirebase(order)
    }

    override fun onLoadTimeFailed(message: String) {
        Toast.makeText(context!!,message,Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }

    override fun onSearchFound(category: CategoryModel, cartItem: CartItem) {
        val foodModel:FoodModel = Common.findFoodInListById(category,cartItem!!.foodId)!!
        if(foodModel != null)
            showUpdateDialog(cartItem,foodModel)
        else
            Toast.makeText(context!!,"Yemek ID'si bulunamadı!",Toast.LENGTH_SHORT).show()
    }

    private fun showUpdateDialog(cartItem: CartItem, foodModel: FoodModel) {

        Common.foodSelected = foodModel
        val builder = androidx.appcompat.app.AlertDialog.Builder(context!!)
        val itemView:View = LayoutInflater.from(context!!).inflate(R.layout.layout_dialog_update_cart,null)
        builder.setView(itemView)

        //View

        val btn_ok = itemView.findViewById<View>(R.id.btn_ok) as Button
        val btn_cancel = itemView.findViewById<View>(R.id.btn_cancel) as Button

        val rdi_group_size = itemView.findViewById<View>(R.id.rdi_group_size) as RadioGroup
        chip_group_user_selected_addon = itemView.findViewById<View>(R.id.chip_group_user_selected_addon) as ChipGroup
        val img_add_on = itemView.findViewById<View>(R.id.img_add_addon) as ImageView

        img_add_on.setOnClickListener{

            if(foodModel.addon != null){

                displayAddonList()
                addonBottomSheetDialog!!.show()
            }
        }

        // Porsiyon

        if(foodModel.size != null){

            for(sizeModel in foodModel.size){

                val radioButton = RadioButton(context)
                radioButton.setOnCheckedChangeListener{ compoundButton, b ->
                    if(b) Common.foodSelected!!.userSelectedSize = sizeModel
                    calculateTotalPrice()
                }
                val params = LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.MATCH_PARENT,1.0f)
                radioButton.layoutParams = params
                radioButton.setText(sizeModel.name)
                radioButton.tag = (sizeModel.price)
                rdi_group_size.addView(radioButton)
            }
            if(rdi_group_size.childCount > 0){
                val radioButton = rdi_group_size.getChildAt(0) as RadioButton
                radioButton.isChecked = true // Varsayılan işaretli
            }
        }

        // Eklenti

        displayAlreadySelectedAddon(chip_group_user_selected_addon!!,cartItem) //fix

        // Dialog penceresi

        val dialog = builder.create()
        dialog.show()

        // Kişiselleştirilmiş

        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.setGravity(Gravity.CENTER)
        //Event
        btn_cancel.setOnClickListener{ dialog.dismiss() }
        btn_ok.setOnClickListener{

            // Önce malzemeyi silme
            cartDataSource!!.deleteCart(cartItem)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object:SingleObserver<Int>{
                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onSuccess(t: Int) {
                        // Başarıyla silinmesi durumunda yeni bilgiyle sepetin güncellenmesi gerekiyor
                        if(Common.foodSelected!!.userSelectedAddon != null)
                            cartItem.foodAddon = Gson().toJson(Common.foodSelected!!.userSelectedAddon)
                        else
                            cartItem.foodAddon="Default"
                        if(Common.foodSelected!!.userSelectedSize != null)
                            cartItem.foodSize = Gson().toJson(Common.foodSelected!!.userSelectedSize)
                        else
                            cartItem.foodSize="Default"

                        cartItem.foodExtraPrice = Common.calculateExtraPrice(Common.foodSelected!!.userSelectedSize,Common.foodSelected!!.userSelectedAddon!!)

                        // Sepete eklemek
                        compositeDisposable.add(cartDataSource!!.insertOrReplaceAll(cartItem)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
                                EventBus.getDefault().postSticky(CountCartEvent(true))
                                dialog.dismiss()
                                calculateTotalPrice()
                                Toast.makeText(context,"Sepete Başarıyla Güncellendi",Toast.LENGTH_SHORT).show()
                            },{
                                    t: Throwable? -> Toast.makeText(context,"[INSERT CART]"+t!!.message,Toast.LENGTH_SHORT).show()
                            }))
                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(context!!,e.message,Toast.LENGTH_SHORT).show()
                    }


                })
        }
    }

    private fun displayAlreadySelectedAddon(chipGroupUserSelectedAddon: ChipGroup, cartItem: CartItem) {
        // Bu fonksiyon daha önce eklenen görüntülerdeki bütün eklentileri gösterecek
            if(cartItem.foodAddon != null && !cartItem.foodAddon.equals("Default")){
            val addonModels: List<AddonModel> = Gson().fromJson(cartItem.foodAddon, object : TypeToken<List<AddonModel>>() {}.type)
            Common.foodSelected!!.userSelectedAddon = addonModels.toMutableList()
            chipGroupUserSelectedAddon.removeAllViews()
            // Bütün görüntüleri yükleme
            for (addonModel in addonModels) {

                val chip = layoutInflater.inflate(R.layout.layout_chip_with_delete, null) as Chip
                chip.text = StringBuilder(addonModel.name!!).append("(+$")
                        .append(addonModel.price).append(")")
                chip.isClickable = false
                chip.setOnCloseIconClickListener {
                    chipGroupUserSelectedAddon.removeView(it)
                    Common.foodSelected!!.userSelectedAddon!!.remove(addonModel)
                    calculateTotalPrice()
                }
                chipGroupUserSelectedAddon.addView(chip)
            }
            }
    }

    private fun displayAddonList() {

        if(Common.foodSelected!!.addon != null && Common.foodSelected!!.addon.size > 0){

            chip_group_addon!!.clearCheck()
            chip_group_addon!!.removeAllViews()
            edt_search_addon!!.addTextChangedListener(this)

            for(addonModel in Common.foodSelected!!.addon){

                val chip = layoutInflater.inflate(R.layout.layout_chip,null) as Chip
                chip.text = StringBuilder(addonModel.name!!).append("(+$")
                    .append(addonModel.price).append(")")
                chip.setOnCheckedChangeListener{compoundButton, b ->
                    if(b)
                        if(Common.foodSelected!!.userSelectedAddon == null)
                            Common.foodSelected!!.userSelectedAddon = ArrayList()
                    Common.foodSelected!!.userSelectedAddon!!.add(addonModel)
                }
                chip_group_addon!!.addView(chip)
            }
        }
    }

    override fun onSearchNotFound(message: String) {
        Toast.makeText(context!!,message,Toast.LENGTH_SHORT).show()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        chip_group_addon!!.clearCheck()
        chip_group_addon!!.removeAllViews()

        for(addonModel in Common.foodSelected!!.addon) {

            if (addonModel.name!!.toLowerCase().contains(p0.toString().toLowerCase())) {

                val chip = layoutInflater.inflate(R.layout.layout_chip, null) as Chip
                chip.text = StringBuilder(addonModel.name!!).append("(+$")
                    .append(addonModel.price).append(")")
                chip.setOnCheckedChangeListener { compoundButton, b ->
                    if (b)
                        if (Common.foodSelected!!.userSelectedAddon == null)
                            Common.foodSelected!!.userSelectedAddon = ArrayList()
                    Common.foodSelected!!.userSelectedAddon!!.add(addonModel)
                }
                chip_group_addon!!.addView(chip)
            }
        }
    }

    override fun afterTextChanged(s: Editable?) {

    }
}