### Added

- Added server-enforced downhill-only ziplines with horizontal-cable tolerance.
- Added protection against using exit jumps to reattach farther uphill on the same cable.
- Added a configurable maximum zipline speed.
- Backported jump-to-dismount behavior and the short attach buffer from newer versions.

### Changed

- Exit momentum now preserves the zipline's real travel direction instead of adding an artificial look-direction boost.
- Downhill-only rules also apply when switching between connected cables.
