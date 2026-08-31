package dev.soranerai.netprivacy.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DomainMatcherTest {
    @Test fun `exact rule accepts only its normalized apex`() {
        val rule = rule("example.com", subdomains = false)
        assertEquals(rule, StrictDomainMatcher.findRule("EXAMPLE.COM.", listOf(rule)))
        assertNull(StrictDomainMatcher.findRule("www.example.com", listOf(rule)))
        assertNull(StrictDomainMatcher.findRule("notexample.com", listOf(rule)))
        assertNull(StrictDomainMatcher.findRule("example.com.evil.org", listOf(rule)))
    }

    @Test fun `subdomain rule observes label boundaries`() {
        val rule = rule("example.com", subdomains = true)
        assertEquals(rule, StrictDomainMatcher.findRule("example.com", listOf(rule)))
        assertEquals(rule, StrictDomainMatcher.findRule("api.foo.example.com", listOf(rule)))
        assertNull(StrictDomainMatcher.findRule("notexample.com", listOf(rule)))
        assertNull(StrictDomainMatcher.findRule("example.com.evil.org", listOf(rule)))
    }

    @Test fun `most specific rule wins and invalid IDN fails closed`() {
        val wildcard = rule("example.com", subdomains = true)
        val api = rule("api.example.com", subdomains = false)
        assertEquals(api, StrictDomainMatcher.findRule("api.example.com", listOf(wildcard, api)))
        assertNull(StrictDomainMatcher.findRule(" ", listOf(wildcard)))
        assertNull(StrictDomainMatcher.findRule("bad domain", listOf(wildcard)))
    }

    @Test fun `IDN matching uses ASCII normalization`() {
        val rule = rule("xn--bcher-kva.example", subdomains = true)
        assertEquals(rule, StrictDomainMatcher.findRule("bücher.example", listOf(rule)))
    }

    private fun rule(domain: String, subdomains: Boolean) = TrustRule(
        id = domain,
        enabled = true,
        domain = domain,
        includeSubdomains = subdomains,
        certificateId = "test-ca",
    )
}
