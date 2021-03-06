package com.example.chefstation

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NavigationRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import com.andremion.counterfab.CounterFab
import com.example.chefstation.Common.Common
import com.example.chefstation.Database.CartDataSource
import com.example.chefstation.Database.CartDatabase
import com.example.chefstation.Database.LocalCartDataSource
import com.example.chefstation.EventBus.*
import com.example.chefstation.Model.CategoryModel
import com.example.chefstation.Model.FoodModel
import com.example.chefstation.Model.PopularCategoryModel
import com.example.chefstation.Model.UserModel
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import dmax.dialog.SpotsDialog
import io.paperdb.Paper
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.app_bar_home.*
import kotlinx.android.synthetic.main.layout_register.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap

class HomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var cartDataSource: CartDataSource
    private lateinit var navController:NavController
    private var drawer:DrawerLayout?=null
    private var dialog:android.app.AlertDialog?=null
    private var menuItemClick=-1

    private var placeSelected:Place?=null
    private var places_fragment:AutocompleteSupportFragment?=null
    private lateinit var placeClient: PlacesClient
    private val placeFields = Arrays.asList(Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG)

    override fun onResume() {
        super.onResume()
        countCartItem()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        dialog = SpotsDialog.Builder().setContext(this).setCancelable(false).build()

        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(this).cartDAO())

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            navController.navigate(R.id.nav_cart)
        }
        drawer = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        navController = findNavController(R.id.nav_host_fragment)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_menu,R.id.nav_food_detail,
                R.id.nav_cart, R.id.nav_update_info,R.id.nav_news
            ), drawer
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        var headerView = navView.getHeaderView(0)
        var txt_user = headerView.findViewById<TextView>(R.id.txt_user)
        Common.setSpanString("Merhaba ",Common.currentUser!!.name,txt_user)

        navView.setNavigationItemSelectedListener(object:NavigationView.OnNavigationItemSelectedListener{
            override fun onNavigationItemSelected(p0: MenuItem): Boolean {

                p0.isChecked = true
                drawer!!.closeDrawers()

                if(p0.itemId == R.id.nav_sign_out){
                    signOut()
                }
                else if(p0.itemId == R.id.nav_home){
                    if(menuItemClick != p0.itemId)
                        navController.navigate(R.id.nav_home)
                }
                else if(p0.itemId == R.id.nav_cart){
                    if(menuItemClick != p0.itemId)
                        navController.navigate(R.id.nav_cart)
                }
                else if(p0.itemId == R.id.nav_menu){
                    if(menuItemClick != p0.itemId)
                        navController.navigate(R.id.nav_menu)
                }
                else if(p0.itemId == R.id.nav_view_order){
                    if(menuItemClick != p0.itemId)
                        navController.navigate(R.id.nav_view_order)
                }
                else if(p0.itemId == R.id.nav_update_info){

                    showUpdateInfoDialog()
                }
                else if(p0.itemId == R.id.nav_news){

                    showNewsDialog()
                }

                menuItemClick = p0!!.itemId

                return true
            }

        })

       // initPlacesClient()

        countCartItem()
    }

    private fun showNewsDialog() {

        Paper.init(this)

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("HABER S??STEM??")
        builder.setMessage("Haber sistemine abone olmak istiyor musunuz?")

        val itemView =
            LayoutInflater.from(this@HomeActivity)
                .inflate(R.layout.layout_subscribe_news, null)
        val ckb_news = itemView.findViewById<View>(R.id.ckb_subscribe_news) as CheckBox
        val isSubscribeNews = Paper.book().read<Boolean>(Common.IS_SUBSCRIBE_NEWS,true)
        if(isSubscribeNews)
            ckb_news.isChecked = true
        builder.setNegativeButton( "HAYIR",{dialogInterface, i ->
            dialogInterface.dismiss()
            Paper.book().delete(Common.IS_SUBSCRIBE_NEWS)
            FirebaseMessaging.getInstance().unsubscribeFromTopic(Common.NEWS_TOPIC)
                .addOnFailureListener { e:Exception ->
                    Toast.makeText(this,e.message,Toast.LENGTH_SHORT).show()
                }
                .addOnSuccessListener { aVoid:Void? ->
                    Toast.makeText(this,"Kay??t Silme ????lemi Ba??ar??l??!",Toast.LENGTH_SHORT).show()
                }})
        builder.setPositiveButton("EVET",{dialogInterface, i ->
          //  if(ckb_news.isChecked){
                Paper.book().write(Common.IS_SUBSCRIBE_NEWS,true)
                FirebaseMessaging.getInstance().subscribeToTopic(Common.NEWS_TOPIC)
                    .addOnFailureListener { e:Exception ->
                        Toast.makeText(this,e.message,Toast.LENGTH_SHORT).show()
                    }
                    .addOnSuccessListener { aVoid:Void? ->
                        Toast.makeText(this,"Kay??t Ba??ar??l??!",Toast.LENGTH_SHORT).show()
                    }
           // }

            /*else{
                Paper.book().delete(Common.IS_SUBSCRIBE_NEWS)
                FirebaseMessaging.getInstance().unsubscribeFromTopic(Common.NEWS_TOPIC)
                    .addOnFailureListener { e:Exception ->
                        Toast.makeText(this,e.message,Toast.LENGTH_SHORT).show()
                    }
                    .addOnSuccessListener { aVoid:Void? ->
                        Toast.makeText(this,"Kay??t Silme ????lemi Ba??ar??l??!",Toast.LENGTH_SHORT).show()
                    }

            }*/
        })

        val dialog = builder.create()
        dialog.show()
    }

    private fun initPlacesClient() {
        Places.initialize(this,getString(R.string.google_maps_key))
        placeClient = Places.createClient(this)
    }

    private fun showUpdateInfoDialog() {

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)

        builder.setTitle("KAYIT")
        builder.setMessage("L??tfen gerekli bilgileri doldurunuz")

        val itemView =
                LayoutInflater.from(this@HomeActivity)
                        .inflate(R.layout.layout_register, null)

        val edt_name = itemView.findViewById<EditText>(R.id.edt_name)
        val edt_phone = itemView.findViewById<EditText>(R.id.edt_phone)
        val txt_address = itemView.findViewById<EditText>(R.id.edt_address)

       /* places_fragment = supportFragmentManager.findFragmentById(R.id.places_autocomplete_fragment) as AutocompleteSupportFragment
        places_fragment!!.setPlaceFields(placeFields)
        places_fragment!!.setOnPlaceSelectedListener(object: PlaceSelectionListener {
            override fun onPlaceSelected(p0: Place) {
                placeSelected = p0
                txt_address.text = placeSelected!!.address
            }

            override fun onError(p0: Status) {
                Toast.makeText(this@HomeActivity,""+p0.statusMessage,Toast.LENGTH_SHORT).show()
            }

        })*/

        edt_phone.setText(Common.currentUser!!.phone)
        txt_address.setText(Common.currentUser!!.address)
        edt_name.setText(Common.currentUser!!.name)

        builder.setView(itemView)
        builder.setNegativeButton("??PTAL") { dialogInterface, i -> dialogInterface.dismiss() }
        builder.setPositiveButton("G??NCELLE") { dialogInterface, i ->

           // if(placeSelected != null) {

                if (TextUtils.isDigitsOnly(edt_name.text.toString())) {
                    Toast.makeText(this@HomeActivity, "L??tfen ??sminizi Giriniz", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (TextUtils.isDigitsOnly(edt_address.text.toString())) {
                    Toast.makeText(this@HomeActivity, "L??tfen Adresi Se??iniz", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                 if (TextUtils.isDigitsOnly(edt_phone.text.toString())) {
                Toast.makeText(this@HomeActivity, "" + "L??tfen Telefon Numaran??z?? Giriniz", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val update_data = HashMap<String,Any>()

            update_data.put("name",edt_name.text.toString())
            update_data.put("address",txt_address.text.toString())
           // update_data.put("lat",placeSelected!!.latLng!!.latitude)
           // update_data.put("lng",placeSelected!!.latLng!!.longitude)

            FirebaseDatabase.getInstance()
                    .getReference(Common.USER_REFERENCE)
                    .child(Common.currentUser!!.uid!!)
                    .updateChildren(update_data)
                    .addOnFailureListener{
                        Toast.makeText(this@HomeActivity,"L??tfen Adresi Se??iniz",Toast.LENGTH_SHORT).show()
                    }
                    .addOnSuccessListener {
                        Common.currentUser!!.name = update_data["name"].toString()
                        Common.currentUser!!.address = update_data["address"].toString()
                       // Common.currentUser!!.lat = update_data["lat"].toString().toDouble()
                       // Common.currentUser!!.lng = update_data["lng"].toString().toDouble()
                        Toast.makeText(this@HomeActivity,"Bilgi G??ncelleme Ba??ar??l??",Toast.LENGTH_SHORT).show()
                    }

        //    } else{


  //          }
        }

        val dialog = builder.create()
        /*dialog.setOnDismissListener{
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.remove(places_fragment!!)
            fragmentTransaction.commit()
        }*/
        dialog.show()
    }

    private fun signOut(){

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("????k????")
                    .setMessage("Ger??ekten ????kmak istiyor musunuz?")
                    .setNegativeButton("Hay??r",{dialogInterface, _ -> dialogInterface.dismiss()})
                    .setPositiveButton("Evet"){dialogInterface, _ ->
                    Common.foodSelected = null
                    Common.categorySelected = null
                    Common.currentUser = null
                    FirebaseAuth.getInstance().signOut()

                    val intent = Intent(this@HomeActivity,MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }

        val dialog = builder.create()
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    // Event bus

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)

    fun onCategorySelected(event:CategoryClick) {

        if (event.isSuccess) {

            //Toast.makeText(this,event.category.name+" ????in T??klay??n",Toast.LENGTH_SHORT).show()
            findNavController(R.id.nav_host_fragment).navigate(R.id.nav_food_list)

        }
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)

    fun onFoodSelected(event: FoodItemClick) {

        if (event.isSuccess) {


            findNavController(R.id.nav_host_fragment).navigate(R.id.nav_food_detail)

        }
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)

    fun onHideFABEvent(event: HideFABCart) {

        if (event.isHide) {

            fab.hide()

        }
        else
            fab.show()
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)

    fun onCountCartEvent(event: CountCartEvent) {

        if (event.isSuccess) {

            countCartItem()

        }
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)

    fun onPopularFoodItemClick(event: PopularFoodItemClick) {

        if (event.popularCategoryModel != null) {

            dialog!!.show()

            FirebaseDatabase.getInstance()
                    .getReference("Category")
                    .child(event.popularCategoryModel!!.menu_id!!)
                    .addListenerForSingleValueEvent(object:ValueEventListener{
                        override fun onDataChange(p0: DataSnapshot) {

                            if(p0.exists()){

                                Common.categorySelected = p0.getValue(CategoryModel::class.java)
                                Common.categorySelected!!.menu_id = p0.key

                                // Yeme??i y??klemek

                                FirebaseDatabase.getInstance()
                                        .getReference("Category")
                                        .child(event.popularCategoryModel!!.menu_id!!)
                                        .child("foods")
                                        .orderByChild("id")
                                        .equalTo(event.popularCategoryModel.food_id)
                                        .limitToLast(1)
                                        .addListenerForSingleValueEvent(object:ValueEventListener{
                                            override fun onDataChange(p0: DataSnapshot) {
                                                if(p0.exists()){

                                                    for(foodSnapShot in p0.children){

                                                        Common.foodSelected = foodSnapShot.getValue(FoodModel::class.java)
                                                        Common.foodSelected!!.key = foodSnapShot.key
                                                    }

                                                    navController!!.navigate(R.id.nav_food_detail)
                                                }
                                                else{

                                                    Toast.makeText(this@HomeActivity,"De??er bulunamad??",Toast.LENGTH_SHORT).show()
                                                }
                                                dialog!!.dismiss()
                                            }

                                            override fun onCancelled(p0: DatabaseError) {
                                                dialog!!.dismiss()
                                                Toast.makeText(this@HomeActivity,""+p0.message,Toast.LENGTH_SHORT).show()
                                            }


                                        })

                            }
                            else{
                                dialog!!.dismiss()
                                Toast.makeText(this@HomeActivity,"De??er bulunamad??",Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onCancelled(p0: DatabaseError) {
                            dialog!!.dismiss()
                            Toast.makeText(this@HomeActivity,""+p0.message,Toast.LENGTH_SHORT).show()
                        }


                    })

        }
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)

    fun onBestDealFoodItemClick(event: BestDealItemClick) {

        if (event.model != null) {

            dialog!!.show()

            FirebaseDatabase.getInstance()
                    .getReference("Category")
                    .child(event.model!!.menu_id!!)
                    .addListenerForSingleValueEvent(object:ValueEventListener{
                        override fun onDataChange(p0: DataSnapshot) {

                            if(p0.exists()){

                                Common.categorySelected = p0.getValue(CategoryModel::class.java)
                                Common.categorySelected!!.menu_id = p0.key

                                // Yeme??i y??klemek

                                FirebaseDatabase.getInstance()
                                        .getReference("Category")
                                        .child(event.model!!.menu_id!!)
                                        .child("foods")
                                        .orderByChild("id")
                                        .equalTo(event.model.food_id)
                                        .limitToLast(1)
                                        .addListenerForSingleValueEvent(object:ValueEventListener{
                                            override fun onDataChange(p0: DataSnapshot) {
                                                if(p0.exists()){

                                                    for(foodSnapShot in p0.children){

                                                        Common.foodSelected = foodSnapShot.getValue(FoodModel::class.java)
                                                        Common.foodSelected!!.key = foodSnapShot.key
                                                    }

                                                    navController!!.navigate(R.id.nav_food_detail)
                                                }
                                                else{

                                                    Toast.makeText(this@HomeActivity,"De??er bulunamad??",Toast.LENGTH_SHORT).show()
                                                }
                                                dialog!!.dismiss()
                                            }

                                            override fun onCancelled(p0: DatabaseError) {
                                                dialog!!.dismiss()
                                                Toast.makeText(this@HomeActivity,""+p0.message,Toast.LENGTH_SHORT).show()
                                            }


                                        })

                            }
                            else{
                                dialog!!.dismiss()
                                Toast.makeText(this@HomeActivity,"De??er bulunamad??",Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onCancelled(p0: DatabaseError) {
                            dialog!!.dismiss()
                            Toast.makeText(this@HomeActivity,""+p0.message,Toast.LENGTH_SHORT).show()
                        }


                    })

        }
    }

    @Subscribe(sticky= true,threadMode = ThreadMode.MAIN)
    public fun onMenuItemBack(event:MenuItemBack){

        menuItemClick = -1
        if(supportFragmentManager.backStackEntryCount > 0)
            supportFragmentManager.popBackStack()
    }

    private fun countCartItem() {

        cartDataSource.countItemInCart(Common.currentUser!!.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object: SingleObserver<Int> {

                override fun onSuccess(t: Int) {
                    val fab:CounterFab = findViewById(R.id.fab)
                    fab.count = t
                }

                override fun onSubscribe(d: Disposable) {

                }

                override fun onError(e: Throwable) {
                    if(!e.message!!.contains("Query returned empty"))
                        Toast.makeText(this@HomeActivity,"[COUNT CART]"+e.message,Toast.LENGTH_SHORT).show()
                    else
                        fab.count = 0
                }

            })

    }
}