package com.example.chefstation.ui.fooddetail

import android.app.AlertDialog
import android.media.Image
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.andremion.counterfab.CounterFab
import com.bumptech.glide.Glide
import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton
import com.example.chefstation.Common.Common
import com.example.chefstation.Database.CartDataSource
import com.example.chefstation.Database.CartDatabase
import com.example.chefstation.Database.CartItem
import com.example.chefstation.Database.LocalCartDataSource
import com.example.chefstation.EventBus.CountCartEvent
import com.example.chefstation.EventBus.MenuItemBack
import com.example.chefstation.Model.CommentModel
import com.example.chefstation.Model.FoodModel
import com.example.chefstation.R
import com.example.chefstation.ui.comment.CommentFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import com.google.gson.Gson
import dmax.dialog.SpotsDialog
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import java.lang.StringBuilder

class FoodDetailFragment : Fragment(), TextWatcher {

    private val compositeDisposable = CompositeDisposable()
    private lateinit var cartDataSource:CartDataSource

    private lateinit var foodDetailViewModel: FoodDetailViewModel

    private lateinit var addonBottomSheetDialog:BottomSheetDialog

    private var img_food:ImageView?=null
    private var btnCart:CounterFab?=null
    private var btnRating:FloatingActionButton?=null
    private var food_name:TextView?=null
    private var food_description:TextView?=null
    private var food_price:TextView?=null
    private var number_button:ElegantNumberButton?=null
    private var ratingBar:RatingBar?=null
    private var btnShowComment:Button?=null
    private var rdi_group_size:RadioGroup?=null
    private var img_add_on:ImageView?=null
    private var chip_group_user_selected_addon:ChipGroup?=null

    //Eklenecek ??r??n

    private var chip_group_addon:ChipGroup?=null
    private var edt_search_addon:EditText?=null


    private var waitingDialog:android.app.AlertDialog?=null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        foodDetailViewModel =
            ViewModelProvider(this).get(FoodDetailViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_food_detail, container, false)
        initViews(root)

        foodDetailViewModel.getMutableLiveDataFood().observe(viewLifecycleOwner, Observer {
            displayInfo(it)
        })

        foodDetailViewModel.getMutableLiveDataComment().observe(this,Observer{
            submitRatingToFirebase(it)
        })
        return root
    }

    private fun submitRatingToFirebase(commentModel:CommentModel?) {

        waitingDialog!!.show()

        // ??nce yorumu Comment ??zerine kaydetmek gerekir
        FirebaseDatabase.getInstance()
                .getReference(Common.COMMENT_REF)
                .child(Common.foodSelected!!.id!!)
                .push()
                .setValue(commentModel)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful)
                    {
                        addRatingToFood(commentModel!!.ratingValue.toDouble())
                    }
                        waitingDialog!!.dismiss()
                }

    }

    private fun addRatingToFood(ratingValue: Double) {

        FirebaseDatabase.getInstance()
                .getReference(Common.CATEGORY_REF)              // Kategoriyi se??mek
                .child(Common.categorySelected!!.menu_id!!)     // Men??y?? se??mek
                .child("foods")                        // Yiyecek dizisini se??mek
                .child(Common.foodSelected!!.key!!)              // Keyi se??mek
                .addListenerForSingleValueEvent(object:ValueEventListener{
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if(dataSnapshot.exists())
                        {
                            val foodModel = dataSnapshot.getValue(FoodModel::class.java)
                            foodModel!!.key = Common.foodSelected!!.key
                            // Puanlamay?? uygulamak
                            val sumRating = foodModel.ratingValue!!.toDouble() + (ratingValue)
                            val ratingCount = foodModel.ratingCount+1


                            val updateData = HashMap<String,Any>()
                            updateData["ratingValue"] = sumRating
                            updateData["ratingCount"] = ratingCount

                            // De??i??kenlerdeki veriyi g??ncellemek

                            foodModel.ratingCount = ratingCount
                            foodModel.ratingValue = sumRating

                            dataSnapshot.ref
                                    .updateChildren(updateData)
                                    .addOnCompleteListener{task ->
                            waitingDialog!!.dismiss()
                            if(task.isSuccessful){

                                Common.foodSelected = foodModel
                                foodDetailViewModel!!.setFoodModel(foodModel)
                                Toast.makeText(context!!,"Te??ekk??rler",Toast.LENGTH_SHORT).show()
                            }
                        }
                        }
                        else
                            waitingDialog!!.dismiss()

                    }

                    override fun onCancelled(p0: DatabaseError) {
                        waitingDialog!!.dismiss()
                        Toast.makeText(context!!,""+p0.message,Toast.LENGTH_SHORT).show()
                    }

                })
    }

    private fun displayInfo(it: FoodModel?) {

        Glide.with(context!!).load(it!!.image).into(img_food!!)
        food_name!!.text = StringBuilder(it!!.name!!)
        food_description!!.text = StringBuilder(it!!.description!!)
        food_price!!.text = StringBuilder(it!!.price!!.toString())

        ratingBar!!.rating = it!!.ratingValue.toFloat() / it!!.ratingCount

        // Boyut Ayarlama

        for(sizeModel in it!!.size){

            val radioButton = RadioButton(context)
            radioButton.setOnCheckedChangeListener{ compoundButton, b ->
                if(b)
                    Common.foodSelected!!.userSelectedSize = sizeModel
                calculateTotalPrice()
            }

            val params = LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.MATCH_PARENT,1.0f)

            radioButton.layoutParams = params
            radioButton.text = sizeModel.name
            radioButton.tag = sizeModel.price

            rdi_group_size!!.addView(radioButton)
        }

        // Varsay??lan olarak ilk radyo butonun se??ilmesi

        if(rdi_group_size!!.childCount > 0){

            val radioButton = rdi_group_size!!.getChildAt(0) as RadioButton
            radioButton.isChecked = true
        }
    }

    private fun calculateTotalPrice() {

        var totalPrice = Common.foodSelected!!.price.toDouble()
        var displayPrice = 0.0

        // Eklenti

        if(Common.foodSelected!!.userSelectedAddon != null && Common.foodSelected!!.userSelectedAddon!!.size > 0){

            for(addonModel in Common.foodSelected!!.userSelectedAddon!!)
                totalPrice += addonModel.price!!.toDouble()
        }

        // Boyut
        totalPrice += Common.foodSelected!!.userSelectedSize!!.price!!.toDouble()
        displayPrice = totalPrice * number_button!!.number.toInt()
        displayPrice = Math.round(displayPrice * 100.0)/100.0

        food_price!!.text = StringBuilder("").append(Common.formatPrice(displayPrice)).toString()
    }

    private fun initViews(root: View?) {

        (activity as AppCompatActivity).supportActionBar!!.setTitle(Common.foodSelected!!.name)

        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context!!).cartDAO())

        addonBottomSheetDialog = BottomSheetDialog(context!!,R.style.DialogStyle)
        val layout_user_selected_addon = layoutInflater.inflate(R.layout.layout_addon_display,null)
        chip_group_addon = layout_user_selected_addon.findViewById(R.id.chip_group_addon) as ChipGroup
        edt_search_addon = layout_user_selected_addon.findViewById(R.id.edt_search) as EditText
        addonBottomSheetDialog.setContentView(layout_user_selected_addon)

        addonBottomSheetDialog.setOnDismissListener{ dialogInterface ->
            displayUserSelectedAddon()
            calculateTotalPrice()
        }

        waitingDialog = SpotsDialog.Builder().setContext(context!!).setCancelable(false).build()

        btnCart = root!!.findViewById(R.id.btnCart) as CounterFab
        img_food = root!!.findViewById(R.id.img_food) as ImageView
        btnRating = root!!.findViewById(R.id.btn_rating) as FloatingActionButton
        food_name = root!!.findViewById(R.id.food_name) as TextView
        food_description = root!!.findViewById(R.id.food_description) as TextView
        food_price = root!!.findViewById(R.id.food_price) as TextView
        number_button = root!!.findViewById(R.id.number_button) as ElegantNumberButton
        ratingBar = root!!.findViewById(R.id.ratingBar) as RatingBar
        btnShowComment = root!!.findViewById(R.id.btnShowComment) as Button
        rdi_group_size = root!!.findViewById(R.id.rdi_group_size) as RadioGroup
        img_add_on = root!!.findViewById(R.id.img_add_addon) as ImageView
        chip_group_user_selected_addon = root!!.findViewById(R.id.chip_group_user_selected_addon) as ChipGroup


        //Event

        img_add_on!!.setOnClickListener{
            if(Common.foodSelected!!.addon != null){

                displayAllAddon()
                addonBottomSheetDialog.show()
            }

        }

        btnRating!!.setOnClickListener {

            showDialogRating()
        }

        btnShowComment!!.setOnClickListener{

            val commentFragment = CommentFragment.getInstance()
            commentFragment.show(activity!!.supportFragmentManager,"CommentFragment")
        }

        btnCart!!.setOnClickListener{

            val cartItem = CartItem()
            cartItem.uid = Common.currentUser!!.uid!!
            cartItem.userPhone = Common.currentUser!!.phone

            cartItem.categoryId = Common.categorySelected!!.menu_id!!
            cartItem.foodId = Common.foodSelected!!.id!!
            cartItem.foodName = Common.foodSelected!!.name!!
            cartItem.foodImage = Common.foodSelected!!.image!!
            cartItem.foodPrice = Common.foodSelected!!.price!!.toDouble()
            cartItem.foodQuantity=number_button!!.number.toInt()
            cartItem.foodExtraPrice=Common.calculateExtraPrice(Common.foodSelected!!.userSelectedSize,Common.foodSelected!!.userSelectedAddon)
            if(Common.foodSelected!!.userSelectedAddon !=null)
                cartItem.foodAddon = Gson().toJson(Common.foodSelected!!.userSelectedAddon)
            else
                cartItem.foodAddon = "Default"
            if(Common.foodSelected!!.userSelectedSize != null)
                cartItem.foodSize = Gson().toJson(Common.foodSelected!!.userSelectedSize)
            else
                cartItem.foodSize="Default"

            cartDataSource.getItemWithAllOptionsInCart(Common.currentUser!!.uid!!,
                cartItem.categoryId,
                cartItem.foodId,
                cartItem.foodSize!!,
                cartItem.foodAddon!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object: SingleObserver<CartItem> {
                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onSuccess(cartItemFromDB: CartItem) {

                        if(cartItemFromDB.equals(cartItem)){

                            // E??er veri veri taban??nda varsa sadece g??ncellemek

                            cartItemFromDB.foodExtraPrice = cartItem.foodExtraPrice;
                            cartItemFromDB.foodAddon = cartItem.foodAddon
                            cartItemFromDB.foodSize = cartItem.foodSize
                            cartItemFromDB.foodQuantity = cartItemFromDB.foodQuantity + cartItem.foodQuantity

                            cartDataSource.updateCart(cartItemFromDB)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(object: SingleObserver<Int> {
                                    override fun onSubscribe(d: Disposable) {

                                    }

                                    override fun onSuccess(t: Int) {
                                        Toast.makeText(context,"Sepet G??ncelleme Ba??ar??l??",Toast.LENGTH_SHORT).show()
                                        EventBus.getDefault().postSticky(CountCartEvent(true))
                                    }

                                    override fun onError(e: Throwable) {
                                        Toast.makeText(context,"[UPDATE CART]"+e.message,Toast.LENGTH_SHORT).show()
                                    }

                                })
                        }
                        else{

                            // E??er veri veritaban??nda yoksa sadece ekle

                            compositeDisposable.add(cartDataSource.insertOrReplaceAll(cartItem)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({
                                    Toast.makeText(context,"Sepete Ba??ar??yla Eklendi",Toast.LENGTH_SHORT).show()
                                    // CounterFab'?? g??ncellemek i??in HomeActivity'e bir bildirim g??nderilecek
                                    EventBus.getDefault().postSticky(CountCartEvent(true))
                                },{
                                        t: Throwable? -> Toast.makeText(context,"[INSERT CART]"+t!!.message,Toast.LENGTH_SHORT).show()
                                }))
                        }

                    }

                    override fun onError(e: Throwable) {
                        if(e.message!!.contains("empty")){

                            compositeDisposable.add(cartDataSource.insertOrReplaceAll(cartItem)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({
                                    Toast.makeText(context,"Sepete Ba??ar??yla Eklendi",Toast.LENGTH_SHORT).show()
                                    // CounterFab'?? g??ncellemek i??in HomeActivity'e bir bildirim g??nderilecek
                                    EventBus.getDefault().postSticky(CountCartEvent(true))
                                },{
                                        t: Throwable? -> Toast.makeText(context,"[INSERT CART]"+t!!.message,Toast.LENGTH_SHORT).show()
                                }))
                        }
                        else
                            Toast.makeText(context,"[CART ERROR]"+e.message,Toast.LENGTH_SHORT).show()
                    }


                })
        }

    }

    private fun displayAllAddon() {
        if(Common.foodSelected!!.addon!!.size > 0){

            chip_group_addon!!.clearCheck()
            chip_group_addon!!.removeAllViews()

            edt_search_addon!!.addTextChangedListener(this)

            for(addonModel in Common.foodSelected!!.addon!!){

                    val chip = layoutInflater.inflate(R.layout.layout_chip,null,false) as Chip
                    chip.text = StringBuilder(addonModel.name!!).append("(+$").append(addonModel.price).append(")").toString()
                    chip.setOnCheckedChangeListener{compoundButton, b ->
                        if(b){
                            if(Common.foodSelected!!.userSelectedAddon == null)
                                Common.foodSelected!!.userSelectedAddon = ArrayList()
                            Common.foodSelected!!.userSelectedAddon!!.add(addonModel)
                        }
                    }

                    chip_group_addon!!.addView(chip)
            }
        }
    }

    private fun displayUserSelectedAddon() {
        if(Common.foodSelected!!.userSelectedAddon != null && Common.foodSelected!!.userSelectedAddon!!.size > 0){

            chip_group_user_selected_addon!!.removeAllViews()
            for(addonModel in Common.foodSelected!!.userSelectedAddon!!){

                val chip = layoutInflater.inflate(R.layout.layout_chip_with_delete,null,false) as Chip
                chip.text = StringBuilder(addonModel!!.name!!).append("(+$").append(addonModel.price).append(")").toString()
                chip.isClickable = false
                chip.setOnCloseIconClickListener{ view ->
                    chip_group_user_selected_addon!!.removeView(view)
                    Common.foodSelected!!.userSelectedAddon!!.remove(addonModel)
                    calculateTotalPrice()
                }
                chip_group_user_selected_addon!!.addView(chip)
            }
        } else
            chip_group_user_selected_addon!!.removeAllViews()
    }

    private fun showDialogRating() {

        var builder = AlertDialog.Builder(context!!)
        builder.setTitle("Yeme??i Puanla")
        builder.setMessage("L??tfen bilgileri doldurunuz")

        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_rating_comment,null)

        val ratingBar = itemView.findViewById<RatingBar>(R.id.rating_bar)
        val edt_comment = itemView.findViewById<EditText>(R.id.edt_comment)

        builder.setView(itemView)

        builder.setNegativeButton("??ptal"){ dialogInterface, i -> dialogInterface.dismiss() }

        builder.setPositiveButton("Tamam"){ dialogInterface, i ->
            val commentModel = CommentModel()
            commentModel.name = Common.currentUser!!.name
            commentModel.uid = Common.currentUser!!.uid
            commentModel.comment = edt_comment.text.toString()
            commentModel.ratingValue = ratingBar.rating
            val serverTimeStamp = HashMap<String,Any>()
            serverTimeStamp["timeStamp"] = ServerValue.TIMESTAMP
            commentModel.commentTimeStamp = (serverTimeStamp)

            foodDetailViewModel!!.setCommentModel(commentModel)
        }

        val dialog = builder.create()
        dialog.show()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
            chip_group_addon!!.clearCheck()
        chip_group_addon!!.removeAllViews()
        for(addonModel in Common.foodSelected!!.addon!!){

            if(addonModel.name!!.toLowerCase().contains(charSequence.toString().toLowerCase())){

                val chip = layoutInflater.inflate(R.layout.layout_chip,null,false) as Chip
                chip.text = StringBuilder(addonModel.name!!).append("(+$").append(addonModel.price).append(")").toString()
                chip.setOnCheckedChangeListener{compoundButton, b ->
                    if(b){
                        if(Common.foodSelected!!.userSelectedAddon == null)
                            Common.foodSelected!!.userSelectedAddon = ArrayList()
                        Common.foodSelected!!.userSelectedAddon!!.add(addonModel)
                    }
                }

                chip_group_addon!!.addView(chip)
            }
        }
    }

    override fun afterTextChanged(p0: Editable?) {

    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }
}