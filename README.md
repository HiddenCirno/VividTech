# VividTech / 绚乱科技

GTCEu / GTLCore 附属模组，当前包含：

- 武德之眼（Eye of Wood）
- 梦想电梯（Dream Elevator）
- 梦想电梯蒸汽加工模块：每种蒸汽加工类型（电路组装、锻造、压缩、研磨、合金冶炼、混合、离心、热力离心、电解、洗矿、化学浸洗、提取、熔炼、碎岩）各自独立的方块，均有 MK1/MK2/MK3 三档

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
- `gtceu:dream_elevator_steam_<type>_module_mk1/mk2/mk3`

创造标签页：**绚乱科技：蒸汽力量**（`gtceu:steam_power`），图标为武德之眼控制器。

## 梦想电梯机制

- 动力模块（铜/青金石/钻石/绿宝石/下界合金，T1–T5）决定全机并行池与生产速度：

| 等级 | 并行池 | 生产速度 |
|---|---|---|
| T1 | 256 | 100% |
| T2 | 384 | 150% |
| T3 | 512 | 200% |
| T4 | 768 | 250% |
| T5 | 1200 | 400% |

- 结构固定 24 个模块槽位，分内环 12 个、外环 12 个。
- 模块并行：MK1/MK2/MK3 = 8/16/32；T5 时内环 12 个模块并行翻倍（MK3 下额外 384）。
- 并行池全机共享，超出池容量的模块会等待。
- T5 动力模块把全机配方电压上限提升到 HV；大型蒸汽输入仓提升到 MV。
- 蒸汽乘区：`(0.8 / 动力模块等级) × 配方电压等级 + 生产模块等级 / 5`（ULV/LV 视为 1 级）。
