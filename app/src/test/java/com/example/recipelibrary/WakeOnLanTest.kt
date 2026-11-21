package com.example.recipelibrary

import com.example.recipelibrary.data.WakeOnLan
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class WakeOnLanTest {

    private val mac = byteArrayOf(0x04, 0x33, 0xC2.toByte(), 0xB9.toByte(), 0x44, 0x3E)

    @Test
    fun parseMac_acceptsColonsAndDashes() {
        assertArrayEquals(mac, WakeOnLan.parseMac("04:33:C2:B9:44:3E"))
        assertArrayEquals(mac, WakeOnLan.parseMac(" 04-33-c2-b9-44-3e "))
    }

    @Test
    fun parseMac_rejectsInvalidInput() {
        assertNull(WakeOnLan.parseMac(""))
        assertNull(WakeOnLan.parseMac("04:33:C2:B9:44"))
        assertNull(WakeOnLan.parseMac("04:33:C2:B9:44:3E:00"))
        assertNull(WakeOnLan.parseMac("04:33:C2:B9:44:ZZ"))
        assertNull(WakeOnLan.parseMac("0433C2B9443E"))
    }

    @Test
    fun buildMagicPacket_isSixFFThenMacSixteenTimes() {
        val packet = WakeOnLan.buildMagicPacket(mac)
        assertEquals(102, packet.size)
        assertArrayEquals(ByteArray(6) { 0xFF.toByte() }, packet.copyOfRange(0, 6))
        for (i in 0 until 16) {
            assertArrayEquals("répétition $i", mac, packet.copyOfRange(6 + i * 6, 12 + i * 6))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun buildMagicPacket_rejectsWrongLength() {
        WakeOnLan.buildMagicPacket(byteArrayOf(1, 2, 3))
    }

    @Test
    fun targetsFor_ipv4_includesHostSubnetBroadcastAndGlobalBroadcast() {
        assertEquals(
            listOf("192.168.1.44", "192.168.1.255", "255.255.255.255"),
            WakeOnLan.targetsFor("192.168.1.44")
        )
    }

    @Test
    fun targetsFor_hostname_fallsBackToGlobalBroadcast() {
        assertEquals(listOf("255.255.255.255"), WakeOnLan.targetsFor("mon-pc.local"))
        assertEquals(listOf("255.255.255.255"), WakeOnLan.targetsFor("192.168.1.999"))
    }

    @Test
    fun send_invalidMac_returnsFalse() {
        assertFalse(WakeOnLan.send("pas-une-mac", "127.0.0.1"))
    }

    @Test
    fun send_deliversMagicPacketOnPort9() {
        // Le port 9 peut être indisponible sur la machine de test : dans ce cas on ne vérifie que l'envoi.
        val listener = runCatching { DatagramSocket(9, InetAddress.getByName("127.0.0.1")) }.getOrNull()
        try {
            assertTrue(WakeOnLan.send("04:33:C2:B9:44:3E", "127.0.0.1"))
            if (listener != null) {
                listener.soTimeout = 3000
                val buffer = DatagramPacket(ByteArray(200), 200)
                listener.receive(buffer)
                assertArrayEquals(WakeOnLan.buildMagicPacket(mac), buffer.data.copyOf(buffer.length))
            }
        } finally {
            listener?.close()
        }
    }
}
