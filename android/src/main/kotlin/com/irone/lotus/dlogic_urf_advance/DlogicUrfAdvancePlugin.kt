package com.irone.lotus.dlogic_urf_advance

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.widget.Toast
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.lang.Exception
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.experimental.and


/** DlogicUrfAdvancePlugin */
class DlogicUrfAdvancePlugin : FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler {
    companion object {
        private var context: Context? = null
        private lateinit var handler: Handler
        private lateinit var runnable: Runnable
        private var methodChannel: MethodChannel? = null
        private var eventChannel: EventChannel? = null
        private var eventSink: EventChannel.EventSink? = null

        private lateinit var device: DlReader
        val incomingHandler: IncomingHandler = IncomingHandler()
        private lateinit var readerThread: ReaderThread

        private var lightMode: Int = 0
        private var beepMode: Int = 0
        private val default_key = ByteArray(8) { 0xFF.toByte() }
        private val key = ByteArray(8) { 0xFF.toByte() }

        val commandQueue: ConcurrentLinkedQueue<Task> = ConcurrentLinkedQueue()

        private lateinit var deviceType: String
        private lateinit var tagId: String
        private lateinit var tagUid: String
        private lateinit var blockData: String

        private var lastRespondedTagUid: String = ""
        private var lastRespondedTagUidError: String = ""

        // This static function is optional and equivalent to onAttachedToEngine. It supports the old
        // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
        // plugin registration via this function while apps migrate to use the new Android APIs
        // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
        //
        // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
        // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
        // depending on the user's project. onAttachedToEngine or registerWith must both be defined
        // in the same class.
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val instance: DlogicUrfAdvancePlugin = DlogicUrfAdvancePlugin()
            instance.onAttachedToEngine(registrar.context(), registrar.messenger())
        }

        fun makeKeyDefault() {
            System.arraycopy(default_key, 0, key, 0, 6)
        }
    }

    fun onAttachedToEngine(applicationContext: Context, binaryMessenger: BinaryMessenger) {
        context = applicationContext

        methodChannel = MethodChannel(binaryMessenger, "lotus/dlogic_urf_advance")
        methodChannel?.setMethodCallHandler(this)

        eventChannel = EventChannel(binaryMessenger, "lotus/dlogic_urf_advance_stream")
        eventChannel?.setStreamHandler(this)
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        onAttachedToEngine(flutterPluginBinding.applicationContext, flutterPluginBinding.binaryMessenger)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        context = null
        methodChannel?.setMethodCallHandler(null)
        methodChannel = null
        eventChannel?.setStreamHandler(null)
        eventChannel = null
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "init" -> result.success(init())
            "connect" -> connect(result)
            "disconnect" -> disconnect(result)
            "getReaderType" -> getReaderType(result)
            "emitUiSignal" -> emitUiSignal(result)
            "enterSleepMode" -> enterSleepMode(result)
            "leaveSleepMode" -> leaveSleepMode(result)
            "changeBeepMode" -> changeBeepMode(result, call.argument<Int>("mode")!!)
            "changeLightMode" -> changeLightMode(result, call.argument<Int>("mode")!!)
            else -> result.notImplemented()
        }
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events

        handler.post(runnable)
    }

    override fun onCancel(arguments: Any?) {
        handler.removeCallbacks(runnable)

        lastRespondedTagUid = ""
        lastRespondedTagUidError = ""

        eventSink = null
    }

    private fun init(): Boolean {
        return try {
            device = DlReader.getInstance(context, R.xml.accessory_filter, R.xml.dev_desc_filter)

            handler = Handler(Looper.getMainLooper())
            runnable = object : Runnable {
                override fun run() {
                    if (device.readerStillConnected())
                        try {
                            commandQueue.add(Task(Const.TASK_GET_CARD_ID))
                        } catch (e: Exception) {
                            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show()
                        }

                    handler.postDelayed(this, 200)
                }
            }

            readerThread = ReaderThread()
            readerThread.start()

            true
        } catch (e: Exception) {
            e.printStackTrace()

            false
        }
    }

    private fun connect(result: Result) {
        commandQueue.add(Task(Const.TASK_CONNECT, result))
    }

    private fun disconnect(result: Result) {
        commandQueue.add(Task(Const.TASK_DISCONNECT, result))
    }

    private fun getReaderType(result: Result) {
        commandQueue.add(Task(Const.TASK_GET_READER_TYPE, result))
    }

    private fun emitUiSignal(result: Result) {
        commandQueue.add(Task(Const.TASK_EMIT_UI_SIGNAL, lightMode.toByte(), beepMode.toByte(), result))
    }

    private fun enterSleepMode(result: Result) {
        commandQueue.add(Task(Const.TASK_ENTER_SLEEP, result))
    }

    private fun leaveSleepMode(result: Result) {
        commandQueue.add(Task(Const.TASK_LEAVE_SLEEP, result))
    }

    private fun changeBeepMode(result: Result, mode: Int) {
        beepMode = mode
        result.success(true)
    }

    private fun changeLightMode(result: Result, mode: Int) {
        lightMode = mode
        result.success(true)
    }

    class Const {
        companion object {
            const val TASK_CONNECT = 1
            const val TASK_GET_READER_TYPE = 2
            const val TASK_GET_CARD_ID = 3
            const val TASK_BLOCK_READ = 4
            const val TASK_DISCONNECT = 5
            const val TASK_EMIT_UI_SIGNAL = 6
            const val TASK_ENTER_SLEEP = 7
            const val TASK_LEAVE_SLEEP = 8
            const val TASK_BLOCK_WRITE = 9

            const val RESPONSE_CONNECTED = 100
            const val RESPONSE_READER_TYPE = 101
            const val RESPONSE_CARD_ID = 102
            const val RESPONSE_BLOCK_READ = 103
            const val RESPONSE_DISCONNECTED = 104
            const val RESPONSE_SUCCESS = 105
            const val RESPONSE_EMIT_UI_SIGNAL = 106
            const val RESPONSE_ENTER_SLEEP = 107
            const val RESPONSE_LEAVE_SLEEP = 108

            const val RESPONSE_ERROR = 400
            const val RESPONSE_ERROR_QUIETLY = 401
            const val RESPONSE_CONNECTED_ERROR = 402
            const val RESPONSE_DISCONNECTED_ERROR = 403
            const val RESPONSE_GET_READER_TYPE_ERROR = 404
            const val RESPONSE_GET_CARD_ID_ERROR = 405
            const val RESPONSE_EMIT_UI_SIGNAL_ERROR = 406
            const val RESPONSE_ENTER_SLEEP_ERROR = 407
            const val RESPONSE_LEAVE_SLEEP_ERROR = 408

            const val MAX_BLOCK_ADDR = 255
            const val DEFAULT_AUTH_MODE = DlReader.Consts.MIFARE_AUTHENT1A
        }
    }

    class IncomingHandler : Handler() {
        override fun handleMessage(msg: Message?) {
            when (msg?.what) {
                Const.RESPONSE_CONNECTED -> {
                    (msg.obj as Result).success(true)
                }
                Const.RESPONSE_CONNECTED_ERROR -> {
                    ((msg.obj as Map<*, *>)["result"] as Result).error("DLOGIC_URF_ERROR", (msg.obj as Map<*, *>)["error"] as String, null)
                }
                Const.RESPONSE_READER_TYPE -> {
                    deviceType = Integer.toHexString(msg.arg1)
                    (msg.obj as Result).success(deviceType)
                }
                Const.RESPONSE_GET_READER_TYPE_ERROR -> {
                    ((msg.obj as Map<*, *>)["result"] as Result).error("DLOGIC_URF_ERROR", (msg.obj as Map<*, *>)["error"] as String, null)
                }
                Const.RESPONSE_CARD_ID -> {
                    tagId = Integer.toHexString(msg.arg1)
                    tagUid = Tools.byteArr2Str(msg.obj as ByteArray)
                    if (lastRespondedTagUid != tagUid)
                    // send id to platform stream
                        eventSink?.success(tagUid)
                    lastRespondedTagUid = tagUid
                }
                Const.RESPONSE_GET_CARD_ID_ERROR -> {
                    if (msg.obj as String == "Reader error code: 8")
                        lastRespondedTagUid = ""
                    if (lastRespondedTagUidError != msg.obj as String && msg.obj as String != "Reader error code: 8")
                        eventSink?.error("DLOGIC_URF_ERROR", msg.obj as String, null)

                    lastRespondedTagUidError = msg.obj as String
                }
                Const.RESPONSE_BLOCK_READ -> {
                    blockData = Tools.byteArr2Str(msg.obj as ByteArray)
                    Toast.makeText(context, "Block successfully read", Toast.LENGTH_SHORT).show()
                }
                Const.RESPONSE_SUCCESS -> {
                    Toast.makeText(context, "Operation completed successfully", Toast.LENGTH_SHORT).show()
                }
                Const.RESPONSE_DISCONNECTED -> {
                    deviceType = ""
                    tagId = ""
                    tagUid = ""
                    blockData = ""

                    makeKeyDefault()
                    (msg.obj as Result).success(true)
                }
                Const.RESPONSE_DISCONNECTED_ERROR -> {
                    ((msg.obj as Map<*, *>)["result"] as Result).error("DLOGIC_URF_ERROR", (msg.obj as Map<*, *>)["error"] as String, null)
                }
                Const.RESPONSE_EMIT_UI_SIGNAL -> {
                    (msg.obj as Result).success(true)
                }
                Const.RESPONSE_EMIT_UI_SIGNAL_ERROR -> {
                    ((msg.obj as Map<*, *>)["result"] as Result).error("DLOGIC_URF_ERROR", (msg.obj as Map<*, *>)["error"] as String, null)
                }
                Const.RESPONSE_ENTER_SLEEP -> {
                    (msg.obj as Result).success(true)
                }
                Const.RESPONSE_ENTER_SLEEP_ERROR -> {
                    ((msg.obj as Map<*, *>)["result"] as Result).error("DLOGIC_URF_ERROR", (msg.obj as Map<*, *>)["error"] as String, null)
                }
                Const.RESPONSE_LEAVE_SLEEP -> {
                    (msg.obj as Result).success(true)
                }
                Const.RESPONSE_LEAVE_SLEEP_ERROR -> {
                    ((msg.obj as Map<*, *>)["result"] as Result).error("DLOGIC_URF_ERROR", (msg.obj as Map<*, *>)["error"] as String, null)
                }
                Const.RESPONSE_ERROR ->
                    Toast.makeText(context, msg.obj as String, Toast.LENGTH_SHORT).show()
                Const.RESPONSE_ERROR_QUIETLY -> {
                }
                else -> {
                    Toast.makeText(context, "Unknown Response", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    class ReaderThread : Thread() {
        private var stopThread: Boolean = false
        private var connected: Boolean = false
        private var cParams: DlReader.CardParams = DlReader.CardParams()

        fun stopRequest() {
            synchronized(context!!) {
                stopThread = true
            }
        }

        override fun run() {
            var localTask: Task?
            lateinit var data: ByteArray

            while (!stopThread) {
                localTask = commandQueue.poll()

                if (localTask != null) {
                    when (localTask.taskCode) {
                        Const.TASK_CONNECT -> {
                            val peekTask: Task? = commandQueue.peek()
                            var nextTaskIsNotDisconnect: Boolean = peekTask == null

                            if (!nextTaskIsNotDisconnect)
                                nextTaskIsNotDisconnect = peekTask?.taskCode != Const.TASK_DISCONNECT

                            if (!device.readerStillConnected() && nextTaskIsNotDisconnect)
                                try {
                                    device.open()
                                    connected = true
                                    incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_CONNECTED, localTask.result))
                                } catch (e: Exception) {
                                    incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_CONNECTED_ERROR, mapOf("error" to "NO_DEVICE_AVAILABLE", "result" to localTask.result)))
                                }
                            else
                                incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_CONNECTED_ERROR, mapOf("error" to "ALREADY_CONNECTED", "result" to localTask.result)))

//                            while (!device.readerStillConnected() && nextTaskIsNotDisconnect) {
//                                try {
//                                    device.open()
//                                    connected = true
//                                    localTask.result?.success(true)
//                                    incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_CONNECTED))
//                                } catch (e: Exception) {
//                                    localTask.result?.success(false)
//                                    incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_ERROR, e.message))
//                                    try {
//                                        sleep(500)
//                                    } catch (ie: InterruptedException) {
//                                        ie.printStackTrace()
//                                    }
//                                }
//                            }
                        }
                        Const.TASK_DISCONNECT -> {
                            if (connected)
                                try {
                                    device.close()
                                    connected = false
                                    incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_DISCONNECTED, localTask.result))
                                } catch (e: Exception) {
                                    incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_DISCONNECTED_ERROR, mapOf("error" to "DISCONNECT_ERROR", "result" to localTask.result)))
                                }
                            else
                                incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_DISCONNECTED_ERROR, mapOf("error" to "NO_DEVICE_CONNECTED", "result" to localTask.result)))
                        }
                        Const.TASK_GET_READER_TYPE ->
                            if (connected)
                                try {
                                    incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_READER_TYPE, device.readerType, 0, localTask.result))
                                } catch (e: Exception) {
                                    incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_GET_READER_TYPE_ERROR, mapOf("error" to e.message, "result" to localTask.result)))
                                }
                            else
                                incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_GET_READER_TYPE_ERROR, mapOf("error" to "NO_DEVICE_CONNECTED", "result" to localTask.result)))
                        Const.TASK_GET_CARD_ID ->
                            if (connected)
                                try {
                                    data = device.getCardIdEx(cParams)
                                    incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_CARD_ID, cParams.sak.toInt(), cParams.uidSize.toInt(), data))
                                } catch (e: Exception) {
                                    incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_GET_CARD_ID_ERROR, e.message))
                                }
                            else
                                incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_GET_CARD_ID_ERROR, "NO_DEVICE_CONNECTED"))
                        Const.TASK_BLOCK_READ ->
                            if (connected)
                                try {
                                    data = device.blockRead(localTask.byteParam1!!, localTask.byteParam2!!, localTask.byteArrParam1)
                                    incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_BLOCK_READ, data))
                                } catch (e: Exception) {
                                    incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_ERROR, e.message))
                                }
                        Const.TASK_BLOCK_WRITE ->
                            if (connected)
                                try {
                                    device.blockWrite(localTask.byteArrParam2, localTask.byteParam1!!, localTask.byteParam2!!, localTask.byteArrParam1)
                                    incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_SUCCESS))
                                } catch (e: Exception) {
                                    incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_ERROR, e.message))
                                }
                        Const.TASK_EMIT_UI_SIGNAL ->
                            if (connected)
                                try {
                                    device.readerUiSignal(localTask.byteParam1!!, localTask.byteParam2!!)
                                    incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_EMIT_UI_SIGNAL, localTask.result))
                                } catch (e: Exception) {
                                    incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_EMIT_UI_SIGNAL_ERROR, mapOf("error" to e.message, "result" to localTask.result)))
                                }
                            else
                                incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_EMIT_UI_SIGNAL_ERROR, mapOf("error" to "NO_DEVICE_CONNECTED", "result" to localTask.result)))
                        Const.TASK_ENTER_SLEEP ->
                            if (connected)
                                try {
                                    device.enterSleepMode()
                                    incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_ENTER_SLEEP, localTask.result))
                                } catch (e: Exception) {
                                    incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_ENTER_SLEEP_ERROR, mapOf("error" to e.message, "result" to localTask.result)))
                                }
                            else
                                incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_ENTER_SLEEP_ERROR, mapOf("error" to "NO_DEVICE_CONNECTED", "result" to localTask.result)))
                        Const.TASK_LEAVE_SLEEP ->
                            if (connected)
                                try {
                                    device.leaveSleepMode()
                                    incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_LEAVE_SLEEP, localTask.result))
                                } catch (e: Exception) {
                                    incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_LEAVE_SLEEP_ERROR, mapOf("error" to e.message, "result" to localTask.result)))
                                }
                            else
                                incomingHandler.sendMessage(incomingHandler.obtainMessage(Const.RESPONSE_LEAVE_SLEEP_ERROR, mapOf("error" to "NO_DEVICE_CONNECTED", "result" to localTask.result)))
                    }
                }
            }
        }
    }

    class Task(val taskCode: Int, val byteParam1: Byte?, val byteParam2: Byte?, val byteArrParam1: ByteArray?, val byteArrParam2: ByteArray?, val result: Result?) {

        constructor(code: Int) : this(code, null, null, null, null, null)

        constructor(code: Int, result: Result) : this(code, null, null, null, null, result)

        constructor(code: Int, p1: Byte, p2: Byte) : this(code, p1, p2, null, null, null)

        constructor(code: Int, p1: Byte, p2: Byte, result: Result) : this(code, p1, p2, null, null, result)

        constructor(code: Int, p1: Byte, p2: Byte, pa1: ByteArray) : this(code, p1, p2, pa1, null, null)

        constructor(code: Int, p1: Byte, p2: Byte, pa1: ByteArray, pa2: ByteArray) : this(code, p1, p2, pa1, pa2, null)
    }

    class Tools {
        companion object {
            fun isNumeric(s: String): Boolean {
                if (TextUtils.isEmpty(s)) {
                    return false
                }

                val p: Pattern = Pattern.compile("[-+]?[0-9]*")
                val m: Matcher = p.matcher(s)
                return m.matches()
            }

            fun byteArr2Str(byteArray: ByteArray): String {
                val sBuilder: StringBuilder = java.lang.StringBuilder(byteArray.size * 2)
                for (b: Byte in byteArray)
                    sBuilder.append(String.format("%02x", b.and(0xff.toByte())))
                return sBuilder.toString()
            }
        }
    }
}
