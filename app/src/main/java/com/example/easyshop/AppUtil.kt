package com.example.easyshop

import android.content.Context
import android.widget.Toast

object AppUtil {
    fun showToast(context : Context, massage : String){
        Toast.makeText(context, massage, Toast.LENGTH_LONG).show()

    }
}