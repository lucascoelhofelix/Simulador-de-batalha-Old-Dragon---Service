// BattleEngine.kt
package com.example.battlesim

import kotlinx.coroutines.delay
import kotlin.math.max

/**
 * BattleEngine: controla uma batalha entre um personagem principal e uma lista de inimigos.
 * Este motor é síncrono por passo (turno) e foi feito para ser chamado dentro de uma coroutine.
 *
 * Observações:
 * - As regras são parametrizáveis. Ajuste attackHitCheck para seguir exatamente Old Dragon se necessário.
 * - Os métodos reportStatus/reportDeath são callbacks para o Service enviar notificações/broadcasts/UI updates.
 */
class BattleEngine(
    private val hero: Character,
    private val enemies: MutableList<Enemy>,
    private val onStatus: (String) -> Unit,
    private val onHeroDeath: (String) -> Unit,
    private val onBattleEnd: (String) -> Unit
) {
    var round: Int = 0
        private set

    private fun attackRoll(attackerBonus: Int, targetAC: Int): Pair<Boolean, Int> {
        val roll = Dice.rollD20()
        val total = roll + attackerBonus
        // Crits: 20 -> hit, 1 -> auto-miss (classic). Use as baseline.
        val isCritical = roll == 20
        val isFumble = roll == 1
        val hit = when {
            isCritical -> true
            isFumble -> false
            else -> total >= targetAC
        }
        return Pair(hit, if (isCritical) 2 else 1) // critical multiplier = 2
    }

    private fun doAttack(attackerName: String, attackerBonus: Int, damageDice: String, multiplier: Int, targetHpRef: () -> Int, applyDamage: (Int) -> Unit): String {
        val (hit, critMult) = attackRoll(attackerBonus, hero.armorClass) // note: targetAC will be passed by caller correctly
        if (!hit) return "$attackerName errou."

        val rawDamage = Dice.roll(damageDice)
        val damage = rawDamage * multiplier * critMult
        applyDamage(damage)
        return "$attackerName acerta e causa $damage dano."
    }

    private fun enemyTurn(enemy: Enemy): String {
        if (enemy.hp <= 0) return "${enemy.name} está fora de combate."
        val (hitRoll, mult) = attackRoll(enemy.attackBonus, hero.armorClass)
        if (!hitRoll) {
            return "${enemy.name} atacou e errou."
        }
        val damage = Dice.roll(enemy.damageDice) * mult
        hero.hp = max(0, hero.hp - damage)
        return "${enemy.name} atacou ${hero.name} e causou $damage dano. (${hero.hp}/${hero.maxHp} HP)"
    }

    private fun heroAttack(target: Enemy): String {
        if (hero.hp <= 0) return "${hero.name} está morto e não pode atacar."

        val (hitRoll, mult) = attackRoll(hero.attackBonus, target.armorClass)
        if (!hitRoll) return "${hero.name} atacou ${target.name} e errou."

        val damage = Dice.roll(hero.damageDice) * mult
        target.hp = max(0, target.hp - damage)
        return "${hero.name} acertou ${target.name} e causou $damage dano. (${target.hp}/${target.maxHp} HP)"
    }

    /**
     * Executa a batalha até um fim ou enquanto caller desejar.
     * - inclui delay entre turnos para não congestionar o CPU (use menor delay para acelerar).
     */
    suspend fun runBattle(turnDelayMs: Long = 1200L) {
        onStatus("Batalha iniciada: ${hero.name} vs ${enemies.joinToString { it.name }}")
        while (true) {
            round++
            onStatus("=== Round $round ===")
            // Hero turn: ataca o primeiro inimigo vivo
            val firstEnemy = enemies.firstOrNull { it.hp > 0 }
            if (firstEnemy != null) {
                val res = heroAttack(firstEnemy)
                onStatus(res)
                if (firstEnemy.hp <= 0) onStatus("${firstEnemy.name} foi derrotado!")
            }

            // Remove mortos
            val aliveEnemies = enemies.filter { it.hp > 0 }.toMutableList()
            enemies.clear()
            enemies.addAll(aliveEnemies)

            if (enemies.isEmpty()) {
                onBattleEnd("Todos os inimigos foram derrotados. Vitória!")
                break
            }

            // Enemies turn
            for (enemy in enemies.toList()) {
                val res = enemyTurn(enemy)
                onStatus(res)
                if (hero.hp <= 0) break
            }

            if (hero.hp <= 0) {
                onHeroDeath("${hero.name} morreu no round $round.")
                break
            }

            // Optionally small heal/regeneration or status effects can be processados aqui

            // small delay so it doesn't spin too fast
            delay(turnDelayMs)
        }
    }
}
