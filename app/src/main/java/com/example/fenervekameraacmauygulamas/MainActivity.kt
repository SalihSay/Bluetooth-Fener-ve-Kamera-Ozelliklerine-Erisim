package com.example.fenervekameraacmauygulamas

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var flasAcıkMı = false
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var cihazBulundu = false

    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_PERMISSIONS = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Kamera yöneticisi ve kamera ID'sini al
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList[0]

        // Bluetooth adaptörünü bağla
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Butonları tanımla
        val flasButton: Button = findViewById(R.id.buttonFlas)
        val cameraButton: Button = findViewById(R.id.buttonKamera)
        val bluetoothButton: Button = findViewById(R.id.buttonBluetooth)

        // Feneri aç/kapa butonuna bastığımda yapması gereken fonksiyon
        flasButton.setOnClickListener {
            flasıAc()
        }

        // Kamerayı aç butonuna bastığımda yapması gereken fonksiyon
        cameraButton.setOnClickListener {
            kameraAc()
        }

        // Bluetooth açma/kapatma butonuna bastığımda yapması gereken fonksiyon
        bluetoothButton.setOnClickListener {
            bluetoothuAc()
        }

        // İlk başta fener durumunu güncelle
        flasButonunuGuncelle(flasButton)
    }

    // Feneri aç/kapat işlemi
    private fun flasıAc() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 1)
        } else {
            try {
                flasAcıkMı = !flasAcıkMı
                cameraManager.setTorchMode(cameraId, flasAcıkMı)
                val flashlightButton: Button = findViewById(R.id.buttonFlas)
                flasButonunuGuncelle(flashlightButton)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }
    }

    // Fenerin durumuna göre fener butonunun metnini güncelle
    private fun flasButonunuGuncelle(button: Button) {
        button.text = if (flasAcıkMı) "Feneri Kapat" else "Feneri Aç"
    }

    // Kamerayı açma işlemi
    private fun kameraAc() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Kamera uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
        }
    }

    // Bluetooth iznini kontrol et ve gerekli izinleri iste
    private fun bluetoothİzinleriniKontrolEt(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val bluetoothScanPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN)
            val bluetoothConnectPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT)

            if (bluetoothScanPermission != PackageManager.PERMISSION_GRANTED || bluetoothConnectPermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT), REQUEST_PERMISSIONS)
                false
            } else {
                true
            }
        } else {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Bluetooth'u açma/kapama işlemi
    private fun bluetoothuAc() {
        if (bluetoothİzinleriniKontrolEt()) {
            try {
                val bluetoothButton: Button = findViewById(R.id.buttonBluetooth)
                if (bluetoothAdapter == null) {
                    Toast.makeText(this, "Bluetooth desteklenmiyor", Toast.LENGTH_SHORT).show()
                    return
                }

                if (!bluetoothAdapter.isEnabled) {
                    // Bluetooth'u aç
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                    bluetoothButton.text = "Bluetooth Kapat"
                } else {
                    // Bluetooth'u kapat
                    bluetoothAdapter.disable()
                    bluetoothButton.text = "Bluetooth Aç"
                }

                // Bağlanabilir cihazları göster
                cihazBulundu = false
                bağlanabilirCihazlarıGoster()
            } catch (e: SecurityException) {
                e.printStackTrace()
                Toast.makeText(this, "Bluetooth izni gereklidir.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Bağlanabilir Bluetooth cihazlarını göster
    private fun bağlanabilirCihazlarıGoster() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                try {
                    val intentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                    registerReceiver(receiver, intentFilter)
                    bluetoothAdapter.startDiscovery()

                    // Tarama tamamlandığında cihaz bulunmadıysa bilgi ver
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (bluetoothAdapter.isDiscovering) {
                            bluetoothAdapter.cancelDiscovery()
                            if (!cihazBulundu) {
                                Toast.makeText(this, "Bağlanabilir cihaz bulunamadı", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }, 30000) // 30 saniye tarama süresi

                    val discoveryFinishedIntentFilter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                    registerReceiver(discoveryFinishedReceiver, discoveryFinishedIntentFilter)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Bluetooth taraması için gerekli izinler verilmedi.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Bluetooth tarama izni verilmedi", Toast.LENGTH_SHORT).show()
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.BLUETOOTH_SCAN),
                    REQUEST_PERMISSIONS
                )
            }
        } else {
            Toast.makeText(this, "Bluetooth tarama izinleri desteklenmiyor", Toast.LENGTH_SHORT).show()
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String = intent.action!!
            if (BluetoothDevice.ACTION_FOUND == action) {
                cihazBulundu = true
                val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!

                // İzin kontrolü
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    val deviceName = device.name
                    val deviceAddress = device.address // MAC adresi
                    Toast.makeText(context, "Cihaz: $deviceName, Adres: $deviceAddress", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Bluetooth cihaz adı için izin verilmedi", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val discoveryFinishedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!cihazBulundu) {
                Toast.makeText(context, "Bağlanabilir cihaz bulunamadı", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        unregisterReceiver(discoveryFinishedReceiver)
    }

    // İzin sonuçlarını yönet
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    flasıAc()
                } else {
                    Toast.makeText(this, "Kamera izni verilmedi", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    bluetoothuAc()
                } else {
                    Toast.makeText(this, "Bluetooth izni verilmedi", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
