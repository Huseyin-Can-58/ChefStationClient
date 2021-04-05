package com.example.chefstation

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.TextUtils
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import android.app.AlertDialog
import android.view.AbsSavedState
import android.widget.TextView
import androidx.appcompat.widget.AlertDialogLayout
import com.example.chefstation.Common.Common
import com.example.chefstation.Common.Common.currentUser
import com.example.chefstation.EventBus.MenuItemBack
import com.example.chefstation.Model.UserModel
//import com.example.chefstation.Remote.ICloudFunctions
//import com.example.chefstation.Remote.RetrofitCloudClient
import io.*
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import dmax.dialog.SpotsDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

class MainActivity : AppCompatActivity() {

   /* private var placeSelected:Place?=null
    private var places_fragment:AutocompleteSupportFragment?=null
    private lateinit var placeClient:PlacesClient
    private val placeFields = Arrays.asList(Place.Field.ID,
    Place.Field.NAME,
    Place.Field.ADDRESS,
    Place.Field.LAT_LNG)*/

    private lateinit var firebaseAuth:FirebaseAuth
    private lateinit var listener:FirebaseAuth.AuthStateListener
    private lateinit var dialog: AlertDialog
    private val compositeDisposable = CompositeDisposable()
    //private lateinit var cloudFunctions: ICloudFunctions

    private lateinit var userRef: DatabaseReference
    private var providers:List<AuthUI.IdpConfig>? = null
    companion object{

        private val APP_REQUEST_CODE =7171
    }

    override fun onStart() {
        super.onStart()
        firebaseAuth.addAuthStateListener(listener)
    }

    override fun onStop() {
        if(listener != null)
            firebaseAuth.removeAuthStateListener(listener)
        compositeDisposable.clear()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        init()
    }

    private fun init(){

       /* Places.initialize(this,getString(R.string.google_maps_key))
        placeClient = Places.createClient(this)*/

        providers = Arrays.asList<AuthUI.IdpConfig>(AuthUI.IdpConfig.PhoneBuilder().build(),AuthUI.IdpConfig.EmailBuilder().build())
        userRef = FirebaseDatabase.getInstance().getReference(Common.USER_REFERENCE)
        firebaseAuth=FirebaseAuth.getInstance()
        dialog = SpotsDialog.Builder().setContext(this).setCancelable(false).build()
        // cloudFunctions = RetrofitCloudClient.getInstance().create(ICloudFunctions::class.java)
        listener = FirebaseAuth.AuthStateListener { firebaseAuth->

            Dexter.withActivity(this@MainActivity)
                .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object:PermissionListener{
                    override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                        val user = firebaseAuth.currentUser

                        if (user != null){

                            checkUserFromFirebase(user!!)

                        } else {

                            phoneLogin()
                        }
                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse?) {

                        Toast.makeText(this@MainActivity,"Uygulamayı kullanmak için bu izni kabul etmelisiniz",Toast.LENGTH_SHORT).show()
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permission: PermissionRequest?,
                        token: PermissionToken?
                    ) {

                    }

                }).check()

        }
    }

    private fun checkUserFromFirebase(user: FirebaseUser) {
        dialog!!.show()
        userRef!!.child(user!!.uid)
            .addListenerForSingleValueEvent(object:ValueEventListener {

                override fun onCancelled(p0: DatabaseError){

                    Toast.makeText(this@MainActivity,""+p0.message,Toast.LENGTH_SHORT).show()
                }

                override fun onDataChange(p0: DataSnapshot){

                    if (p0.exists()){

                        val userModel = p0.getValue(UserModel::class.java)

                        goToHomeActivity(userModel)
                    }
                    else{

                        showRegisterDialog(user!!)
                    }

                    dialog!!.dismiss()
                }

            })
    }

    private fun showRegisterDialog(user:FirebaseUser) {

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)

        builder.setTitle("KAYIT")
        builder.setMessage("Lütfen gerekli bilgileri doldurunuz")

        val itemView =
            LayoutInflater.from(this@MainActivity)
                    .inflate(R.layout.layout_register, null)

        val phone_input_layout = itemView.findViewById<TextInputLayout>(R.id.phone_input_layout)
        val edt_name = itemView.findViewById<EditText>(R.id.edt_name)
        val edt_phone = itemView.findViewById<EditText>(R.id.edt_phone)
        val edt_address = itemView.findViewById<EditText>(R.id.edt_address)

       /* places_fragment = supportFragmentManager.findFragmentById(R.id.places_autocomplete_fragment) as AutocompleteSupportFragment
        places_fragment!!.setPlaceFields(placeFields)
        places_fragment!!.setOnPlaceSelectedListener(object: PlaceSelectionListener{
            override fun onPlaceSelected(p0: Place) {
                placeSelected = p0
                txt_address.text = placeSelected!!.address
            }

            override fun onError(p0: Status) {
                Toast.makeText(this@MainActivity,""+p0.statusMessage,Toast.LENGTH_SHORT).show()
            }

        })*/
        if(user.phoneNumber == null || TextUtils.isEmpty(user.phoneNumber)){

            phone_input_layout.hint = "Email"
            edt_phone.setText(user.email)
            edt_name.setText(user.displayName)
        }
        else
            edt_phone.setText(user!!.phoneNumber)

        builder.setView(itemView)
        builder.setNegativeButton("IPTAL") { dialogInterface, i -> dialogInterface.dismiss() }
        builder.setPositiveButton("KAYIT") { dialogInterface, i ->

                if (TextUtils.isDigitsOnly(edt_name.text.toString())) {
                    Toast.makeText(this@MainActivity, "Lütfen İsminizi Giriniz", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                else if (TextUtils.isDigitsOnly(edt_address.text.toString())){

                    Toast.makeText(this@MainActivity, "Lütfen Adresinizi Giriniz", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val userModel = UserModel()
                userModel.uid = user!!.uid
                userModel.name = edt_name.text.toString()
                userModel.address = edt_address.text.toString()
                userModel.phone = edt_phone.text.toString()
              /*  userModel.lat = placeSelected!!.latLng!!.longitude
                userModel.lng = placeSelected!!.latLng!!.longitude*/

                userRef!!.child(user!!.uid)
                        .setValue(userModel)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                dialogInterface.dismiss()

                                Toast.makeText(this@MainActivity, "" + "Tebrikler! Kayıt Başarılı", Toast.LENGTH_SHORT).show()

                                goToHomeActivity(userModel)
                            }
                        }

            if (TextUtils.isDigitsOnly(edt_phone.text.toString())) {

                Toast.makeText(this@MainActivity, "" + "Lütfen Telefon Numaranızı Giriniz", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
        }

        val dialog = builder.create()
       /* dialog.setOnDismissListener{
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.remove(places_fragment!!)
            fragmentTransaction.commit()
        }*/
        dialog.show()

    }

    private fun goToHomeActivity(userModel: UserModel?) {

                FirebaseInstanceId.getInstance()
                    .instanceId
                    .addOnFailureListener { e -> Toast.makeText(this@MainActivity,""+e.message,Toast.LENGTH_SHORT).show()

                        Common.currentUser = userModel!!
                        startActivity(Intent(this@MainActivity,HomeActivity::class.java))
                        finish()
                    }
                    .addOnCompleteListener { task ->
                        if(task.isSuccessful){

                            Common.currentUser = userModel!!
                            // Bu fonksiyon geçerli kullanıcı oluşturulduktan sonra çağırılmalı
                            Common.updateToken(this@MainActivity,task.result!!.token)
                            startActivity(Intent(this@MainActivity,HomeActivity::class.java))
                            finish()
                        }
                    }
        }

    private fun phoneLogin() {

        startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder()
                .setTheme(R.style.LoginTheme)
                .setLogo(R.drawable.logo)
                .setAvailableProviders(providers!!).build(), APP_REQUEST_CODE)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == APP_REQUEST_CODE)
        {

            val response = IdpResponse.fromResultIntent(data)
            if(resultCode == Activity.RESULT_OK){

                val user = FirebaseAuth.getInstance().currentUser
            } else {

                Toast.makeText(this,"Giriş Başarısız Oldu",Toast.LENGTH_SHORT).show()
            }
        }
    }

}
