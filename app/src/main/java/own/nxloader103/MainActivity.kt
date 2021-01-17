package own.nxloader103

/*
*
I made this app because I fucking hate closed sourced shit in other open sourced apps.
* FUCK CLOSED SOURCED APPS!!!!!!!   ESP TO SXOS AND OTHERS
*
* I HAD ENOUGH TROUBLE AND I AM FED UP WITH CLOSED SOURCED APPS
*
* ELY M.
*
* */



import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.github.angads25.filepicker.controller.DialogSelectionListener
import com.github.angads25.filepicker.model.DialogConfigs
import com.github.angads25.filepicker.model.DialogProperties
import com.github.angads25.filepicker.view.FilePickerDialog
import com.intentfilter.androidpermissions.PermissionManager
import com.intentfilter.androidpermissions.PermissionManager.PermissionRequestListener
import com.intentfilter.androidpermissions.models.DeniedPermissions
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.buffer
import okio.sink
import org.jetbrains.anko.doAsync
import java.io.File
import java.util.Collections.singleton
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask


class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    private val uiDispatcher: CoroutineDispatcher = Dispatchers.Main
    private val PayLoadsFolder = Environment.getExternalStorageDirectory().getAbsolutePath() + "/NXLoader103/"
    private val APX_VID = 0x0955
    private val APX_PID = 0x7321
    private var ACTION_USB_PERMISSION ="own.nxloader103.USB_PERMISSION"

    //payloads downloads
    val HEKATE_NAME = "hekate_ctcaer.bin"
    val AMS_NAME = "fuse-primary.bin"
    val TEGRAEXPLORER_NAME = "TegraExplorer.bin"
    val LOCKPICK_NAME = "Lockpick_RCM.bin"
    val HEKATE_PAYLOAD = "https://github.com/ELY3M/NXLoader103/releases/latest/download/hekate_ctcaer.bin"
    val AMS_PAYLOAD = "https://github.com/Atmosphere-NX/Atmosphere/releases/latest/download/fusee-primary.bin"
    val TEGRAEXPLORER_PAYLOAD = "https://github.com/suchmememanyskill/TegraExplorer/releases/latest/download/TegraExplorer.bin"
    val LOCKPICK_PAYLOAD = "https://github.com/shchmue/Lockpick_RCM/releases/latest/download/Lockpick_RCM.bin"

//I know need to unzip hekate bin from a zip programmically  :/
//I would like Ctcaer to upload hekate bin to git releases page if possible.

//best solution is upload latest hekate_ctcaer payload bin to my github releases page......
// have to go it asap once hekate come out with new release.



    private  var dialog:FilePickerDialog?=null
    private var usbreceiver:BroadcastReceiver?=null
    private var autointent:Intent?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)




        setContentView(R.layout.activity_main)


        //storage permission//
        val permissionManager = PermissionManager.getInstance(applicationContext)
        permissionManager.checkPermissions(
            singleton(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            object : PermissionRequestListener {
                override fun onPermissionGranted() {

                    //make sure we have folder for payloads
                    val dir = File(PayLoadsFolder)
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }


                    var sharepreferences = getSharedPreferences("Config", Context.MODE_PRIVATE)
                    val payload_name = sharepreferences.getString("binpath", null)
                    if (payload_name != null) {
                        filepath.text = payload_name
                    }
                    setSlideMenu()
                    setItems()
                    RegisterReceiver()

                    ListPayloads()


                }

                override fun onPermissionDenied(deniedPermissions: DeniedPermissions) {
                    Toast.makeText(applicationContext, R.string.permsdenied, Toast.LENGTH_SHORT)
                        .show()
                }
            })



        ///download payload button///
        val mainDownloadButton = findViewById<Button>(R.id.download)
        mainDownloadButton.setOnClickListener {
            showDialog()


        }



        ///Update button///
        val mainUpdateButton = findViewById<Button>(R.id.updatepayloads)
        mainUpdateButton.setOnClickListener {
        Toast.makeText(this, "Updating the built-in payloads... It will take few mins.", Toast.LENGTH_LONG).show()
            downloadme(HEKATE_NAME, HEKATE_PAYLOAD)
            downloadme(AMS_NAME, AMS_PAYLOAD)
            downloadme(TEGRAEXPLORER_NAME, TEGRAEXPLORER_PAYLOAD)
            downloadme(LOCKPICK_NAME, LOCKPICK_PAYLOAD)

            //wait 13 secs and update payload list....
            CoroutineScope(Dispatchers.IO).launch {
                delay(TimeUnit.SECONDS.toMillis(13))
                withContext(Dispatchers.Main) {
                ListPayloads()
                }
            }

        }




    }



private fun ListPayloads() {

    val requiredSuffixes = listOf("bin")
    fun hasRequiredSuffix(file: File): Boolean {
        return requiredSuffixes.contains(file.extension)
    }

    val m_ll = findViewById<View>(R.id.llisting) as LinearLayout
    m_ll.removeAllViews();

    File(PayLoadsFolder).walkTopDown().filter { file ->
        file.isFile && hasRequiredSuffix(
            file
        )
    }.forEach {
        //println(it)
        //it.toString() full path
        //it.name only filenames
        //stringBuilder.append(it.name + "\n\n")
        //listing.text = stringBuilder.toString()

        var num: Int = 0
        var binpath: String = ""
        val dir = File(PayLoadsFolder)
        val files = dir.listFiles()
        if (files != null) {
            num = files.size
            val m_ll = findViewById<View>(R.id.llisting) as LinearLayout
            val text = TextView(applicationContext)
            text.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text.setTextAlignment(View.TEXT_ALIGNMENT_CENTER)
            text.setBackgroundColor(Color.BLACK)
            text.setTextColor(Color.WHITE)
            text.text = "\n\n" + it.name
            binpath = it.toString()

            text.setOnClickListener()
            {

                filepath.setText(binpath)
                var sharepreferences = getSharedPreferences("Config", Context.MODE_PRIVATE)
                var shareditor = sharepreferences.edit().putString("binpath", binpath)
                shareditor.apply()
                Toast.makeText(this@MainActivity, "payload updated to: " + binpath, Toast.LENGTH_SHORT).show()
            }

            m_ll.addView(text)


        }

    }

}


//download dialog
private fun showDialog() {
    val dialog = Dialog(this)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.setCancelable(false)
    dialog.setContentView(R.layout.dialog_payload_download)

    val payloadname = dialog.findViewById<EditText>(R.id.PayloadName)
    val downloadurl = dialog.findViewById<EditText>(R.id.PayloadURL)
    val downloadbutton = dialog.findViewById<Button>(R.id.PayloadDownload)
    val closebutton = dialog.findViewById<Button>(R.id.close)

    payloadname.setText(AMS_NAME)
    downloadurl.setText(AMS_PAYLOAD)



        downloadbutton.setOnClickListener {
        val getpayloadname = payloadname.text.toString()
        val getdownloadurl = downloadurl.text.toString()

        if (getpayloadname == null || getpayloadname.trim()==""){
            Toast.makeText(this, "please input payload name, payload name cannot be blank", Toast.LENGTH_SHORT).show()
        } else {
            payloadname.setText(getpayloadname).toString()
        }


        if (getdownloadurl == null || getpayloadname.trim()==""){
            Toast.makeText(this, "please input url to the payload bin", Toast.LENGTH_SHORT).show()
        } else {
                payloadname.setText(getpayloadname).toString()
        }

            if (getpayloadname != null && getdownloadurl != null) {

                downloadme(getpayloadname, getdownloadurl)
                ListPayloads()
                Toast.makeText(this@MainActivity, "Downloaded payload: " + getpayloadname + " url: " + getdownloadurl, Toast.LENGTH_SHORT).show()

            } else {

                Toast.makeText(this, "the text boxes are empty  :(", Toast.LENGTH_SHORT).show()

            }

    }


    closebutton.setOnClickListener {
        ListPayloads() //update the payloads list
        dialog.dismiss()
    }
    dialog.show()

}



    fun downloadme(name: String, url: String) = GlobalScope.launch(uiDispatcher) {
        withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            Log.d("nxloader103", "downloadme ran")
            try {
                val request = Request.Builder()
                    .url(url)
                    .build()

                val response = client.newCall(request).execute()

                val Directory = File(PayLoadsFolder)
                if (!Directory.exists()) {
                    Directory.mkdirs()
                }

                val sink = File(Directory, name).sink().buffer()
                sink.writeAll(response.body!!.source())
                sink.close()
                response.body!!.close()
            } catch (e: Exception) {
                Log.d("nxloader103", "exception: " + e.printStackTrace())
                e.printStackTrace()
            }
        }

    }







    override fun onResume() {
        super.onResume()
        UsbPermissionCheck()

    }

    fun RegisterReceiver()
    {
        if(usbreceiver==null) {
            usbreceiver = usbBroadcastreceiver()
            var filter = IntentFilter()
            filter.addAction(ACTION_USB_PERMISSION)
            filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            registerReceiver(usbreceiver, filter)
        }
        else{
            unregisterReceiver(usbreceiver)
        }
    }
    override fun onDestroy() {
        if (usbreceiver != null) {
            unregisterReceiver(usbreceiver)
            super.onDestroy()
        }
    }
    fun UsbPermissionCheck():Boolean
    {
        var connection=false
        var manager =  getSystemService(Context.USB_SERVICE) as UsbManager

        var deviceList = manager.getDeviceList();
        var  deviceIterator = deviceList.values.iterator();
        var mPermissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_USB_PERMISSION),
            0
        )
        for(i in manager.deviceList)
        {
            var device=i.value
            if(manager.hasPermission(device))
            {
                connection=true
                switchstatus.text=getString(R.string.deviceconnection)
                switchstatus.setTextColor(resources.getColor(R.color.light_green))
            }
            else {
                connection=false
                switchstatus.text=getString(R.string.devicenotconnection)
                switchstatus.setTextColor(resources.getColor(R.color.red))
                manager.requestPermission(device, mPermissionIntent)
            }

        }
        return connection
    }

//    File permission check
    override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode)
        {
            FilePickerDialog.EXTERNAL_READ_PERMISSION_GRANT -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (dialog != null) {
                        //Show dialog if the read permission has been granted.
                        dialog!!.show();
                    }
                } else {
                    //Permission has not been granted. Notify the user.
                    dialog!!.dismiss()
                }
            }
        }
    }

//    USB broadcasterreceiver
    fun usbBroadcastreceiver():BroadcastReceiver{
        var usbreceiver=object:BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                var action=intent!!.action
                when(action)
                {
                    "own.nxloader103.USB_PERMISSION" -> {
                        synchronized(this) {
                            val device: UsbDevice? =
                                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                            if (intent.getBooleanExtra(
                                    UsbManager.EXTRA_PERMISSION_GRANTED,
                                    false
                                )
                            ) {
                                device?.apply {
                                    //call method to set up device communication

                                }
                            } else {

                            }
                        }
                    }
                    UsbManager.ACTION_USB_DEVICE_ATTACHED ->
                        if (UsbPermissionCheck()) injectauto()

                    UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                        switchstatus.text = getString(R.string.devicenotconnection)
                        switchstatus.setTextColor(resources.getColor(R.color.red))
                    }
                }
            }
        }
        return usbreceiver
    }

//    set the slide menu
    fun setSlideMenu()
    {
        var menuarray=resources.getStringArray(R.array.menu)
        val item1 = SecondaryDrawerItem().withIdentifier(1).withName(menuarray.get(1)).withSelectable(
            false
        )
//        bartitle.text=menuarray.get(0)
        bartitle.text=getString(R.string.app_name)+" "+packageManager.getPackageInfo(packageName, 0).versionName
        var leftmenu= DrawerBuilder()
                .withActivity(this@MainActivity)
                .addDrawerItems(
                    item1
                )
                .withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener {
                    override fun onItemClick(
                        view: View?,
                        position: Int,
                        drawerItem: IDrawerItem<*, *>?
                    ): Boolean {
                        when (drawerItem!!.identifier.toInt()) {
                            1 -> {
                                startActivity(Intent(this@MainActivity, AboutActivity::class.java))
                            }

                        }

                        return false
                    }

                })
                .build()
        slidemenu.setOnClickListener {
            leftmenu.openDrawer()
        }
    }

    fun setFile()
    {

        filebtn.setOnClickListener {
            if(SDCardsUtils.getSDCard1(this)!=null){
                var sddialog=AlertDialog.Builder(this)
                sddialog.setTitle(R.string.sdchoose)
                val items = arrayOf(getString(R.string.insd), getString(R.string.outsd))

                sddialog.setItems(items, object : DialogInterface.OnClickListener {
                    override fun onClick(dialog1: DialogInterface?, which: Int) {
                        when (which) {
                            0 -> {
                                var folder = SDCardsUtils.getSDCard0(this@MainActivity)
                                filedialog(folder)
                                dialog!!.show()
                            }

                            1 -> {
                                var folder = SDCardsUtils.getSDCard1(this@MainActivity)
                                filedialog(folder)
                                dialog!!.show()

                            }
                        }
                    }

                }).show()
            }
            else {
                var folder = SDCardsUtils.getSDCard0(this@MainActivity)
                filedialog(folder)
                dialog!!.show()
            }
        }
    }

    fun filedialog(folder: String){
        var properties = DialogProperties()
        properties.selection_mode = DialogConfigs.SINGLE_MODE
        properties.selection_type = DialogConfigs.FILE_SELECT
        properties.root = File(folder)
        properties.error_dir = File(DialogConfigs.DEFAULT_DIR)
        properties.offset = File(DialogConfigs.DEFAULT_DIR)
        var exten=ArrayList<String>()
        exten.add("bin")
        properties.extensions=exten.toTypedArray()
        dialog = FilePickerDialog(this@MainActivity, properties)
        dialog!!.setTitle(getString(R.string.fileselmsg))
        dialog!!.setPositiveBtnName(getString(R.string.filesepo))
        dialog!!.setNegativeBtnName(getString(R.string.filesena))

        dialog!!.setDialogSelectionListener(object : DialogSelectionListener {
            override fun onSelectedFilePaths(files: Array<out String>?) {
                filepath.text = files!!.get(0)
                var sharepreferences = getSharedPreferences("Config", Context.MODE_PRIVATE)
                var shareditor = sharepreferences.edit().putString("binpath", files!!.get(0))
                shareditor.apply()
            }
        })
    }

//    Init the elements and functions
    fun setItems()
    {
        setFile()
        var sharepreferences=getSharedPreferences("Config", Context.MODE_PRIVATE)

        opengithub.setOnClickListener {
            var uri = Uri.parse("https://github.com/ELY3M/NXloader103")
            val intent = Intent()
            intent.action = "android.intent.action.VIEW"
            intent.data = uri
            startActivity(intent)
        }


        injectauto()
        injection.setOnClickListener {
            if(UsbPermissionCheck()) {
                runOnUiThread {

                    injectionloading.visibility = View.VISIBLE
                    injectionloading.bringToFront()
                    injectBin()
                }
            }
            else
                    Toastmessage(getString(R.string.devicenotconnection))

        }


        autoinjection.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                if (isChecked) {
                    sharepreferences.edit().putBoolean("autoinject", true).apply()

//                    autointent=Intent(this@MainActivity,AutoInjectService::class.java)
//                    startService(autointent)
                } else {
                    sharepreferences.edit().putBoolean("autoinject", false).apply()

//                    if(autointent!=null)
//                        stopService(autointent)
                }
            }

        })
    }

    fun injectauto()
    {
        var sharepreferences=getSharedPreferences("Config", Context.MODE_PRIVATE)

        var autoinject=sharepreferences.getBoolean("autoinject", false)
        if(autoinject)
        {
            autoinjection.isChecked=true
            var binpath = sharepreferences.getString("binpath", null)
            if(binpath!=null)
                injectBin()
            else
                Toastmessage(getString(R.string.filepathsrc))
        }
        else autoinjection.isChecked=false
    }
    fun injectBin()
    {
        var usbManager=getSystemService(Context.USB_SERVICE) as UsbManager
        var devicelist=usbManager.deviceList

        for(a in devicelist)
        {
            if(a.value.productId==APX_PID&&a.value.vendorId==APX_VID) {
//                    Thread(Runnable {
                var u=PrimaryLoader()
                if(u!=null) {
                    var sharepreferences=getSharedPreferences("Config", Context.MODE_PRIVATE)
                    u.handleDevice(this, a.value, object : PrimaryLoader.Injectionprogress {
                        override fun onFailed(errormsg: String) {
                            doAsync {
                                injectionloading.visibility = View.GONE
                                Toastmessage(errormsg)
                            }
                        }


                        override fun onCompleted() {
                            doAsync {
                                injectionloading.visibility = View.GONE
                                Toastmessage(getString(R.string.injectsuccess))
                            }
                        }
                    }
                    )
                }
//                    }).start()

            }
        }
    }
    fun Toastmessage(msg: String)
    {
        object : Thread() {
            override fun run() {
                Looper.prepare()
                //                CumToast.show(context,messagestr);
                if (msg != "")
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show()
                Looper.loop()
            }

        }.start()
    }
}
