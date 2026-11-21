package com.example.recipelibrary.data

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Réveil du PC Hermes par « paquet magique » Wake-on-LAN.
 * Ne fonctionne que sur le même réseau local que le PC, qui doit être en veille (pas éteint).
 */
object WakeOnLan {

    private const val PORT = 9
    private val MAC_REGEX = Regex("^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$")

    /** Convertit "04:33:C2:B9:44:3E" (ou avec des tirets) en 6 octets ; null si le format est invalide. */
    fun parseMac(mac: String): ByteArray? {
        val trimmed = mac.trim()
        if (!MAC_REGEX.matches(trimmed)) return null
        return trimmed.split(':', '-').map { it.toInt(16).toByte() }.toByteArray()
    }

    /** Paquet magique : 6 octets 0xFF suivis de 16 répétitions de l'adresse MAC (102 octets). */
    fun buildMagicPacket(mac: ByteArray): ByteArray {
        require(mac.size == 6) { "Une adresse MAC fait 6 octets" }
        val packet = ByteArray(6 + 16 * 6) { 0xFF.toByte() }
        for (i in 0 until 16) mac.copyInto(packet, 6 + i * 6)
        return packet
    }

    /**
     * Adresses vers lesquelles envoyer le paquet pour un serveur donné :
     * l'IP connue du PC, la diffusion de son sous-réseau /24 et la diffusion générale.
     */
    fun targetsFor(serverHost: String): List<String> {
        val parts = serverHost.split('.')
        val isIpv4 = parts.size == 4 && parts.all { p -> p.toIntOrNull()?.let { it in 0..255 } == true }
        return if (isIpv4) {
            listOf(serverHost, "${parts[0]}.${parts[1]}.${parts[2]}.255", "255.255.255.255").distinct()
        } else {
            listOf("255.255.255.255")
        }
    }

    /**
     * Envoie le paquet magique (appel bloquant). [bind] permet d'attacher la socket au Wi-Fi.
     * Retourne false si l'adresse MAC est invalide ou si rien n'a pu partir. */
    fun send(mac: String, serverHost: String, bind: (DatagramSocket) -> Unit = {}): Boolean {
        val packet = buildMagicPacket(parseMac(mac) ?: return false)
        var sent = false
        runCatching {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                bind(socket)
                for (target in targetsFor(serverHost)) {
                    runCatching {
                        socket.send(DatagramPacket(packet, packet.size, InetAddress.getByName(target), PORT))
                        sent = true
                    }
                }
            }
        }
        return sent
    }
}
