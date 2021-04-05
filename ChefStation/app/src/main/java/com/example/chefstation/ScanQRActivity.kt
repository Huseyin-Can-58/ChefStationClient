package com.example.chefstation

import android.Manifest
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.chefstation.Common.Common
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.android.synthetic.main.activity_scan_qr.*

class ScanQRActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_qr)

        Dexter.withContext(this@ScanQRActivity)
            .withPermission(Manifest.permission.CAMERA)
            .withListener(object: PermissionListener{
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {

                    zxscan.setResultHandler {

                        val intent = Intent()
                        intent.putExtra(Common.QR_CODE_TAG,it.text.toString())
                        setResult(Activity.RESULT_OK,intent)
                        finish()
                    }

                    zxscan.startCamera()
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {

                    Toast.makeText(this@ScanQRActivity,"Lütfen kameraya erişim verin",Toast.LENGTH_SHORT).show()
                    finish()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {

                }

            })

    }
}