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

然后在 VividTech 里生成 dev 用的 GTLCore 补丁 jar（移除与 AE2 15.4.11 不兼容的一个 GTLCore GUI mixin）：

```bash
cd ../VividTech
python tools/patch-gtlc-dev-jar.py   ../GTLCore/build/devlibs/gtlcore-1.2.3.3-fix2-dev.jar   dev/gtlcore-1.2.3.3-fix2-dev-patched.jar
./gradlew compileJava
./gradlew runClient
```

> `build.gradle` 会优先使用 `dev/` 下的补丁 jar，没有时回退到 GTLCore 原始 dev jar。
>
> 完整的 dev 客户端还需要一批 GTLCore 运行时依赖。项目已经通过 Maven/Modrinth
> 配置了 AE2、AE2WTlib、Ad Astra、gtmthings、mae2、FTB 等；其余本地 jar 放在
> `libs/`（已被 .gitignore 忽略），可按需从 GTL 整合包复制。
>
> JEI 暂未加入 dev runtime：GTCEu 1.4.4 的 JEI mixin 与 15.49.x 的
> `FluidHelper#getTooltip(List, ...)` 签名不兼容，会导致启动崩溃。

## 注册约定

方块/物品继续通过 GTCEu 的 `GTRegistration.REGISTRATE` 注册，因此方块 ID 保持：

- `gtceu:eye_of_wood`
- `gtceu:dream_elevator`
- `gtceu:steam_industrial_processing_plant_t1/t2/t3`

创造标签页：**绚乱科技：蒸汽力量**（`gtceu:steam_power`），图标为武德之眼控制器。
