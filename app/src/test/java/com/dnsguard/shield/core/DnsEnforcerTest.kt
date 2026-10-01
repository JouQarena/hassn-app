package com.dnsguard.shield.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DnsEnforcerTest {
    private val target = "family.adguard-dns.com"

    @Test fun `only matching mode and specifier are considered restored`() {
        assertTrue(DnsEnforcer.matches(DnsManager.DnsStatus("hostname", target), target))
        assertFalse(DnsEnforcer.matches(DnsManager.DnsStatus("off", target), target))
        assertFalse(DnsEnforcer.matches(DnsManager.DnsStatus("opportunistic", target), target))
        assertFalse(DnsEnforcer.matches(DnsManager.DnsStatus("hostname", "one.one.one.one"), target))
        assertFalse(DnsEnforcer.matches(DnsManager.DnsStatus("hostname", null), target))
    }
}
