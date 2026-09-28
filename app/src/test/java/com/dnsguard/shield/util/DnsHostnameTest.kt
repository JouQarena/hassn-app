package com.dnsguard.shield.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DnsHostnameTest {

    @Test
    fun `valid production hostnames are accepted`() {
        assertTrue(DnsHostname.isValid("family.adguard-dns.com"))
        assertTrue(DnsHostname.isValid("dns.adguard-dns.com"))
        assertTrue(DnsHostname.isValid("unfiltered.adguard-dns.com"))
        assertTrue(DnsHostname.isValid("one.one.one.one"))
        assertTrue(DnsHostname.isValid("security.cloudflare-dns.com"))
        assertTrue(DnsHostname.isValid("dns.quad9.net"))
        assertTrue(DnsHostname.isValid("dns.google"))
        assertTrue(DnsHostname.isValid("doh.opendns.com"))
    }

    @Test
    fun `whitespace and case are normalised before validation`() {
        assertTrue(DnsHostname.isValid("  Family.AdGuard-DNS.com  "))
        assertTrue(DnsHostname.normalize("  DOH.Example.COM ") == "doh.example.com")
    }

    @Test
    fun `empty and malformed inputs are rejected`() {
        assertFalse(DnsHostname.isValid(""))
        assertFalse(DnsHostname.isValid("   "))
        assertFalse(DnsHostname.isValid("localhost"))
        assertFalse(DnsHostname.isValid("1.2.3.4"))
        assertFalse(DnsHostname.isValid("192.168.1.1"))
        assertFalse(DnsHostname.isValid(".leadingdot.com"))
        assertFalse(DnsHostname.isValid("trailingdot.com."))
        assertFalse(DnsHostname.isValid("double..dot.com"))
        assertFalse(DnsHostname.isValid("-bad-tld.com"))
        assertFalse(DnsHostname.isValid("bad.-label.com"))
        assertFalse(DnsHostname.isValid("has space.com"))
        assertFalse(DnsHostname.isValid("under_score.com"))
        assertFalse(DnsHostname.isValid("http://insecure.example"))
        assertFalse(DnsHostname.isValid("no_tld"))
        assertFalse(DnsHostname.isValid("x".repeat(70) + ".com"))
        assertFalse(DnsHostname.isValid("a." + "b".repeat(63) + "." + "c".repeat(200)))
    }

    @Test
    fun `boundary lengths behave correctly`() {
        // 63-char label is legal, 64 is not.
        val label63 = "a".repeat(63)
        assertTrue(DnsHostname.isValid("$label63.com"))
        assertFalse(DnsHostname.isValid("${label63}a.com"))
    }
}
