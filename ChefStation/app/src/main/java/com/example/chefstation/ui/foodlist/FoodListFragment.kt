package com.example.chefstation.ui.foodlist

import android.app.AppComponentFactory
import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chefstation.Adapter.MyFoodListAdapter
import com.example.chefstation.Common.Common
import com.example.chefstation.EventBus.MenuItemBack
import com.example.chefstation.Model.CategoryModel
import com.example.chefstation.Model.FoodModel
import com.example.chefstation.R
import org.greenrobot.eventbus.EventBus

class FoodListFragment : Fragment() {

    private lateinit var foodListViewModel: FoodListViewModel

    var recycler_food_list : RecyclerView?=null
    var layoutAnimationController:LayoutAnimationController?=null

    var adapter : MyFoodListAdapter?=null

    override fun onStop() {
        if(adapter != null)
            adapter!!.onStop()
        super.onStop()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        foodListViewModel =
            ViewModelProviders.of(this).get(FoodListViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_food_list,container,false)
        initViews(root)
        foodListViewModel.getMutableFoodModelListData().observe(this, Observer{

           if(it != null){  // Kategoride yemek yokken hata vermemek için kontrol

               adapter = MyFoodListAdapter(context!!,it)
               recycler_food_list!!.adapter = adapter
               recycler_food_list!!.layoutAnimation = layoutAnimationController
           }
        })
        return root
    }

    private fun initViews(root: View?) {

        setHasOptionsMenu(true)

        (activity as AppCompatActivity).supportActionBar!!.setTitle(Common.categorySelected!!.name)

        recycler_food_list = root!!.findViewById(R.id.recycler_food_list) as RecyclerView
        recycler_food_list!!.setHasFixedSize(true)
        recycler_food_list!!.layoutManager = LinearLayoutManager(context)

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)

        (activity as AppCompatActivity).supportActionBar!!.title = Common.categorySelected!!.name
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_menu,menu)

        val menuItem = menu.findItem(R.id.action_search)

        val searchManager = activity!!.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menuItem.actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(activity!!.componentName))

        //Event

        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(s: String?): Boolean {
                startSearch(s!!)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }


        })

        // Metni Temizleme

        val closeButton = searchView.findViewById<View>(R.id.search_close_btn) as ImageView
        closeButton.setOnClickListener{

            val txt = searchView.findViewById<View>(R.id.search_src_text) as EditText
            //Metni Temizleme
            txt.setText("")
            //Kuyruğu temizleme
            searchView.setQuery("",false)
            // Görünümü sıfırlamak
            searchView.onActionViewCollapsed()
            // Arama widget'ını sıfırlama
            menuItem.collapseActionView()
            // Sonuçları orijinal haline getirme
            foodListViewModel.getMutableFoodModelListData()
        }
    }

    private fun startSearch(s: String) {
        val resultFood = ArrayList<FoodModel>()
        for(i in 0 until Common.categorySelected!!.foods!!.size){

            val categoryModel = Common.categorySelected!!.foods!![i]
            if(categoryModel.name!!.toLowerCase().contains(s))
                resultFood.add(categoryModel)
        }

        foodListViewModel.getMutableFoodModelListData().value = resultFood
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }
}