# ChestLink

Server-side Fabric mod that links vanilla chests into named channels. Every chest in a channel uses the same 27-slot inventory, including access through its GUI, hoppers, droppers, and comparators.

This document is also available in [中文](README.zh.md).

## Features

- Shared inventory. All members of a channel read and write the same item list rather than copying items between separate inventories.
- Cross-dimension links. A channel can contain chests from different dimensions, and an unloaded member does not prevent the other members from working.
- Vanilla-client compatible. The mod adds no blocks, items, block entities, or client-side assets. A vanilla client can join a dedicated server that runs ChestLink.
- Persistent channels. Channel contents and member positions are stored with the world.
- Vanilla automation support. Hoppers, droppers, container menus, and comparator output use the shared inventory.

## Versions

| Component | Version |
| --- | --- |
| Minecraft | 26.2 |
| Fabric Loader | 0.19.3 or newer |
| Fabric API | 0.156.0+26.2 |
| Fabric Loom | 1.17.17 |
| Gradle | 9.5.1 |
| Java | 25 or newer |

## Installation

Install Fabric Loader, Fabric API, and the ChestLink JAR in the server's `mods` directory. Clients do not need ChestLink or Fabric API. For single-player use, install them in the local game instance because the integrated server runs inside the client.

## Commands

Commands require permission level 2. Look at a chest within five blocks when running `link` or `unlink`.

```text
/chestlink link <channel>
/chestlink unlink
/chestlink list
```

`link` creates the channel when it does not exist and uses the chest's current contents as the initial shared inventory. Joining an existing channel requires an empty chest.

`unlink` removes the targeted chest. When other members remain, the items stay in the channel and the removed chest becomes empty. Removing the last member moves the channel inventory back into that chest and deletes the channel.

`list` shows every channel and the dimension and coordinates of each member.

Only single chests without an unopened loot table can be linked. A trapped chest is supported. Breaking a linked chest applies the same rules as `unlink`: a non-last member drops no channel items, while the last member drops the channel inventory.

## Storage

ChestLink stores channel data in `<world>/data/chestlink/links.dat`. A linked chest's private inventory is cleared, while its normal inventory methods are redirected to the channel. Removing the mod leaves the channel data file unused by vanilla, so unlink every chest before uninstalling if the items must remain accessible.

## Build and test

```bash
./gradlew test
./gradlew runGameTest
./gradlew build
```

The built JAR is written to `build/libs/`. JUnit covers logic that does not require a running world. Fabric GameTest covers shared inventories, mixins, and link lifecycle behavior on a test server. GitHub Actions runs both test suites and uploads the built JAR.

## Known limits

- Each channel has 27 slots, regardless of how many chests it contains.
- Double chests cannot be linked. Placing another chest next to an already linked chest can still make vanilla form a double chest and produce an unsupported 54-slot interface.
- Lid animation and open-count state remain local to each chest.
- `/data` writes to a linked chest edit its private storage, which is not the active channel inventory.
- A third-party mod that removes a chest without the normal block-removal path may bypass automatic unlinking.

## License

[Apache License 2.0](LICENSE)
