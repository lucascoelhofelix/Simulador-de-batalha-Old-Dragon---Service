// Exemplo: MainActivity.kt (trecho)
val startIntent = Intent(this, BattleService::class.java).apply {
    action = BattleService.ACTION_START
    // Você pode serializar o personagem e inimigos se quiser enviar dados reais
    // startIntent.putExtra(BattleService.EXTRA_HERO, heroSerializable)
}
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    startForegroundService(startIntent)
} else {
    startService(startIntent)
}
