package com.example.easyshop.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyshop.AppUtil
import com.example.easyshop.components.ProductItemView
import com.example.easyshop.model.ProductModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@Composable
fun FavoritePage(modifier: Modifier = Modifier){

    val productsList = remember {
        mutableStateOf<List<ProductModel>>(emptyList())

    }

    val context = LocalContext.current

    LaunchedEffect(key1 = Unit) {

        val favoriteList = AppUtil.getFavoriteList(context)
        if (favoriteList.isEmpty()){
            productsList.value = emptyList()
        }else{
            Firebase.firestore.collection("data").document("stock")
                .collection("products")
                .whereIn("id", favoriteList.toList())
                .get().addOnCompleteListener {
                    if (it.isSuccessful) {
                        val resultList = it.result.documents.mapNotNull { doc ->
                            doc.toObject(ProductModel::class.java)
                        }
                        productsList.value = resultList
                    }
                }
        }

    }
    Column (
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    )
    {
        Text(text = "Your favorites", style = TextStyle(
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (productsList.value.isNotEmpty()){
            LazyColumn (
                modifier = Modifier.fillMaxSize()
            ) {
                items(productsList.value.chunked(2)){rowItems ->
                    Row {
                        rowItems.forEach {
                            ProductItemView(product = it,
                                modifier = Modifier.weight(1f))
                        }
                        if (rowItems.size == 1){
                            Spacer(modifier = Modifier.weight(1f))
                        }

                    }
                }
            }
        }else{
            Column (
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally

            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color(0xFFE0E0E0)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No favorite items yet",
                    fontSize = 18.sp,
                    color = Color.Gray
                )
            }
        }


    }


}