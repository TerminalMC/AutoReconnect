# Changelog

## 3.2.2

- Added a command to open the config screen
- Fixed automessages not saving on add/delete

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

## 2.0.1

- Fixed an issue with AutoMessage scheduling

## 2.0.0

- Now, AutoMessages are identified by either the Server IP, the Realm Name, or the World Name
- Now, AutoMessage identifiers can be parsed as regex patterns
- Now, AutoMessage delays are configured in decimal seconds
- Fixed the caret position in regex syntax-error tooltips
- Added a config option to enable command signing, fixing AutoMessage commands on certain servers
- Enabled pairing multiple AutoMessage objects to a single connection

## 1.0.4

- Added Simplified Chinese translation (Lithum-12)

## 1.0.3

- Added exception to prevent reconnecting when disconnected by transfer packet

## 1.0.2

- Fixed YACL dependency ID
- Fixed realms reconnection
- Fixed backup screen translation keys
- Added Russian translation (rfin0)

## 1.0.1

- Added Ukrainian translation (ttrafford7)
