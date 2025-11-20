// BattleService.kt
package com.example.battlesim

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class BattleService : Service() {
    private val svcScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var engineJob: Job? = null

    companion object {
        const val ACTION_START = "com.example.battlesim.action.START"
        const val ACTION_STOP = "com.example.battlesim.action.STOP"
        const val EXTRA_HERO = "extra_hero"
        const val EXTRA_ENEMIES = "extra_enemies"
        const val NOTIF_CHANNEL_ID = "battle_channel"
        const val NOTIF_ID_FOREGROUND = 1001
        const val NOTIF_ID_DEATH = 1002
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this, NOTIF_CHANNEL_ID, "Simulador de Batalha", "Canal para simulações")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> {
                // reconstruct sample hero/enemies if not provided (for testing)
                val hero = intent?.getSerializableExtra(EXTRA_HERO) as? Character ?: sampleHero()
                val enemies = intent?.getSerializableExtra(EXTRA_ENEMIES) as? ArrayList<Enemy> ?: arrayListOf(sampleEnemy1(), sampleEnemy2())
                startForegroundServiceWithNotification("Simulação iniciada: ${hero.name}")
                startBattle(hero, enemies.toMutableList())
            }
            ACTION_STOP -> {
                stopSelf()
            }
            else -> {
                // If started without action, start as demo
                val hero = sampleHero()
                val enemies = mutableListOf(sampleEnemy1(), sampleEnemy2())
                startForegroundServiceWithNotification("Simulação iniciada: ${hero.name}")
                startBattle(hero, enemies)
            }
        }
        // Keep service running until explicitly stopped.
        return START_STICKY
    }

    private fun startForegroundServiceWithNotification(text: String) {
        val notification = NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("Simulador de Batalha")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID_FOREGROUND, notification)
    }

    private fun startBattle(hero: Character, enemies: MutableList<Enemy>) {
        engineJob?.cancel()
        val engine = BattleEngine(
            hero,
            enemies,
            onStatus = { status -> updateForegroundNotification(status) },
            onHeroDeath = { msg -> notifyHeroDeath(msg) },
            onBattleEnd = { msg -> endBattle(msg) }
        )
        engineJob = svcScope.launch {
            try {
                engine.runBattle(800L) // delay entre turnos
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                // optional cleanup
            }
        }
    }

    private fun updateForegroundNotification(text: String) {
        val notification = NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("Simulação de Batalha")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setOngoing(true)
            .build()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID_FOREGROUND, notification)
    }

    private fun notifyHeroDeath(text: String) {
        // Notification to inform the user that hero died
        val intent = Intent(this, MainActivity::class.java)
        val pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notif = NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_delete)
            .setContentTitle("Personagem morto")
            .setContentText(text)
            .setContentIntent(pIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID_DEATH, notif)
        // stop the battle and service
        stopSelf()
    }

    private fun endBattle(msg: String) {
        updateForegroundNotification(msg)
        // optional: stop after a victory
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        engineJob?.cancel()
        svcScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ----- samples for quick testing
    private fun sampleHero(): Character {
        return Character(
            id = "hero1",
            name = "Aventureiro",
            hp = 12,
            maxHp = 12,
            attackBonus = 3,
            armorClass = 12,
            damageDice = "1d6+1"
        )
    }

    private fun sampleEnemy1(): Enemy {
        return Enemy(
            id = "gob1",
            name = "Goblin",
            hp = 6,
            maxHp = 6,
            attackBonus = 2,
            armorClass = 10,
            damageDice = "1d4"
        )
    }

    private fun sampleEnemy2(): Enemy {
        return Enemy(
            id = "rat1",
            name = "Rato Gigante",
            hp = 4,
            maxHp = 4,
            attackBonus = 1,
            armorClass = 9,
            damageDice = "1d3"
        )
    }
}
