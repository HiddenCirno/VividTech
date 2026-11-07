# VividTech / 绚乱科技

GTCEu / GTLCore 附属模组，当前包含：

- 武德之眼（Eye of Wood）
- 梦想电梯（Dream Elevator）
- 蒸汽工业加工厂模块 T1/T2/T3

## 依赖

- Minecraft 1.20.1
- Forge 47.x
- GTCEu 1.4.4
- GTLCore 分支 `skyblock`（必须安装；提供蒸汽多方块、模块框架、轨道判定等基础设施）

## 开发环境

VividTech 直接依赖 GTLCore 的 **Loom dev jar**：

```groovy
modImplementation files("../GTLCore/build/devlibs/gtlcore-${gtlcore_dev_version}-dev.jar")
```

因此本地开发前需要先构建 GTLCore：

```bash
cd ../GTLCore
./gradlew compileJava jar
```

然后：

```bash
cd ../VividTech
./gradlew compileJava
./gradlew runClient
```

> GTLCore 的 dev runtime 还需要它自己的强制依赖（AE2、Ad Astra、gtmthings、mae2 等）。
> 如果只做编译检查，当前配置已足够。

## 注册约定

方块/物品继续通过 GTCEu 的 `GTRegistration.REGISTRATE` 注册，因此方块 ID 保持：

- `gtceu:eye_of_wood`
- `gtceu:dream_elevator`
- `gtceu:steam_industrial_processing_plant_t1/t2/t3`

创造标签页：**绚乱科技：蒸汽力量**（`gtceu:steam_power`），图标为武德之眼控制器。
