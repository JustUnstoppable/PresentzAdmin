package com.example.presentzadmin.fragments

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import com.example.presentzadmin.R
import com.example.presentzadmin.adapter.AddProductImageAdapter
import com.example.presentzadmin.databinding.FragmentAddProductBinding
import com.example.presentzadmin.model.AddProductModel
import com.example.presentzadmin.model.CategoryModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.util.*
import kotlin.collections.ArrayList


/**
 * A simple [Fragment] subclass.
 * Use the [AddProductFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AddProductFragment : Fragment() {
   private lateinit var binding:FragmentAddProductBinding
   private lateinit var list:ArrayList<Uri>
   private lateinit var listImages:ArrayList<String>
   private lateinit var adapter:AddProductImageAdapter
   private var coverImage:Uri?=null
   private lateinit var dialog: Dialog
   private var coverimageUrl:String?=""
   private lateinit var categoryList:ArrayList<String>
    private var launchGalleryActivity=registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){
        if(it.resultCode== Activity.RESULT_OK){
            coverImage=it.data!!.data
            binding.productCoverImage.setImageURI(coverImage)
        }
    }
    private var launchProductActivity=registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){
        if(it.resultCode== Activity.RESULT_OK){
            val imageUrl=it.data!!.data
            list.add(imageUrl!!)
            adapter.notifyDataSetChanged()
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding=FragmentAddProductBinding.inflate(layoutInflater)
        list= ArrayList()
        listImages= ArrayList()

        dialog= Dialog(requireContext())
        dialog.setContentView(R.layout.progress_dialog)
        dialog.setCancelable(false)
        binding.selectCoverImage.setOnClickListener {
            binding.productCoverImage.isVisible=true
            val intent= Intent("android.intent.action.GET_CONTENT")
            //pick image from gallery
            intent.type="image/*"
            launchGalleryActivity.launch(intent)
        }
        binding.ProductImageBtn.setOnClickListener {

            val intent= Intent("android.intent.action.GET_CONTENT")
            //pick image from gallery
            intent.type="image/*"
            launchProductActivity.launch(intent)
        }
        setProductCategory()
        adapter= AddProductImageAdapter(list)
        binding.ProductImgRecyclerView.adapter=adapter
        binding.submitProductBtn.setOnClickListener {
            validateData()
        }
        return binding.root
    }

    private fun validateData() {
        if(binding.productNameET.text.toString().isEmpty()){
            binding.productNameET.requestFocus()
            binding.productNameET.error="Empty"
        }else if(binding.productSPET.text.toString().isEmpty()){
            binding.productSPET.requestFocus()
            binding.productSPET.error="Empty"
        }else if(binding.productMRPET.text.toString().isEmpty()){
            binding.productMRPET.requestFocus()
            binding.productMRPET.error="Empty"
        }else if(coverImage==null){
            Toast.makeText(requireContext(), "Please select cover image", Toast.LENGTH_SHORT).show()
        }else if(list.size <1){
            Toast.makeText(requireContext(), "Please select product image", Toast.LENGTH_SHORT).show()
        }else{
            uploadImage()
        }
    }

    private fun uploadImage() {
        dialog.show()
        val fileName= UUID.randomUUID().toString()+".jpg"
        val refStorage= FirebaseStorage.getInstance().reference.child("products/$fileName")
        refStorage.putFile(coverImage!!)
            .addOnSuccessListener {
                it.storage.downloadUrl.addOnSuccessListener { image->
                    coverimageUrl=image.toString()
                    uploadProductImage()
                }

            }.addOnFailureListener {
                dialog.dismiss()
                Toast.makeText(requireContext(), "Something went wrong with storage!!", Toast.LENGTH_SHORT).show()
            }
    }
    private var i=0
    private fun uploadProductImage() {
        dialog.show()
        val fileName=UUID.randomUUID().toString()+".jpg"
        val refStorage=FirebaseStorage.getInstance().reference.child("products/$fileName")
        refStorage.putFile(list[i]!!)
            .addOnSuccessListener {
                it.storage.downloadUrl.addOnSuccessListener { image->
                    listImages.add(image!!.toString())
                    if(list.size==listImages.size){
                        storeData()
                    }else{
                        i+=1
                        uploadProductImage()
                    }

                }

            }.addOnFailureListener {
                dialog.dismiss()
                Toast.makeText(requireContext(), "Something went wrong with storage!!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun storeData() {
        val db=Firebase.firestore.collection("products")
        val key=db.document().id
        val data=AddProductModel(
            binding.productNameET.text.toString(),
            binding.productDescriptionET.text.toString(),
            coverimageUrl.toString(),
            categoryList[binding.productCategoryDropdown.selectedItemPosition],
            key,
            binding.productMRPET.text.toString(),
            binding.productSPET.text.toString(),
            listImages
        )
        db.document(key).set(data).addOnSuccessListener {
            dialog.dismiss()
            Toast.makeText(requireContext(), "Product Added", Toast.LENGTH_SHORT).show()
            binding.productNameET.text=null
        }.addOnFailureListener {
            dialog.dismiss()
            Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT).show()

        }
    }

    private fun setProductCategory(){
        categoryList= ArrayList()
        Firebase.firestore.collection("categories").get().addOnSuccessListener {
            categoryList.clear()
            for(doc in it.documents){
                val data=doc.toObject(CategoryModel::class.java)
                categoryList.add(data!!.cat!!)
            }
            categoryList.add(0,"Select Category ")
            val arrayAdapter=ArrayAdapter(requireContext(),R.layout.dropdown_item_layout,categoryList)
            binding.productCategoryDropdown.adapter=arrayAdapter
        }
    }

}