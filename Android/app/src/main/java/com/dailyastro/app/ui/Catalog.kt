package com.dailyastro.app.ui

import androidx.annotation.DrawableRes
import com.dailyastro.app.R
import java.time.LocalDate
import java.time.MonthDay
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Offline catalog so onboarding and pickers work before the first network call. Mirrors backend/app/data.py. */
object Catalog {
    data class Lang(val code: String, val native: String, @DrawableRes val image: Int)
    data class Sign(val id: String, val en: String, val start: MonthDay, val end: MonthDay, val symbol: String, @DrawableRes val image: Int)

    val languages = listOf(
        Lang("en", "English", R.drawable.lang_en), Lang("hi", "हिन्दी", R.drawable.lang_hi), Lang("bn", "বাংলা", R.drawable.lang_bn),
        Lang("te", "తెలుగు", R.drawable.lang_te), Lang("mr", "मराठी", R.drawable.lang_mr), Lang("ta", "தமிழ்", R.drawable.lang_ta),
        Lang("gu", "ગુજરાતી", R.drawable.lang_gu), Lang("kn", "ಕನ್ನಡ", R.drawable.lang_kn), Lang("ml", "മലയാളം", R.drawable.lang_ml),
        Lang("pa", "ਪੰਜਾਬੀ", R.drawable.lang_pa),
    )

    val signs = listOf(
        Sign("aries", "Aries", MonthDay.of(3, 21), MonthDay.of(4, 19), "♈", R.drawable.sign_aries),
        Sign("taurus", "Taurus", MonthDay.of(4, 20), MonthDay.of(5, 20), "♉", R.drawable.sign_taurus),
        Sign("gemini", "Gemini", MonthDay.of(5, 21), MonthDay.of(6, 20), "♊", R.drawable.sign_gemini),
        Sign("cancer", "Cancer", MonthDay.of(6, 21), MonthDay.of(7, 22), "♋", R.drawable.sign_cancer),
        Sign("leo", "Leo", MonthDay.of(7, 23), MonthDay.of(8, 22), "♌", R.drawable.sign_leo),
        Sign("virgo", "Virgo", MonthDay.of(8, 23), MonthDay.of(9, 22), "♍", R.drawable.sign_virgo),
        Sign("libra", "Libra", MonthDay.of(9, 23), MonthDay.of(10, 22), "♎", R.drawable.sign_libra),
        Sign("scorpio", "Scorpio", MonthDay.of(10, 23), MonthDay.of(11, 21), "♏", R.drawable.sign_scorpio),
        Sign("sagittarius", "Sagittarius", MonthDay.of(11, 22), MonthDay.of(12, 21), "♐", R.drawable.sign_sagittarius),
        Sign("capricorn", "Capricorn", MonthDay.of(12, 22), MonthDay.of(1, 19), "♑", R.drawable.sign_capricorn),
        Sign("aquarius", "Aquarius", MonthDay.of(1, 20), MonthDay.of(2, 18), "♒", R.drawable.sign_aquarius),
        Sign("pisces", "Pisces", MonthDay.of(2, 19), MonthDay.of(3, 20), "♓", R.drawable.sign_pisces),
    )

    private val names = mapOf(
        "hi" to "मेष,वृषभ,मिथुन,कर्क,सिंह,कन्या,तुला,वृश्चिक,धनु,मकर,कुंभ,मीन",
        "bn" to "মেষ,বৃষ,মিথুন,কর্কট,সিংহ,কন্যা,তুলা,বৃশ্চিক,ধনু,মকর,কুম্ভ,মীন",
        "te" to "మేషం,వృషభం,మిథునం,కర్కాటకం,సింహం,కన్య,తుల,వృశ్చికం,ధనుస్సు,మకరం,కుంభం,మీనం",
        "mr" to "मेष,वृषभ,मिथुन,कर्क,सिंह,कन्या,तूळ,वृश्चिक,धनु,मकर,कुंभ,मीन",
        "ta" to "மேஷம்,ரிஷபம்,மிதுனம்,கடகம்,சிம்மம்,கன்னி,துலாம்,விருச்சிகம்,தனுசு,மகரம்,கும்பம்,மீனம்",
        "gu" to "મેષ,વૃષભ,મિથુન,કર્ક,સિંહ,કન્યા,તુલા,વૃશ્ચિક,ધન,મકર,કુંભ,મીન",
        "kn" to "ಮೇಷ,ವೃಷಭ,ಮಿಥುನ,ಕರ್ಕಾಟಕ,ಸಿಂಹ,ಕನ್ಯಾ,ತುಲಾ,ವೃಶ್ಚಿಕ,ಧನು,ಮಕರ,ಕುಂಭ,ಮೀನ",
        "ml" to "മേടം,ഇടവം,മിഥുനം,കർക്കടകം,ചിങ്ങം,കന്നി,തുലാം,വൃശ്ചികം,ധനു,മകരം,കുംഭം,മീനം",
        "pa" to "ਮੇਖ,ਵ੍ਰਿਸ਼ਭ,ਮਿਥੁਨ,ਕਰਕ,ਸਿੰਘ,ਕੰਨਿਆ,ਤੁਲਾ,ਵ੍ਰਿਸ਼ਚਿਕ,ਧਨੁ,ਮਕਰ,ਕੁੰਭ,ਮੀਨ",
    ).mapValues { it.value.split(",") }

    fun sign(id: String?) = signs.firstOrNull { it.id == id } ?: signs[0]
    fun lang(code: String) = languages.firstOrNull { it.code == code } ?: languages[0]
    fun name(id: String?, language: String): String { val i = signs.indexOf(sign(id)); return names[language]?.get(i) ?: signs[i].en }

    fun range(id: String?, language: String): String {
        val s = sign(id)
        val f = DateTimeFormatter.ofPattern("MMM d", Locale.forLanguageTag(language))
        return "${s.start.atYear(2024).format(f)} - ${s.end.atYear(2024).format(f)}"
    }

    /** Tropical sun sign for a birth date, matching the ranges above. */
    fun signFor(date: LocalDate): Sign {
        val md = MonthDay.from(date)
        return signs.first { s -> if (s.start.isBefore(s.end)) !md.isBefore(s.start) && !md.isAfter(s.end) else !md.isBefore(s.start) || !md.isAfter(s.end) }
    }

    @DrawableRes fun categoryIcon(key: String) = when (key) {
        "love" -> R.drawable.ic_cat_love; "career" -> R.drawable.ic_cat_career; "health" -> R.drawable.ic_cat_health
        "spiritual" -> R.drawable.ic_cat_spiritual; else -> R.drawable.ic_cat_lucky
    }
}
