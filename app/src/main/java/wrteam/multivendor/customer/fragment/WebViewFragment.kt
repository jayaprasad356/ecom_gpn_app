package com.gpn.customerapp.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.json.JSONException
import org.json.JSONObject
import com.gpn.customerapp.R
import com.gpn.customerapp.databinding.FragmentWebViewBinding
import com.gpn.customerapp.helper.ApiConfig
import com.gpn.customerapp.helper.Constant
import com.gpn.customerapp.helper.VolleyCallback

class WebViewFragment : Fragment() {
    lateinit var binding:FragmentWebViewBinding
    
    lateinit var type: String
    lateinit var root: View
    lateinit var activity: Activity

    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_web_view, container, false)
        binding = FragmentWebViewBinding.inflate(inflater,container,false)
        setHasOptionsMenu(true)
        activity = requireActivity()
        type = requireArguments().getString("type").toString()
        
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return if (url != null) {
                    view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    true
                } else {
                    false
                }
            }
        }
        try {
            when (type) {
                "Privacy Policy" -> getContent(Constant.GET_PRIVACY, "privacy")
                "Terms & Conditions" -> getContent(Constant.GET_TERMS, "terms")
                "Contact Us" -> getContent(Constant.GET_CONTACT, "contact")
                "About Us" -> getContent(Constant.GET_ABOUT_US, "about")
                else -> getInvoice(type)
            }
            activity.invalidateOptionsMenu()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return binding.root
    }

    private fun getInvoice(type: String?) {
        try {
            binding.webView.loadUrl(
                Constant.INVOICE_URL + "?id=" + type!!.split("#")
                    .toTypedArray()[1] + "&token=" + ApiConfig.createJWT(
                    "eKart",
                    "eKart Authentication"
                )
            )
            binding.btnPrint.visibility = View.VISIBLE
            binding.btnPrint.setOnClickListener {  createWebPagePrint(binding.webView) }
        } catch (e: Exception) {
            e.printStackTrace()
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun createWebPagePrint(webView: WebView?) {
        val printManager = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val printAdapter = webView!!.createPrintDocumentAdapter()
        val jobName = getString(R.string.order) + "_" + type.split("#").toTypedArray()[1]
        val builder = PrintAttributes.Builder()
        builder.setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        val printJob = printManager.print(jobName, printAdapter, builder.build())
        if (printJob.isCompleted) {
            Toast.makeText(activity, R.string.print_complete, Toast.LENGTH_SHORT).show()
        } else if (printJob.isFailed) {
            Toast.makeText(activity, R.string.print_failed, Toast.LENGTH_SHORT).show()
        }
        // Save the job object for later status checking
    }

    fun getContent(type: String, key: String?) {
        binding.progressBar.visibility = View.VISIBLE
        val params: MutableMap<String, String> = HashMap()
        params[Constant.SETTINGS] = Constant.GetVal
        params[type] = Constant.GetVal
        ApiConfig.requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {
                    if (result) {
                try {
                    val obj = JSONObject(response)
                    if (!obj.getBoolean(Constant.ERROR)) {
                        val privacyStr = obj.getString(key.toString())
                        binding.webView.isVerticalScrollBarEnabled = true
                        binding.webView.loadDataWithBaseURL("", privacyStr, "text/html", "UTF-8", "")
                        binding.progressBar.visibility = View.GONE
                    } else {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(activity, obj.getString(Constant.MESSAGE), Toast.LENGTH_LONG)
                            .show()
                    }
                    binding.progressBar.visibility = View.GONE
                } catch (e: JSONException) {
                    e.printStackTrace()
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
        }, activity, Constant.SETTING_URL, params, false)
    }

    override fun onResume() {
        super.onResume()
        Constant.TOOLBAR_TITLE = requireArguments().getString("type").toString()
        activity.invalidateOptionsMenu()
        hideKeyboard()
    }

    fun hideKeyboard() {
        try {
            val inputMethodManager =
                (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            inputMethodManager.hideSoftInputFromWindow(root.applicationWindowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.toolbar_cart).isVisible = false
        menu.findItem(R.id.toolbar_layout).isVisible = false
        menu.findItem(R.id.toolbar_search).isVisible = false
        menu.findItem(R.id.toolbar_search).isVisible = false
        super.onPrepareOptionsMenu(menu)
    }
}