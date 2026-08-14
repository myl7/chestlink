# ChestLink

Server-only Fabric mod for Minecraft 26.2。用 `/chestlink` 命令把多个原版箱子链接到同一频道，同频道的箱子共享同一个 27 格物品栏。

- **原版客户端可直接加入**：不注册任何新方块、物品、方块实体或粒子，registry sync 不会踢人。粒子特效用原版粒子由服务端下发，客户端无需安装任何东西。
- **真共享而非同步**：同频道所有箱子读写的是同一个 `NonNullList<ItemStack>` 对象。GUI、漏斗、投掷器、比较器全部走同一个列表，不存在两份数据互相同步的一致性问题。
- **跨维度**：频道数据全局存储（`<world>/data/chestlink/links.dat`），一头区块未加载不影响另一头读写。

## 命令

权限等级 2（OP 或单人开启作弊）。所有命令作用于视线 5 格内指向的箱子。

| 命令 | 作用 |
| --- | --- |
| `/chestlink link <channel>` | 把指向的箱子链接到频道。频道不存在时创建，并以这个箱子的当前内容作为频道内容；频道已存在时要求箱子为空 |
| `/chestlink unlink` | 解除指向箱子的链接。非最后一个成员：箱子变空箱，物品保留在频道；最后一个成员：频道物品回到这个箱子，频道删除 |
| `/chestlink list` | 列出所有频道、成员数与各成员的维度坐标 |

链接限制：仅单箱（非大箱子）、无未开启的战利品表、未被链接过。陷阱箱同样可用。

破坏已链接的箱子等价于 unlink：非最后成员不掉落任何东西（物品还在频道里），最后成员掉落频道全部内容。

已链接的箱子每秒冒出 `REVERSE_PORTAL` 粒子作为提示，link/unlink 成功时有一次 `PORTAL` 爆发。

## 构建

```sh
./gradlew build
```

产物在 `build/libs/`。需要 Java 25。运行时依赖 Fabric API（Command API v2 与 Lifecycle Events v1 模块）。

## 实现方式

三个 mixin 挂在原版类上：

1. `ChestBlockEntity#getItems` 头部拦截（cancellable）：已链接则返回频道共享列表。`getItem` / `setItem` / `removeItem`、漏斗传输、`ChestMenu` slot、比较器输出最终都经过这一个口。`level == null`（区块反序列化阶段）与客户端侧直接走原逻辑。
2. `BlockEntity#setChanged()` 尾部：GUI 与漏斗的修改都以它收口。已链接则标记持久化数据 dirty，并刷新同频道其他已加载成员旁的比较器。
3. `LevelChunk#setBlockState` 中对 `BlockEntity#preRemoveSideEffects` 的调用点（`@WrapOperation`）：箱子被破坏时先执行 unlink 例程，之后原版掉落逻辑读到的就是箱子自己的字段——非最后成员为空、最后成员持有频道全部内容，掉落语义自然成立。

数据层是一个 `SavedData`（`SavedDataType` + Codec），挂在服务器全局 `SavedDataStorage` 上。刻意不拦截箱子自身的存读档：链接存在时箱子 NBT 里的内容是不被访问的死存储，卸载本 mod 后箱子退化为各自持有的快照，不会丢东西（频道权威内容始终在 `links.dat` 里）。

## 已知限制

- 只支持单箱。在已链接的单箱旁再放一个箱子会被原版合并成大箱，产生「54 格 GUI 里 27 格是共享的」的怪异状态——不要这么做。
- 箱盖动画与开箱人数按各自方块实体独立计算，纯视觉差异。
- 用 `/data` 直接改写已链接箱子的 `Items` 不会进入频道（写进的是死存储字段）。
- 极少数不经过 `LevelChunk#setBlockState` 的方块移除路径（如某些第三方 mod 的直接移除）不会触发自动 unlink。
