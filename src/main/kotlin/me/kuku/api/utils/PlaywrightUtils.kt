@file:Suppress("SpellCheckingInspection")

package me.kuku.api.utils

import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import me.kuku.api.utils.PlaywrightBrowser.*
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Component

object PlaywrightUtils {

    val playwright: Playwright by lazy {
        Playwright.create()
    }

    fun browser(playwrightBrowser: PlaywrightBrowser = Chromium, option: BrowserType.LaunchOptions.() -> Unit = {}): com.microsoft.playwright.Browser {
        val browserType = when (playwrightBrowser) {
            Chromium -> playwright.chromium()
            Firefox -> playwright.firefox()
            Webkit -> playwright.webkit()
        }
        return browserType.launch(BrowserType.LaunchOptions().apply(option))
    }

}

enum class PlaywrightBrowser {
    Chromium, Firefox, Webkit
}

fun com.microsoft.playwright.Browser.newPage(option: com.microsoft.playwright.Browser.NewPageOptions.() -> Unit): com.microsoft.playwright.Page {
    return newPage(com.microsoft.playwright.Browser.NewPageOptions().apply(option))
}

@Component
class ClearBrowser: DisposableBean {

    override fun destroy() {
        PlaywrightUtils.playwright.close()
    }

}
