package com.spinwin.rewards.data.model

import java.util.Locale

/**
 * Country and Currency configuration for global audience with US primary targeting.
 */
data class CountryInfo(
    val code: String,           // ISO 2-letter (e.g., "US", "GB", "IN")
    val name: String,           // Full country name
    val dialCode: String,       // Calling code (e.g., "+1", "+44", "+91")
    val flag: String,           // Emoji flag
    val currencyCode: String,   // Currency 3-letter code (e.g., "USD", "GBP", "INR")
    val currencySymbol: String, // Currency symbol (e.g., "$", "£", "₹")
    val pointsPerUnit: Int = 1000 // 1,000 points = 1.00 USD / GBP / EUR / 1.00 INR (100 pts = 10 paise = ₹0.10)
)

object CountryRegistry {
    val US = CountryInfo("US", "United States", "+1", "🇺🇸", "USD", "$", 1000)
    val GB = CountryInfo("GB", "United Kingdom", "+44", "🇬🇧", "GBP", "£", 1200)
    val CA = CountryInfo("CA", "Canada", "+1", "🇨🇦", "CAD", "C$", 1000)
    val AU = CountryInfo("AU", "Australia", "+61", "🇦🇺", "AUD", "A$", 1000)
    val DE = CountryInfo("DE", "Germany", "+49", "🇩🇪", "EUR", "€", 1100)
    val FR = CountryInfo("FR", "France", "+33", "🇫🇷", "EUR", "€", 1100)
    val AE = CountryInfo("AE", "United Arab Emirates", "+971", "🇦🇪", "AED", "AED", 300)
    val IN = CountryInfo("IN", "India", "+91", "🇮🇳", "INR", "₹", 1000)
    val SG = CountryInfo("SG", "Singapore", "+65", "🇸🇬", "SGD", "S$", 1000)
    val JP = CountryInfo("JP", "Japan", "+81", "🇯🇵", "JPY", "¥", 10)
    val BR = CountryInfo("BR", "Brazil", "+55", "🇧🇷", "BRL", "R$", 200)
    val MX = CountryInfo("MX", "Mexico", "+52", "🇲🇽", "MXN", "Mex$", 100)
    val NG = CountryInfo("NG", "Nigeria", "+234", "🇳🇬", "NGN", "₦", 1)
    val PH = CountryInfo("PH", "Philippines", "+63", "🇵🇭", "PHP", "₱", 20)
    val ZA = CountryInfo("ZA", "South Africa", "+27", "🇿🇦", "ZAR", "R", 60)
    val SA = CountryInfo("SA", "Saudi Arabia", "+966", "🇸🇦", "SAR", "SAR", 300)
    val ES = CountryInfo("ES", "Spain", "+34", "🇪🇸", "EUR", "€", 1100)
    val IT = CountryInfo("IT", "Italy", "+39", "🇮🇹", "EUR", "€", 1100)
    val NL = CountryInfo("NL", "Netherlands", "+31", "🇳🇱", "EUR", "€", 1100)
    val SE = CountryInfo("SE", "Sweden", "+46", "🇸🇪", "SEK", "kr", 100)
    val CH = CountryInfo("CH", "Switzerland", "+41", "🇨🇭", "CHF", "CHF", 1200)
    val NZ = CountryInfo("NZ", "New Zealand", "+64", "🇳🇿", "NZD", "NZ$", 1000)
    val IE = CountryInfo("IE", "Ireland", "+353", "🇮🇪", "EUR", "€", 1100)
    val MY = CountryInfo("MY", "Malaysia", "+60", "🇲🇾", "MYR", "RM", 250)
    val ID = CountryInfo("ID", "Indonesia", "+62", "🇮🇩", "IDR", "Rp", 1)
    val VN = CountryInfo("VN", "Vietnam", "+84", "🇻🇳", "VND", "₫", 1)
    val PK = CountryInfo("PK", "Pakistan", "+92", "🇵🇰", "PKR", "Rs", 10)
    val BD = CountryInfo("BD", "Bangladesh", "+880", "🇧🇩", "BDT", "৳", 10)
    val TR = CountryInfo("TR", "Turkey", "+90", "🇹🇷", "TRY", "₺", 30)
    val EG = CountryInfo("EG", "Egypt", "+20", "🇪🇬", "EGP", "E£", 20)
    val KE = CountryInfo("KE", "Kenya", "+254", "🇰🇪", "KES", "KSh", 10)
    val GH = CountryInfo("GH", "Ghana", "+233", "🇬🇭", "GHS", "GH₵", 70)
    val AR = CountryInfo("AR", "Argentina", "+54", "🇦🇷", "ARS", "$", 1)
    val CO = CountryInfo("CO", "Colombia", "+57", "🇨🇴", "COP", "$", 1)
    val CL = CountryInfo("CL", "Chile", "+56", "🇨🇱", "CLP", "$", 1)

    val ALL_COUNTRIES: List<CountryInfo> = listOf(
        US, GB, CA, AU, DE, FR, AE, IN, SG, JP,
        BR, MX, NG, PH, ZA, SA, ES, IT, NL, SE,
        CH, NZ, IE, MY, ID, VN, PK, BD, TR, EG,
        KE, GH, AR, CO, CL
    )

    val DEFAULT = US

    fun detectDefaultCountry(): CountryInfo {
        return try {
            val iso = Locale.getDefault().country.uppercase()
            ALL_COUNTRIES.firstOrNull { it.code == iso } ?: US
        } catch (_: Exception) {
            US
        }
    }

    fun findByCode(code: String): CountryInfo {
        return ALL_COUNTRIES.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: US
    }

    fun format(amount: Double, country: CountryInfo = DEFAULT): String {
        return "${country.currencySymbol}${String.format(Locale.US, "%.2f", amount)}"
    }
}
