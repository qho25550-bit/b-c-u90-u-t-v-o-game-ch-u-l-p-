package com.carter.dualaidebate

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var question: EditText
    private lateinit var result: LinearLayout
    private lateinit var runButton: Button

    // Đổi thành URL HTTPS của backend đã triển khai.
    // Không đặt API key OpenAI/Gemini trong ứng dụng Android.
    private val backendUrl = "https://YOUR-BACKEND-DOMAIN.example.com/debate"

    private val bg = Color.rgb(8, 8, 12)
    private val panel = Color.rgb(24, 17, 22)
    private val red = Color.rgb(255, 55, 79)
    private val white = Color.rgb(245, 242, 245)
    private val muted = Color.rgb(180, 170, 180)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildUi()
    }

    private fun buildUi() {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(28))
        }
        scroll.addView(page)
        setContentView(scroll)

        page.addView(label("DUAL AI DEBATE", 27, red, true))
        page.addView(label("HAI TRÍ TUỆ • MỘT KẾT LUẬN TỐT HƠN", 11, muted))
        page.addView(space(18))
        page.addView(section("AI A  /  NGƯỜI ỦNG HỘ", "OpenAI", red))
        page.addView(space(8))
        page.addView(section("AI B  /  NGƯỜI PHẢN BIỆN", "Google Gemini", Color.rgb(183, 145, 255)))
        page.addView(space(18))
        page.addView(label("NHẬP CÂU HỎI CỦA BẠN", 14, white, true))

        question = EditText(this).apply {
            hint = "Ví dụ: Có nên học lập trình từ cấp 2 không?"
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 16f
            minLines = 3
            gravity = Gravity.TOP
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(panel)
        }
        page.addView(question, LinearLayout.LayoutParams(-1, dp(120)))
        page.addView(space(12))

        runButton = Button(this).apply {
            text = "BẮT ĐẦU TRANH LUẬN"
            setTextColor(Color.WHITE)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(red))
            setOnClickListener { startDebate() }
        }
        page.addView(runButton, LinearLayout.LayoutParams(-1, dp(54)))
        page.addView(space(14))

        val stopInfo = label("Tối đa 2 vòng phản biện • Có tổng hợp kết luận", 12, muted)
        page.addView(stopInfo)
        page.addView(space(18))
        page.addView(label("DIỄN BIẾN TRANH LUẬN", 16, red, true))

        result = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        page.addView(result)
        addResult("SẴN SÀNG", "Nhập câu hỏi rồi nhấn BẮT ĐẦU TRANH LUẬN.")
    }

    private fun startDebate() {
        val q = question.text.toString().trim()
        if (q.length < 5) {
            Toast.makeText(this, "Bạn hãy nhập câu hỏi rõ ràng hơn.", Toast.LENGTH_SHORT).show()
            return
        }
        runButton.isEnabled = false
        runButton.text = "ĐANG TRANH LUẬN..."
        result.removeAllViews()
        addResult("ĐANG XỬ LÝ", "Hai AI đang phân tích câu hỏi. Quá trình có thể mất một lúc.")
        executor.execute {
            try {
                val conn = (URL(backendUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 30000
                    readTimeout = 180000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                conn.outputStream.use { os ->
                    OutputStreamWriter(os, Charsets.UTF_8).use {
                        it.write(JSONObject().put("question", q).toString())
                    }
                }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = stream.bufferedReader().use { it.readText() }
                conn.disconnect()
                if (code !in 200..299) throw Exception("Máy chủ trả lỗi HTTP $code: $body")
                val json = JSONObject(body)
                runOnUiThread {
                    result.removeAllViews()
                    addResult("AI A • LẬP LUẬN BAN ĐẦU", json.optString("a_initial"))
                    addResult("AI B • PHẢN BIỆN VÒNG 1", json.optString("b_critique_1"))
                    addResult("AI A • PHẢN HỒI", json.optString("a_response"))
                    addResult("AI B • PHẢN BIỆN VÒNG 2", json.optString("b_critique_2"))
                    addResult("KẾT LUẬN • AI TỔNG HỢP", json.optString("final_answer"))
                    runButton.isEnabled = true
                    runButton.text = "TRANH LUẬN LẠI"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    result.removeAllViews()
                    addResult("KHÔNG THỂ KẾT NỐI", e.message ?: "Lỗi không xác định")
                    addResult("KIỂM TRA", "Hãy kiểm tra URL backend HTTPS, kết nối Internet và trạng thái máy chủ.")
                    runButton.isEnabled = true
                    runButton.text = "THỬ LẠI"
                }
            }
        }
    }

    private fun addResult(title: String, body: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(13), dp(14), dp(13))
            setBackgroundColor(panel)
        }
        val heading = label(title, 13, red, true)
        val content = label(body.ifBlank { "Không có nội dung trả về." }, 14, white)
        content.setLineSpacing(dp(4).toFloat(), 1f)
        card.addView(heading)
        card.addView(space(7))
        card.addView(content)
        val params = LinearLayout.LayoutParams(-1, -2)
        params.bottomMargin = dp(10)
        result.addView(card, params)
    }

    private fun section(title: String, provider: String, color: Int): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(panel)
        }
        row.addView(label(title, 14, color, true))
        row.addView(label(provider, 12, muted))
        return row
    }

    private fun label(text: String, size: Int, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun space(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
