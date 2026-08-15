# ChestLink

ChestLink 是一个纯服务端 Fabric 模组，可以通过命名频道连接多个原版箱子。同一频道的所有箱子共用 27 格库存，箱子界面、漏斗、投掷器和比较器都会使用其中的物品。

本文档也有 [English](README.md) 版本。

## Minecraft 版本

- 26.2

## 安装

从 [releases 页面](https://github.com/myl7/chestlink/releases)下载 JAR。

在服务器安装 Fabric Loader，再把 Fabric API 和 ChestLink JAR 放入服务器的 `mods` 目录。客户端无需安装 ChestLink 或 Fabric API。

单人游戏需要把这些文件安装到本地游戏实例。

## 指令

使用以下指令。执行 `link` 或 `unlink` 时，需要看向五格内的箱子。

```text
/chestlink link <频道名>
/chestlink unlink
/chestlink list
```

这些指令需要 2 级权限。

频道不存在时，`link` 会创建频道。第一个箱子中的物品会成为频道的初始物品。加入已有频道的箱子必须为空。

`unlink` 会把箱子移出频道。如果频道内还有其他箱子，物品会留在频道中，被移除的箱子会变为空箱。如果移除的是最后一个箱子，共享物品会回到该箱子，频道随后删除。

`list` 会显示每个频道，以及其中各个箱子的维度和坐标。

普通单箱和陷阱箱可以加入频道。带有未开启战利品表的箱子不能加入。破坏已连接的箱子时，物品处理规则与 `unlink` 相同。

频道可以跨维度连接箱子。只要当前箱子所在的区块已加载，即使频道内其他箱子所在的区块未加载，也可以使用共享物品。

## 卸载

如果需要保留频道中的物品，请在卸载 ChestLink 前解除所有连接。卸载模组后，Minecraft 不会显示仍留在频道中的物品。

## 限制

- 无论连接多少箱子，每个频道都只有 27 格。
- ChestLink 只支持单箱。不要在已连接箱子旁放置另一个箱子，Minecraft 可能把它们合并成不受支持的大箱子。
- 打开一个已连接箱子时，只有这个箱子会播放开盖动画，频道内其他箱子的盖子不会打开。
- 其他模组如果不通过破坏方块的方式移除箱子，该箱子可能继续留在频道列表中。

## 构建和测试

安装 JDK 25 后运行：

```bash
./gradlew runGameTest
./gradlew build
```

构建产物位于 `build/libs/`。

## 许可证

[Apache License 2.0](LICENSE)
