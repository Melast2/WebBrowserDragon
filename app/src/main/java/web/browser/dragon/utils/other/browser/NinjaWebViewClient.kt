package web.browser.dragon.utils.other.browser

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.preference.PreferenceManager
import android.view.View
import android.webkit.*
import android.widget.EditText
import android.widget.ImageView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import web.browser.dragon.R
import web.browser.dragon.utils.other.database.Record
import web.browser.dragon.utils.other.database.RecordAction
import web.browser.dragon.utils.other.unit.BrowserUnit
import web.browser.dragon.utils.other.unit.HelperUnit
import web.browser.dragon.utils.other.unit.RecordUnit
import web.browser.dragon.utils.other.view.NinjaWebView
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.lang.Exception
import java.util.*

class NinjaWebViewClient(ninjaWebView: NinjaWebView) : WebViewClient() {
    private val ninjaWebView: NinjaWebView = ninjaWebView
    private val context: Context = ninjaWebView.context
    private val sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    //    private val adBlock = AdBlock(context)

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)

        Timber.d("TAG_NinjaWebViewClient_2")
    }
    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)

        Timber.d("TAG_NinjaWebViewClient_1")
        ninjaWebView.isBackPressed = false
        if (ninjaWebView.isForeground) {
            ninjaWebView.invalidate()
        } else {
            ninjaWebView.postInvalidate()
        }
        if (sp.getBoolean("onPageFinished", false)) {
            Objects.requireNonNull(sp.getString("sp_onPageFinished", ""))?.let {
                view.evaluateJavascript(
                    it,
                    null
                )
            }
        }
        if (ninjaWebView.isSaveData) {
            view.evaluateJavascript(
                "var links=document.getElementsByTagName('video'); for(let i=0;i<links.length;i++){links[i].pause()};",
                null
            )
        }
        if (ninjaWebView.isHistory) {
            val action = RecordAction(ninjaWebView.getContext())
            action.open(true)
            if (action.checkUrl(ninjaWebView.getUrl(), RecordUnit.TABLE_HISTORY)) {
                action.deleteURL(ninjaWebView.getUrl(), RecordUnit.TABLE_HISTORY)
            }
            action.addHistory(
                Record(
                    ninjaWebView.getTitle(),
                    ninjaWebView.getUrl(),
                    System.currentTimeMillis(),
                    0,
                    0,
                    ninjaWebView.isDesktopMode,
                    ninjaWebView.isNightMode,
                    0
                )
            )
            action.close()
        }
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        ninjaWebView.setStopped(false)
        ninjaWebView.resetFavicon()
        super.onPageStarted(view, url, favicon)
        if (sp.getBoolean("onPageStarted", false)) {
            Objects.requireNonNull(sp.getString("sp_onPageStarted", ""))?.let {
                view.evaluateJavascript(
                    it,
                    null
                )
            }
        }
        if (ninjaWebView.isFingerPrintProtection) {
            //Block WebRTC requests which can reveal local IP address
            //Tested with https://diafygi.github.io/webrtc-ips/
            view.evaluateJavascript(
                "['createOffer', 'createAnswer','setLocalDescription', 'setRemoteDescription'].forEach(function(method) {\n" +
                        "    webkitRTCPeerConnection.prototype[method] = function() {\n" +
                        "      console.log('webRTC snoop');\n" +
                        "      return null;\n" +
                        "    };\n" +
                        "  });", null
            )

            //Prevent canvas fingerprinting by randomizing
            //can be tested e.g. at https://webbrowsertools.com
            //
            //The Javascript part below is taken from "Canvas Fingerprint Defender", Firefox plugin, Version 0.1.9, by ilGur
            //The source code has been published originally under Mozilla Public License V2.0. You can obtain a copy of the license at https://mozilla.org/MPL/2.0/
            //The author has given explicit written permission to use his code under GPL V3 in this project.
            view.evaluateJavascript(
                ("\n" +
                        "  const toBlob = HTMLCanvasElement.prototype.toBlob;\n" +
                        "  const toDataURL = HTMLCanvasElement.prototype.toDataURL;\n" +
                        "  const getImageData = CanvasRenderingContext2D.prototype.getImageData;\n" +
                        "  //\n" +
                        "  var noisify = function (canvas, context) {\n" +
                        "    if (context) {\n" +
                        "      const shift = {\n" +
                        "        'r': Math.floor(Math.random() * 10) - 5,\n" +
                        "        'g': Math.floor(Math.random() * 10) - 5,\n" +
                        "        'b': Math.floor(Math.random() * 10) - 5,\n" +
                        "        'a': Math.floor(Math.random() * 10) - 5\n" +
                        "      };\n" +
                        "      //\n" +
                        "      const width = canvas.width;\n" +
                        "      const height = canvas.height;\n" +
                        "      if (width && height) {\n" +
                        "        const imageData = getImageData.apply(context, [0, 0, width, height]);\n" +
                        "        for (let i = 0; i < height; i++) {\n" +
                        "          for (let j = 0; j < width; j++) {\n" +
                        "            const n = ((i * (width * 4)) + (j * 4));\n" +
                        "            imageData.data[n + 0] = imageData.data[n + 0] + shift.r;\n" +
                        "            imageData.data[n + 1] = imageData.data[n + 1] + shift.g;\n" +
                        "            imageData.data[n + 2] = imageData.data[n + 2] + shift.b;\n" +
                        "            imageData.data[n + 3] = imageData.data[n + 3] + shift.a;\n" +
                        "          }\n" +
                        "        }\n" +
                        "        //\n" +
                        "        window.top.postMessage(\"canvas-fingerprint-defender-alert\", '*');\n" +
                        "        context.putImageData(imageData, 0, 0); \n" +
                        "      }\n" +
                        "    }\n" +
                        "  };\n" +
                        "  //\n" +
                        "  Object.defineProperty(HTMLCanvasElement.prototype, \"toBlob\", {\n" +
                        "    \"value\": function () {\n" +
                        "      noisify(this, this.getContext(\"2d\"));\n" +
                        "      return toBlob.apply(this, arguments);\n" +
                        "    }\n" +
                        "  });\n" +
                        "  //\n" +
                        "  Object.defineProperty(HTMLCanvasElement.prototype, \"toDataURL\", {\n" +
                        "    \"value\": function () {\n" +
                        "      noisify(this, this.getContext(\"2d\"));\n" +
                        "      return toDataURL.apply(this, arguments);\n" +
                        "    }\n" +
                        "  });\n" +
                        "  //\n" +
                        "  Object.defineProperty(CanvasRenderingContext2D.prototype, \"getImageData\", {\n" +
                        "    \"value\": function () {\n" +
                        "      noisify(this.canvas, this);\n" +
                        "      return getImageData.apply(this, arguments);\n" +
                        "    }\n" +
                        "  });"), null
            )

            //Prevent WebGL fingerprinting by randomizing
            //can be tested e.g. at https://webbrowsertools.com
            //
            //The Javascript part below is taken from "WebGL Fingerprint Defender", Firefox plugin, Version 0.1.5, by ilGur
            //The source code has been published originally under Mozilla Public License V2.0. You can obtain a copy of the license at https://mozilla.org/MPL/2.0/
            //The author has given explicit written permission to use his code under GPL V3 in this project.
            view.evaluateJavascript(
                ("\n" +
                        "  var config = {\n" +
                        "    \"random\": {\n" +
                        "      \"value\": function () {\n" +
                        "        return Math.random();\n" +
                        "      },\n" +
                        "      \"item\": function (e) {\n" +
                        "        var rand = e.length * config.random.value();\n" +
                        "        return e[Math.floor(rand)];\n" +
                        "      },\n" +
                        "      \"number\": function (power) {\n" +
                        "        var tmp = [];\n" +
                        "        for (var i = 0; i < power.length; i++) {\n" +
                        "          tmp.push(Math.pow(2, power[i]));\n" +
                        "        }\n" +
                        "        /*  */\n" +
                        "        return config.random.item(tmp);\n" +
                        "      },\n" +
                        "      \"int\": function (power) {\n" +
                        "        var tmp = [];\n" +
                        "        for (var i = 0; i < power.length; i++) {\n" +
                        "          var n = Math.pow(2, power[i]);\n" +
                        "          tmp.push(new Int32Array([n, n]));\n" +
                        "        }\n" +
                        "        /*  */\n" +
                        "        return config.random.item(tmp);\n" +
                        "      },\n" +
                        "      \"float\": function (power) {\n" +
                        "        var tmp = [];\n" +
                        "        for (var i = 0; i < power.length; i++) {\n" +
                        "          var n = Math.pow(2, power[i]);\n" +
                        "          tmp.push(new Float32Array([1, n]));\n" +
                        "        }\n" +
                        "        /*  */\n" +
                        "        return config.random.item(tmp);\n" +
                        "      }\n" +
                        "    },\n" +
                        "    \"spoof\": {\n" +
                        "      \"webgl\": {\n" +
                        "        \"buffer\": function (target) {\n" +
                        "          var proto = target.prototype ? target.prototype : target.__proto__;\n" +
                        "          const bufferData = proto.bufferData;\n" +
                        "          Object.defineProperty(proto, \"bufferData\", {\n" +
                        "            \"value\": function () {\n" +
                        "              var index = Math.floor(config.random.value() * arguments[1].length);\n" +
                        "              var noise = arguments[1][index] !== undefined ? 0.1 * config.random.value() * arguments[1][index] : 0;\n" +
                        "              //\n" +
                        "              arguments[1][index] = arguments[1][index] + noise;\n" +
                        "              window.top.postMessage(\"webgl-fingerprint-defender-alert\", '*');\n" +
                        "              //\n" +
                        "              return bufferData.apply(this, arguments);\n" +
                        "            }\n" +
                        "          });\n" +
                        "        },\n" +
                        "        \"parameter\": function (target) {\n" +
                        "          var proto = target.prototype ? target.prototype : target.__proto__;\n" +
                        "          const getParameter = proto.getParameter;\n" +
                        "          Object.defineProperty(proto, \"getParameter\", {\n" +
                        "            \"value\": function () {\n" +
                        "              window.top.postMessage(\"webgl-fingerprint-defender-alert\", '*');\n" +
                        "              //\n" +
                        "              if (arguments[0] === 3415) return 0;\n" +
                        "              else if (arguments[0] === 3414) return 24;\n" +
                        "              else if (arguments[0] === 36348) return 30;\n" +
                        "              else if (arguments[0] === 7936) return \"WebKit\";\n" +
                        "              else if (arguments[0] === 37445) return \"Google Inc.\";\n" +
                        "              else if (arguments[0] === 7937) return \"WebKit WebGL\";\n" +
                        "              else if (arguments[0] === 3379) return config.random.number([14, 15]);\n" +
                        "              else if (arguments[0] === 36347) return config.random.number([12, 13]);\n" +
                        "              else if (arguments[0] === 34076) return config.random.number([14, 15]);\n" +
                        "              else if (arguments[0] === 34024) return config.random.number([14, 15]);\n" +
                        "              else if (arguments[0] === 3386) return config.random.int([13, 14, 15]);\n" +
                        "              else if (arguments[0] === 3413) return config.random.number([1, 2, 3, 4]);\n" +
                        "              else if (arguments[0] === 3412) return config.random.number([1, 2, 3, 4]);\n" +
                        "              else if (arguments[0] === 3411) return config.random.number([1, 2, 3, 4]);\n" +
                        "              else if (arguments[0] === 3410) return config.random.number([1, 2, 3, 4]);\n" +
                        "              else if (arguments[0] === 34047) return config.random.number([1, 2, 3, 4]);\n" +
                        "              else if (arguments[0] === 34930) return config.random.number([1, 2, 3, 4]);\n" +
                        "              else if (arguments[0] === 34921) return config.random.number([1, 2, 3, 4]);\n" +
                        "              else if (arguments[0] === 35660) return config.random.number([1, 2, 3, 4]);\n" +
                        "              else if (arguments[0] === 35661) return config.random.number([4, 5, 6, 7, 8]);\n" +
                        "              else if (arguments[0] === 36349) return config.random.number([10, 11, 12, 13]);\n" +
                        "              else if (arguments[0] === 33902) return config.random.float([0, 10, 11, 12, 13]);\n" +
                        "              else if (arguments[0] === 33901) return config.random.float([0, 10, 11, 12, 13]);\n" +
                        "              else if (arguments[0] === 37446) return config.random.item([\"Graphics\", \"HD Graphics\", \"Intel(R) HD Graphics\"]);\n" +
                        "              else if (arguments[0] === 7938) return config.random.item([\"WebGL 1.0\", \"WebGL 1.0 (OpenGL)\", \"WebGL 1.0 (OpenGL Chromium)\"]);\n" +
                        "              else if (arguments[0] === 35724) return config.random.item([\"WebGL\", \"WebGL GLSL\", \"WebGL GLSL ES\", \"WebGL GLSL ES (OpenGL Chromium\"]);\n" +
                        "              //\n" +
                        "              return getParameter.apply(this, arguments);\n" +
                        "            }\n" +
                        "          });\n" +
                        "        }\n" +
                        "      }\n" +
                        "    }\n" +
                        "  };\n" +
                        "  //\n" +
                        "  config.spoof.webgl.buffer(WebGLRenderingContext);\n" +
                        "  config.spoof.webgl.buffer(WebGL2RenderingContext);\n" +
                        "  config.spoof.webgl.parameter(WebGLRenderingContext);\n" +
                        "  config.spoof.webgl.parameter(WebGL2RenderingContext);"), null
            )

            //Prevent AudioContext fingerprinting by randomizing
            //can be tested e.g. at https://webbrowsertools.com
            //
            //The Javascript part below is taken from "AudioContext Fingerprint Defender", Firefox plugin, Version 0.1.6, by ilGur
            //The source code has been published originally under Mozilla Public License V2.0. You can obtain a copy of the license at https://mozilla.org/MPL/2.0/
            //The author has given explicit written permission to use his code under GPL V3 in this project.
            view.evaluateJavascript(
                ("\n" +
                        "    const context = {\n" +
                        "    \"BUFFER\": null,\n" +
                        "    \"getChannelData\": function (e) {\n" +
                        "      const getChannelData = e.prototype.getChannelData;\n" +
                        "      Object.defineProperty(e.prototype, \"getChannelData\", {\n" +
                        "        \"value\": function () {\n" +
                        "          const results_1 = getChannelData.apply(this, arguments);\n" +
                        "          if (context.BUFFER !== results_1) {\n" +
                        "            context.BUFFER = results_1;\n" +
                        "            for (var i = 0; i < results_1.length; i += 100) {\n" +
                        "              let index = Math.floor(Math.random() * i);\n" +
                        "              results_1[index] = results_1[index] + Math.random() * 0.0000001;\n" +
                        "            }\n" +
                        "          }\n" +
                        "          //\n" +
                        "          return results_1;\n" +
                        "        }\n" +
                        "      });\n" +
                        "    },\n" +
                        "    \"createAnalyser\": function (e) {\n" +
                        "      const createAnalyser = e.prototype.__proto__.createAnalyser;\n" +
                        "      Object.defineProperty(e.prototype.__proto__, \"createAnalyser\", {\n" +
                        "        \"value\": function () {\n" +
                        "          const results_2 = createAnalyser.apply(this, arguments);\n" +
                        "          const getFloatFrequencyData = results_2.__proto__.getFloatFrequencyData;\n" +
                        "          Object.defineProperty(results_2.__proto__, \"getFloatFrequencyData\", {\n" +
                        "            \"value\": function () {\n" +
                        "              const results_3 = getFloatFrequencyData.apply(this, arguments);\n" +
                        "              for (var i = 0; i < arguments[0].length; i += 100) {\n" +
                        "                let index = Math.floor(Math.random() * i);\n" +
                        "                arguments[0][index] = arguments[0][index] + Math.random() * 0.1;\n" +
                        "              }\n" +
                        "              //\n" +
                        "              return results_3;\n" +
                        "            }\n" +
                        "          });\n" +
                        "          //\n" +
                        "          return results_2;\n" +
                        "        }\n" +
                        "      });\n" +
                        "    }\n" +
                        "  };\n" +
                        "  //\n" +
                        "  context.getChannelData(AudioBuffer);\n" +
                        "  context.createAnalyser(AudioContext);\n" +
                        "  context.getChannelData(OfflineAudioContext);\n" +
                        "  context.createAnalyser(OfflineAudioContext);  "), null
            )

            //Prevent Font fingerprinting by randomizing
            //can be tested e.g. at https://webbrowsertools.com
            //
            //The Javascript part below is taken from "Font Fingerprint Defender", Firefox plugin, Version 0.1.3, by ilGur
            //The source code has been published originally under Mozilla Public License V2.0. You can obtain a copy of the license at https://mozilla.org/MPL/2.0/
            //The author has given explicit written permission to use his code under GPL V3 in this project.
            view.evaluateJavascript(
                ("\n" +
                        "  var rand = {\n" +
                        "    \"noise\": function () {\n" +
                        "      var SIGN = Math.random() < Math.random() ? -1 : 1;\n" +
                        "      return Math.floor(Math.random() + SIGN * Math.random());\n" +
                        "    },\n" +
                        "    \"sign\": function () {\n" +
                        "      const tmp = [-1, -1, -1, -1, -1, -1, +1, -1, -1, -1];\n" +
                        "      const index = Math.floor(Math.random() * tmp.length);\n" +
                        "      return tmp[index];\n" +
                        "    }\n" +
                        "  };\n" +
                        "  //\n" +
                        "  Object.defineProperty(HTMLElement.prototype, \"offsetHeight\", {\n" +
                        "    get () {\n" +
                        "      const height = Math.floor(this.getBoundingClientRect().height);\n" +
                        "      const valid = height && rand.sign() === 1;\n" +
                        "      const result = valid ? height + rand.noise() : height;\n" +
                        "      //\n" +
                        "      if (valid && result !== height) {\n" +
                        "        window.top.postMessage(\"font-fingerprint-defender-alert\", '*');\n" +
                        "      }\n" +
                        "      //\n" +
                        "      return result;\n" +
                        "    }\n" +
                        "  });\n" +
                        "  //\n" +
                        "  Object.defineProperty(HTMLElement.prototype, \"offsetWidth\", {\n" +
                        "    get () {\n" +
                        "      const width = Math.floor(this.getBoundingClientRect().width);\n" +
                        "      const valid = width && rand.sign() === 1;\n" +
                        "      const result = valid ? width + rand.noise() : width;\n" +
                        "      //\n" +
                        "      if (valid && result !== width) {\n" +
                        "        window.top.postMessage(\"font-fingerprint-defender-alert\", '*');\n" +
                        "      }\n" +
                        "      //\n" +
                        "      return result;\n" +
                        "    }\n" +
                        "  });"), null
            )

            //Spoof screen resolution, color depth: set values like in Tor browser, random values for device memory, hardwareConcurrency, remove battery, network connection, keyboard, media devices info, prevent sendBeacon
            view.evaluateJavascript(
                ("" +
                        "Object.defineProperty(window, 'devicePixelRatio',{value:1});" +
                        "Object.defineProperty(window.screen, 'width',{value:1000});" +
                        "Object.defineProperty(window.screen, 'availWidth',{value:1000});" +
                        "Object.defineProperty(window.screen, 'height',{value:900});" +
                        "Object.defineProperty(window.screen, 'availHeight',{value:900});" +
                        "Object.defineProperty(window.screen, 'colorDepth',{value:24});" +
                        "Object.defineProperty(window, 'outerWidth',{value:1000});" +
                        "Object.defineProperty(window, 'outerHeight',{value:900});" +
                        "Object.defineProperty(window, 'innerWidth',{value:1000});" +
                        "Object.defineProperty(window, 'innerHeight',{value:900});" +
                        "Object.defineProperty(navigator, 'getBattery',{value:function(){}});" +
                        "const ram=Math.pow(2,Math.floor(Math.random() * 4));Object.defineProperty(navigator, 'deviceMemory',{value:ram});" +
                        "const hw=Math.pow(2,Math.floor(Math.random() * 4));Object.defineProperty(navigator, 'hardwareConcurrency',{value:hw});" +
                        "Object.defineProperty(navigator, 'connection',{value:null});" +
                        "Object.defineProperty(navigator, 'keyboard',{value:null});" +
                        "Object.defineProperty(navigator, 'sendBeacon',{value:null});"), null
            )
            if (!ninjaWebView.isCamera) {
                view.evaluateJavascript(
                    "" +
                            "Object.defineProperty(navigator, 'mediaDevices',{value:null});", null
                )
            }
        }
    }

    override fun onLoadResource(view: WebView, url: String) {
        if (sp.getBoolean("onLoadResource", false)) {
            Objects.requireNonNull(sp.getString("sp_onLoadResource", ""))?.let {
                view.evaluateJavascript(
                    it,
                    null
                )
            }
        }
        if (ninjaWebView.isFingerPrintProtection) {
            view.evaluateJavascript(
                "var test=document.querySelector(\"a[ping]\"); if(test!==null){test.removeAttribute('ping')};",
                null
            )
            //do not allow ping on http only pages (tested with http://tests.caniuse.com)
        }
        if (view.settings.useWideViewPort && (view.width < 1300)) view.evaluateJavascript(
            "document.querySelector('meta[name=\"viewport\"]').setAttribute('content', 'width=1200px');",
            null
        )
        //  Client-side detection for GlobalPrivacyControl
        view.evaluateJavascript(
            "if (navigator.globalPrivacyControl === undefined) { Object.defineProperty(navigator, 'globalPrivacyControl', { value: true, writable: false,configurable: false});} else {try { navigator.globalPrivacyControl = true;} catch (e) { console.error('globalPrivacyControl is not writable: ', e); }};",
            null
        )
        //  Script taken from:
        //
        //  donotsell.js
        //  DuckDuckGo
        //
        //  Copyright © 2020 DuckDuckGo. All rights reserved.
        //
        //  Licensed under the Apache License, Version 2.0 (the "License");
        //  you may not use this file except in compliance with the License.
        //  You may obtain a copy of the License at
        //
        //  http://www.apache.org/licenses/LICENSE-2.0
        //
        //  Unless required by applicable law or agreed to in writing, software
        //  distributed under the License is distributed on an "AS IS" BASIS,
        //  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        //  See the License for the specific language governing permissions and
        //  limitations under the License.
        //
        view.evaluateJavascript(
            "if (navigator.doNotTrack === null) { Object.defineProperty(navigator, 'doNotTrack', { value: 1, writable: false,configurable: false});} else {try { navigator.doNotTrack = 1;} catch (e) { console.error('doNotTrack is not writable: ', e); }};",
            null
        )
        view.evaluateJavascript(
            "if (window.doNotTrack === undefined) { Object.defineProperty(window, 'doNotTrack', { value: 1, writable: false,configurable: false});} else {try { window.doNotTrack = 1;} catch (e) { console.error('doNotTrack is not writable: ', e); }};",
            null
        )
        view.evaluateJavascript(
            "if (navigator.msDoNotTrack === undefined) { Object.defineProperty(navigator, 'msDoNotTrack', { value: 1, writable: false,configurable: false});} else {try { navigator.msDoNotTrack = 1;} catch (e) { console.error('msDoNotTrack is not writable: ', e); }};",
            null
        )
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val uri = request.url
        if (ninjaWebView.isBackPressed) {
            return false
        } else {
            // handle the url by implementing your logic
            val url = uri.toString()
            if (url.startsWith("http://") || url.startsWith("https://")) return false
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                view.context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Timber.d("TAG_webviewclient: shouldOverrideUrlLoading Exception:$e")
                return true
            }
        }
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        return if (ninjaWebView.isAdBlock
//            && adBlock.isAd(request.url.toString())
        ) {
            WebResourceResponse(
                BrowserUnit.MIME_TYPE_TEXT_PLAIN,
                BrowserUnit.URL_ENCODING,
                ByteArrayInputStream("".toByteArray())
            )
        } else super.shouldInterceptRequest(view, request)
    }

    override fun onFormResubmission(view: WebView, doNotResend: Message, resend: Message) {
        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(R.string.app_warning)
        builder.setIcon(R.drawable.icon_alert)
        builder.setMessage(R.string.dialog_content_resubmission)
        builder.setPositiveButton(R.string.app_ok) { dialog, whichButton -> resend.sendToTarget() }
        builder.setNegativeButton(R.string.app_cancel) { dialog, whichButton -> dialog.cancel() }
        val dialog = builder.create()
        dialog.show()
        dialog.setOnCancelListener({ dialog1: DialogInterface? -> doNotResend.sendToTarget() })
        HelperUnit.setupDialog(context, dialog)
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        val message: String
        when (error.primaryError) {
            SslError.SSL_UNTRUSTED -> message = "\"Certificate authority is not trusted.\""
            SslError.SSL_EXPIRED -> message = "\"Certificate has expired.\""
            SslError.SSL_IDMISMATCH -> message = "\"Certificate Hostname mismatch.\""
            SslError.SSL_NOTYETVALID -> message = "\"Certificate is not yet valid.\""
            SslError.SSL_DATE_INVALID -> message = "\"Certificate date is invalid.\""
            else -> message = "\"Certificate is invalid.\""
        }
        val text = message + " - " + context.getString(R.string.dialog_content_ssl_error)
        val builder = MaterialAlertDialogBuilder(context)
        builder.setIcon(R.drawable.icon_alert)
        builder.setTitle(R.string.app_warning)
        builder.setMessage(text)
        builder.setPositiveButton(R.string.app_ok) { dialog, whichButton -> handler.proceed() }
        builder.setNegativeButton(R.string.app_cancel) { dialog, whichButton -> dialog.cancel() }
        val dialog = builder.create()
        dialog.show()
        dialog.setOnCancelListener({ dialog1: DialogInterface? -> handler.cancel() })
        HelperUnit.setupDialog(context, dialog)
    }

    override fun onReceivedHttpAuthRequest(
        view: WebView,
        handler: HttpAuthHandler,
        host: String,
        realm: String
    ) {
        val builder = MaterialAlertDialogBuilder(context)
        val dialogView = View.inflate(context, R.layout.dialog_edit_title, null)
        val edit_title_layout: TextInputLayout = dialogView.findViewById(R.id.edit_title_layout)
        val edit_userName_layout: TextInputLayout =
            dialogView.findViewById(R.id.edit_userName_layout)
        val edit_PW_layout: TextInputLayout = dialogView.findViewById(R.id.edit_PW_layout)
        val ib_icon = dialogView.findViewById<ImageView>(R.id.edit_icon)
        ib_icon.visibility = View.GONE
        edit_title_layout.visibility = View.GONE
        edit_userName_layout.visibility = View.VISIBLE
        edit_PW_layout.visibility = View.VISIBLE
        val pass_userNameET = dialogView.findViewById<EditText>(R.id.edit_userName)
        val pass_userPWET = dialogView.findViewById<EditText>(R.id.edit_PW)
        builder.setView(dialogView)
        builder.setTitle("HttpAuthRequest")
        builder.setIcon(R.drawable.icon_alert)
        builder.setMessage(view.url)
        builder.setPositiveButton(R.string.app_ok) { dialog, whichButton ->
            val user: String = pass_userNameET.getText().toString().trim { it <= ' ' }
            val pass: String = pass_userPWET.getText().toString().trim { it <= ' ' }
            handler.proceed(user, pass)
            dialog.cancel()
        }
        builder.setNegativeButton(R.string.app_cancel) { dialog, whichButton -> dialog.cancel() }
        val dialog = builder.create()
        dialog.show()
        HelperUnit.setupDialog(context, dialog)
        dialog.setOnCancelListener { dialog1: DialogInterface ->
            handler.cancel()
            dialog1.cancel()
        }
    }

    init {
//        context = ninjaWebView.getContext()
        //        adBlock = AdBlock(context)
    }
}