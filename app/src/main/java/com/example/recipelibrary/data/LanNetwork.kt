package com.example.recipelibrary.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.URL
import java.net.URLConnection

/**
 * Fait passer le trafic vers le PC de la maison par le Wi-Fi.
 *
 * Juste après la connexion au Wi-Fi, Android garde la 4G comme réseau par défaut le temps de
 * vérifier le Wi-Fi : sans ce choix explicite, le signal de réveil et les requêtes vers
 * 192.168.x.x partent en 4G et se perdent (constaté le 15/11 à 19:20).
 */
object LanNetwork {
    @Volatile
    private var connectivity: ConnectivityManager? = null

    fun init(context: Context) {
        if (connectivity == null) {
            connectivity = context.applicationContext.getSystemService(ConnectivityManager::class.java)
        }
    }

    /** Réseau Wi-Fi (ou Ethernet) connecté, ou null. */
    @Suppress("DEPRECATION")
    fun wifi(): Network? {
        val cm = connectivity ?: return null
        return runCatching {
            cm.allNetworks.firstOrNull { network ->
                val caps = cm.getNetworkCapabilities(network) ?: return@firstOrNull false
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            }
        }.getOrNull()
    }

    /** Vrai si le service a été initialisé (dans l'app) ; faux dans les tests JVM. */
    fun isAvailable(): Boolean = connectivity != null

    /** Adresse du réseau local (192.168.x.x, 10.x.x.x, 172.16-31.x.x) : à joindre par le Wi-Fi. */
    fun isLocalAddress(host: String): Boolean =
        runCatching { InetAddress.getByName(host).isSiteLocalAddress }.getOrDefault(false)

    /** Ouvre une connexion HTTP, par le Wi-Fi si l'adresse est sur le réseau local. */
    fun open(url: URL): URLConnection {
        val network = if (isLocalAddress(url.host)) wifi() else null
        return network?.openConnection(url) ?: url.openConnection()
    }

    /** Attache une socket UDP au Wi-Fi (sans effet si pas de Wi-Fi). */
    fun bind(socket: DatagramSocket) {
        runCatching { wifi()?.bindSocket(socket) }
    }
}
