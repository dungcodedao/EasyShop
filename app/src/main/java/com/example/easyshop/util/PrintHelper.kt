package com.example.easyshop.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Helper to handle printing in Android
 */
object PrintHelper {

    /**
     * Prints a simple HTML content as a PDF or to a real printer.
     * This is a robust way to print receipts as we can style them with CSS easily.
     */
    fun printHtml(context: Context, htmlContent: String, jobName: String = "EasyShop Receipt") {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                createPrintJob(view, jobName)
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun createPrintJob(webView: WebView, jobName: String) {
        val printManager = webView.context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val printAdapter = webView.createPrintDocumentAdapter(jobName)
        
        printManager.print(
            jobName,
            printAdapter,
            PrintAttributes.Builder().build()
        )
    }

    /**
     * Generates HTML Receipt from Order Data
     * This allows us to keep the logic of 'how it looks on paper' separate.
     */
    fun generateReceiptHtml(
        orderId: String, 
        total: String, 
        subtotal: String,
        discount: String,
        date: String, 
        itemsHtml: String
    ): String {
        return """
            <html>
                <style>
                    body { font-family: 'Courier New', Courier, monospace; padding: 20px; color: #333; }
                    .header { text-align: center; border-bottom: 2px dashed #ccc; padding-bottom: 10px; }
                    .brand { font-size: 24px; font-weight: bold; margin-bottom: 5px; }
                    .meta { margin: 15px 0; font-size: 14px; }
                    .table { width: 100%; border-collapse: collapse; margin: 20px 0; }
                    .table th { border-bottom: 1px dashed #ccc; text-align: left; padding: 5px; }
                    .table td { padding: 5px; font-size: 13px; }
                    .summary-row { font-size: 14px; }
                    .total-row { border-top: 2px dashed #ccc; font-weight: bold; font-size: 18px; }
                    .footer { text-align: center; margin-top: 30px; font-size: 12px; color: #888; }
                </style>
                <body>
                    <div class="header">
                        <div class="brand">EasyShop</div>
                        <div>Chuyên linh kiện PC cao cấp</div>
                        <div>Hotline: 1900 8888</div>
                    </div>
                    <div class="meta">
                        <div><b>Mã đơn hàng:</b> #$orderId</div>
                        <div><b>Ngày thanh toán:</b> $date</div>
                    </div>
                    <table class="table">
                        <thead>
                            <tr>
                                <th>Sản phẩm</th>
                                <th style="text-align:center">SL</th>
                                <th style="text-align:right">Thành tiền</th>
                            </tr>
                        </thead>
                        <tbody>
                            $itemsHtml
                        </tbody>
                        <tfoot>
                            <tr class="summary-row">
                                <td colspan="2">Tạm tính</td>
                                <td style="text-align:right">$subtotal</td>
                            </tr>
                            <tr class="summary-row">
                                <td colspan="2">Giảm giá</td>
                                <td style="text-align:right">-$discount</td>
                            </tr>
                            <tr class="total-row">
                                <td colspan="2">TỔNG CỘNG</td>
                                <td style="text-align:right">$total</td>
                            </tr>
                        </tfoot>
                    </table>
                    <div class="footer">
                        <p>Cảm ơn quý khách đã tin tưởng EasyShop!</p>
                        <p>Tra cứu bảo hành tại www.easyshop.com.vn</p>
                    </div>
                </body>
            </html>
        """.trimIndent()
    }
}
