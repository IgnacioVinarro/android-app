package com.ignaciovinarro.stayfocused.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// Paleta de colores e íconos para tareas y, a futuro, cualquier otro evento que un módulo
// quiera dejar elegir al usuario (CLAUDE.md: colores e íconos son claves de texto, como
// constantes en core/ui, nunca hardcodeados en cada pantalla).
enum class TaskColorKey(val key: String, val color: Color) {
    BLUE("blue", Color(0xFF4A6FA5)),
    RED("red", Color(0xFFC62828)),
    ORANGE("orange", Color(0xFFEF6C00)),
    YELLOW("yellow", Color(0xFFF9A825)),
    GREEN("green", Color(0xFF2E7D32)),
    TEAL("teal", Color(0xFF00695C)),
    PURPLE("purple", Color(0xFF6A1B9A)),
    PINK("pink", Color(0xFFAD1457)),
    GRAY("gray", Color(0xFF546E7A))
}

enum class TaskIconKey(val key: String, val icon: ImageVector) {
    DEFAULT("task_default", Icons.Filled.CheckCircle),
    EVENT("task_event", Icons.Filled.Event),
    WORK("task_work", Icons.Filled.Work),
    IMPORTANT("task_important", Icons.Filled.Star),
    HOME("task_home", Icons.Filled.Home),
    STUDY("task_study", Icons.AutoMirrored.Filled.MenuBook),
    CALL("task_call", Icons.Filled.Call),
    HEALTH("task_health", Icons.Filled.LocalHospital)
}

fun colorForKey(key: String): Color =
    TaskColorKey.entries.firstOrNull { it.key == key }?.color ?: TaskColorKey.BLUE.color

// Íconos de categorías de Finanzas (claves "category_*", las mismas que siembra DefaultCategories).
enum class CategoryIconKey(val key: String, val icon: ImageVector) {
    FOOD("category_food", Icons.Filled.Restaurant),
    GROCERIES("category_groceries", Icons.Filled.ShoppingCart),
    TRANSPORT("category_transport", Icons.Filled.DirectionsBus),
    HOME("category_home", Icons.Filled.Home),
    HEALTH("category_health", Icons.Filled.MedicalServices),
    LEISURE("category_leisure", Icons.Filled.Movie),
    EDUCATION("category_education", Icons.Filled.School),
    CLOTHING("category_clothing", Icons.Filled.Checkroom),
    GIFT("category_gift", Icons.Filled.CardGiftcard),
    TRAVEL("category_travel", Icons.Filled.Flight),
    PETS("category_pets", Icons.Filled.Pets),
    SAVINGS("category_savings", Icons.Filled.Savings),
    SALARY("category_salary", Icons.Filled.Payments),
    OTHER("category_other", Icons.Filled.MoreHoriz)
}

// Resuelve la clave de ícono de cualquier ítem (tarea o categoría); si no la conoce, un círculo.
fun iconForKey(key: String): ImageVector =
    TaskIconKey.entries.firstOrNull { it.key == key }?.icon
        ?: CategoryIconKey.entries.firstOrNull { it.key == key }?.icon
        ?: Icons.Filled.Circle
