package com.awareframework.activityrecognition.awareframework_activityrecognition


import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.StreamHandler

import android.util.Log
import android.os.Looper
import com.awareframework.android.core.AwareSensor
import io.flutter.plugin.common.EventChannel.EventSink

import com.awareframework.android.core.db.Engine
import com.tappun.android.sensor.transition_activityrecognitionensortransition_activityrecognition.ActivityRecognitionSensor
import com.tappun.android.sensor.transition_activityrecognitionensortransition_activityrecognition.model.ActivityRecognitionData

import android.content.Intent
import android.os.Handler

/** AwareframeworkActivityrecognitionPlugin */
class AwareframeworkActivityrecognitionPlugin: AwareFlutterPluginCore(), FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    this.setMethodChannel(flutterPluginBinding, this, "awareframework_activityrecognition/method")
    this.setEventChannels(flutterPluginBinding, this, listOf("awareframework_activityrecognition/event",
      "awareframework_activityrecognition/event_on_data_changed"))
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    this.resetMethodChannel()
    this.resetEventChannels()
  }
}

interface AwareFlutterPluginMethodHandler{
  fun beginMethodHandle(call: MethodCall, result: Result)
  fun endMethodHandle(call: MethodCall, result: Result)
}

open class AwareFlutterPluginCore: StreamHandler, MethodCallHandler {
  var streamHandlers: ArrayList<MyStreamHandler> = ArrayList()

  var methodEventHandler:AwareFlutterPluginMethodHandler? = null
  var methodChannel : MethodChannel? = null
  var eventChannels = ArrayList<EventChannel>()

  var binding:FlutterPlugin.FlutterPluginBinding? = null


  public fun setMethodChannel(binding: FlutterPlugin.FlutterPluginBinding,
                              instance:MethodCallHandler,
                              channelName:String) {
    this.binding = binding
    this.methodChannel = MethodChannel(binding.binaryMessenger, channelName)
    this.methodChannel?.setMethodCallHandler(instance)
  }

  public fun setEventChannels(binding: FlutterPlugin.FlutterPluginBinding,
                              instance:StreamHandler,
                              channelNames:List<String>){
    this.binding = binding
    for (name in channelNames) {
      println("setevent channnels no for")
      val stream = EventChannel(binding.binaryMessenger, name)
      stream.setStreamHandler(instance)
      eventChannels.add(stream)
    }
  }

  public fun resetMethodChannel(){
    this.methodChannel?.setMethodCallHandler(null)
    this.methodChannel = null
  }

  public fun resetEventChannels(){
    this.eventChannels.forEach {
      it.setStreamHandler(null)
    }
    this.eventChannels.clear()
  }

    override fun onMethodCall(call: MethodCall, result: Result) {
      this.methodEventHandler?.beginMethodHandle(call, result)

      Log.d(this.toString(), call.method)
      print("on method call")

      when (call.method) {
        "start" -> {
          this.start(call, result)
          println("this.start")
        }
        "sync" -> {
          this.sync(call, result)
        }
        "stop" -> {
          this.stop(call, result)
        }
        "enable" -> {
          this.enable(call, result)
        }
        "disable" -> {
          this.disable(call, result)
        }
        "is_enable" -> {
          this.isEnable(call, result)
        }
        "cancel_broadcast_stream" -> {
          this.cancelStreamHandler(call, result)
        }
        "set_label" -> {
          this.setLabel(call, result)
        }
        else -> {
          result.notImplemented()
        }
      }

      this.methodEventHandler?.endMethodHandle(call, result)
    }


    open fun start(call: MethodCall, result: Result) {
      println("open fun start")
      println(call)
      println(result)

      this.binding?.applicationContext?.let { appContext ->
        val config = ActivityRecognitionSensor.Config()
        val args = call.arguments
        if (args is Map<*, *>) {
          val debug = args["debug"]
          val deviceId = args["deviceId"]
          val dbHost = args["dbHost"]

          if (debug is Boolean) config.debug = debug
          if (deviceId is String) config.deviceId = deviceId
          if (dbHost is String) config.dbHost = dbHost
        }


        ActivityRecognitionSensor.start(appContext, config.apply {
          dbType = Engine.DatabaseType.ROOM
          debug = true

          sensorObserver = object : ActivityRecognitionSensor.Observer {
            override fun onDataChanged(data: ActivityRecognitionData) {

              for(handler in streamHandlers){
                handler.eventSink?.let{
                  val d:Map<String, Any?> = mapOf<String, Any?>(
                    "detectedActivity" to data.detectedActivity,
                    "activityTransiton" to data.activityTransiton,
                    "timeStamp" to data.timestamp,
                    "timeZone" to data.timezone)
                  it.success(d)
                }
              }
            }
          }
        })
      }
    }

    open fun stop(call: MethodCall, result: Result) {
      this.binding?.applicationContext?.let { appContext ->
        ActivityRecognitionSensor.stop(appContext)
      }
    }

    open fun sync(call: MethodCall, result: Result) {
      if (call.arguments != null) {
        call.arguments.let { args ->
          if (args is Map<*, *>) args["force"].let { state ->
            if (state is Boolean) {
              val intent = Intent(ActivityRecognitionSensor.ACTION_AWARE_ACTIVITYRECOGNITION_SYNC)
              binding?.applicationContext?.sendBroadcast(intent)
              return
            }
          }
        }
      }
    }

    open fun enable(call: MethodCall, result: Result) {
    }

    open fun disable(call: MethodCall, result: Result) {
    }

    open fun isEnable(call: MethodCall, result: Result) {
    }

    open fun setLabel(call: MethodCall, result: Result) {

    }


    public fun cancelStreamHandler(call: MethodCall, result: Result) {
      call.arguments.let { args ->
        when(args) {
          is Map<*, *> -> {
            when(val eventName = args["name"]){
              is String -> removeDuplicateEventNames(eventName)
              else -> {}
            }
          }
          else -> {}
        }
      }
    }

    private fun removeDuplicateEventNames(eventName:String){
      val events = ArrayList<MyStreamHandler>()
      this.streamHandlers.forEach { myStreamHandler ->
        if (myStreamHandler.eventName == eventName) {
          Log.d("[NOTE]",
            "($eventName) is duplicate. The current event channel is overwritten by the new event channel."
          )
          events.add(myStreamHandler)
        }
      }
      this.streamHandlers.removeAll(events)
    }

    public fun getStreamHandlers(name: String): List<MyStreamHandler>? {
      this.getStreamHandler(name)?.let {
        return mutableListOf(it)
      }
      return null
    }

    private fun getStreamHandler(name: String):MyStreamHandler? {
      for (handler in this.streamHandlers){
        if (handler.eventName == name) {
          return handler
        }
      }
      return null
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
      arguments?.let { args ->
        when (args) {
          is Map<*, *> -> {
            when (val eventName = args["name"]) {
              is String -> {
                this.removeDuplicateEventNames(eventName)
                events?.let {
                  val handler = MyStreamHandler(eventName, MainThreadEventSink(events))
                  streamHandlers.add(handler)
                }
              }
              else -> {}
            }
          }
          else -> {}
        }
      }
    }

    override fun onCancel(arguments: Any?) {

    }

  }


  class MyStreamHandler(val eventName: String, var eventSink: MainThreadEventSink?){}


  class MainThreadEventSink internal constructor(private val eventSink: EventSink) :
    EventSink {
    private val handler: Handler = Handler(Looper.getMainLooper())

    override fun success(o: Any) {
      handler.post(Runnable { eventSink.success(o) })
    }

    override fun error(s: String, s1: String, o: Any) {
      handler.post(Runnable { eventSink.error(s, s1, o) })
    }

    override fun endOfStream() {
    }

  }

