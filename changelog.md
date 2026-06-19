# Changelog

## 3.102.3

- Reset `mc` version counter to 1 at mc1.0.0
- Removed duplicate logger names

## 3.2.2

- Added a command to open the config screen
- Fixed automessages not saving on add/delete
- Disabled reconnecting when initial manual connection fails
- Expanded default list of negative condition keys

## 3.2.1

- Updated Russian translation (rfin0)
- Added Traditional Chinese translation (StarsShine11904)

## 3.0.0

- Re-enabled config screen

## 3.0.0-beta.1

- Updated to mc26.1
- Temporarily disabled config screen
- Mod versioning scheme is now `major.mc.minor`:
  - `major` is incremented on 'significant' feature changes, or breaking API changes (if
    applicable).
  - `mc` is never reset, and is incremented on every MC release, irrespective of whether a mod
    update was required.
  - `minor` is reset when `major` is changed, and is incremented on every update that does not
    change either of the previous two numbers.
