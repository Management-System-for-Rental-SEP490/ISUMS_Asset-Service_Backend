package com.isums.assetservice.domains.enums;

/**
 * Discriminator for rows in {@code asset_event_images}.
 *
 * <p>One maintenance event typically produces TWO sets of photos:
 * <ul>
 *   <li>{@link #BEFORE} — snapshot of the asset's images captured at
 *       the moment the event was created (what it looked like when the
 *       technician arrived).</li>
 *   <li>{@link #AFTER} — photos uploaded <em>after</em> the technician
 *       finished the work (proof of completion).</li>
 * </ul>
 *
 * <p>The previous schema stored both kinds in the same column without a
 * discriminator, which made it impossible to render a "before / after"
 * comparison view without joining heuristically against the preceding
 * event. Adding this enum + the {@code type} column on
 * {@code AssetEventImage} lets the read side return them as two
 * separate arrays cheaply.
 */
public enum AssetEventImageType {
    BEFORE,
    AFTER
}
