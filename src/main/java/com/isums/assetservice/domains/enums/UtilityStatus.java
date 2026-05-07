package com.isums.assetservice.domains.enums;

/**
 * Derived status for a single (house, utility) tile on the alerts
 * dashboard. Thresholds are expressed as a fraction of
 * {@code currentUsage / monthlyLimit}:
 *
 * <table>
 *   <tr><th>Status</th>     <th>Condition</th>                      <th>UI</th></tr>
 *   <tr><td>{@link #GOOD}</td>     <td>used &lt; 0.80 · limit</td>  <td>green</td></tr>
 *   <tr><td>{@link #WARNING}</td>  <td>0.80 ≤ used &lt; 1.00</td>    <td>amber</td></tr>
 *   <tr><td>{@link #CRITICAL}</td> <td>used ≥ limit</td>             <td>red</td></tr>
 *   <tr><td>{@link #NO_DATA}</td>  <td>no forecast or no threshold</td><td>grey</td></tr>
 * </table>
 *
 * <p>The 80% warn / 100% critical split mirrors the product SLO —
 * landlord wants advance notice early enough to call the tenant,
 * but not so early that the dashboard is flooded with amber
 * (→ alert fatigue).
 *
 * <p>{@link #NO_DATA} is intentionally a distinct tile state rather
 * than an error: if a newly-provisioned house has no sensor rollup
 * yet, or the landlord hasn't configured a monthly cap, we want to
 * <em>show</em> the house on the board (so the landlord knows they
 * need to act) rather than hide it.
 */
public enum UtilityStatus {
    GOOD,
    WARNING,
    CRITICAL,
    NO_DATA
}
