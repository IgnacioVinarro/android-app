package com.ignaciovinarro.stayfocused.core.util

// Dinero en centavos de ARS (CLAUDE.md: Long en la unidad mínima, nunca decimales). Estas funciones
// solo convierten entre centavos y texto; el formato es el argentino: punto para los miles y coma
// para los decimales. Se hace a mano (no con NumberFormat) para que el resultado no dependa del
// idioma del teléfono.

private val MONEY_INPUT = Regex("""(-?)(\d{1,3}(?:\.\d{3})+|\d+)(?:,(\d{1,2}))?""")
private const val MAX_WHOLE_DIGITS = 12

// Para los campos de monto: deja pasar solo lo que puede formar parte de un monto (dígitos, punto y
// coma; el signo menos solo si `allowNegative`). No valida el formato completo: eso lo hace
// `parseArsToCents`.
fun filterMoneyInput(text: String, allowNegative: Boolean = false): String =
    text.filter { it.isDigit() || it == '.' || it == ',' || (allowNegative && it == '-') }

// Para mostrar: `$ 1.234,56`, y con signo adelante si es negativo: `-$ 1.234,56`.
fun formatArs(cents: Long): String {
    val negative = cents < 0
    val abs = if (negative) -cents else cents
    val grouped = (abs / 100).toString().reversed().chunked(3).joinToString(".").reversed()
    val decimals = (abs % 100).toString().padStart(2, '0')
    return "${if (negative) "-" else ""}$ $grouped,$decimals"
}

// Para llenar un campo de texto editable: sin puntos de miles, con coma decimal solo si hay
// centavos (`150050` → `1500,50`; `150000` → `1500`).
fun formatArsForInput(cents: Long): String {
    val negative = cents < 0
    val abs = if (negative) -cents else cents
    val decimals = abs % 100
    val text = if (decimals == 0L) "${abs / 100}" else "${abs / 100},${decimals.toString().padStart(2, '0')}"
    return if (negative) "-$text" else text
}

// Lo que escribió el usuario → centavos, o null si no es un monto válido. Acepta `1234`, `1234,5`,
// `1234,56` y `1.234,56` (el punto solo como separador de miles, en grupos de 3). Con
// `allowNegative` acepta un `-` adelante (saldos iniciales de una tarjeta, por ejemplo).
fun parseArsToCents(text: String, allowNegative: Boolean = false): Long? {
    val clean = text.trim().removePrefix("$").trim()
    val match = MONEY_INPUT.matchEntire(clean) ?: return null
    val (sign, whole, fraction) = match.destructured
    if (sign.isNotEmpty() && !allowNegative) return null
    val digits = whole.replace(".", "")
    if (digits.length > MAX_WHOLE_DIGITS) return null
    val cents = digits.toLong() * 100 + if (fraction.isEmpty()) 0 else fraction.padEnd(2, '0').toLong()
    return if (sign.isEmpty()) cents else -cents
}
