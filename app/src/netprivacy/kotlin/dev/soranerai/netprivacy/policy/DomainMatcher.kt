package dev.soranerai.netprivacy.policy

interface DomainMatcher {
    fun findRule(host: String, rules: List<TrustRule>): TrustRule?
}

object StrictDomainMatcher : DomainMatcher {
    override fun findRule(host: String, rules: List<TrustRule>): TrustRule? {
        val normalizedHost = DomainNormalizer.normalize(host) ?: return null
        return rules.asSequence()
            .filter { it.enabled }
            .mapNotNull { rule -> DomainNormalizer.normalize(rule.domain)?.let { domain -> rule to domain } }
            .filter { (rule, domain) ->
                normalizedHost == domain || (rule.includeSubdomains && normalizedHost.endsWith(".$domain"))
            }
            .maxByOrNull { (_, domain) -> domain.length }
            ?.first
    }

}
