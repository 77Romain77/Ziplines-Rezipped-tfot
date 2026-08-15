### Added

- Added server-enforced downhill-only ziplines with horizontal-cable tolerance.
- Added protection against using exit jumps to reattach farther uphill on the same cable.
- Added a configurable maximum zipline speed.
- Added configurable gravity strength and velocity retention for realistic physics.
- Added configurable automatic detachment at the end of a zipline when no valid continuation exists.
- Added collision-aware end detection so supports near cable endpoints can still trigger switching or automatic detachment.
- Added a configurable look-direction boost strength for boosted jump exits.
- Backported jump-to-dismount behavior and the short attach buffer from newer versions.

### Changed

- Maximum zipline speed is now configured in blocks per second. Existing configs using the previous blocks-per-tick value are migrated automatically.
- Exit momentum now preserves the zipline's real travel direction instead of adding an artificial look-direction boost.
- Downhill-only rules also apply when switching between connected cables.
- Auto-detach-at-end is handled directly in zipline logic instead of through a mixin.
