// Models.kt
package com.example.battlesim

import kotlin.random.Random

data class Character(
    val id: String,
    val name: String,
    var hp: Int,
    val maxHp: Int,
    val attackBonus: Int,   // bônus de ataque (ACERTOS)
    val armorClass: Int,    // Classe de Armadura (defesa)
    val damageDice: String  // ex: "1d6+2"
)

data class Enemy(
    val id: String,
    val name: String,
    var hp: Int,
    val maxHp: Int,
    val attackBonus: Int,
    val armorClass: Int,
    val damageDice: String
)

/**
 * Simple dice parser and roller: accepts strings like "1d6", "2d8+1", "1d10-1", "3d4+2"
 */
object Dice {
    private val diceRegex = Regex("""^\s*(\d+)d(\d+)\s*([+-]\s*\d+)?\s*$""")

    fun roll(diceExpr: String): Int {
        val m = diceRegex.matchEntire(diceExpr)
            ?: throw IllegalArgumentException("Formato de dado inválido: '$diceExpr'")

        val (countS, sidesS, modifierS) = m.destructured
        val count = countS.toInt()
        val sides = sidesS.toInt()
        val modifier = modifierS.replace(" ", "").toIntOrNull() ?: 0

        var total = 0
        repeat(count) {
            total += Random.nextInt(1, sides + 1)
        }
        return total + modifier
    }

    fun rollD20(): Int = Random.nextInt(1, 21)
}
