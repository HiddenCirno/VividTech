# VividTech / 绚乱科技

GTCEu / GTLCore 附属模组，当前包含：

- 武德之眼（Eye of Wood）
- 梦想电梯（Dream Elevator）
- 梦想电梯蒸汽加工模块：每种蒸汽加工类型（电路组装、锻造、压缩、研磨、合金冶炼、混合、离心、热力离心、洗矿、化学浸洗、提取、熔炼、碎岩）各自独立的方块，均有 MK-I/MK-II/MK-III 三档
- 梦想电梯蒸汽岩层开采模块：每 120/60/30 秒产出 64 个矿石方块，按当前维度矿脉权重抽取，无并行
- 梦想电梯蒸汽地层钻探模块：每 120/60/30 秒产出 64 桶石油/天然气，持续消耗 模块等级 × 1 mB/t 钻井液，无并行
- 梦想电梯蒸汽加压模块：4 个时配方电压上限 +1 级、全机工作速度 ×2、蒸汽消耗 ×2；16 个时 +2 级、速度 ×4、蒸汽消耗 ×4

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
- `gtceu:dream_elevator_steam_stratum_mining_module_mk1/mk2/mk3`
- `gtceu:dream_elevator_steam_stratum_drilling_module_mk1/mk2/mk3`

创造标签页：**绚乱科技：蒸汽力量**（`gtceu:steam_power`），图标为武德之眼控制器。

## 资源采集模块

### 蒸汽岩层开采模块

- 每 120 / 60 / 30 秒产出 64 个矿石方块（MK-I/II/III），按当前维度矿脉权重抽取
- MK-I：仅主世界；MK-II：主世界/下界；MK-III：主世界/下界/末地
- 在哪个维度就只抽哪个维度的矿脉，不会跨维度产出
- 优先输出矿石方块（主世界石矿/下界下界岩矿/末地末地石矿）；该材料没有对应矿石形态时依次退回粗矿、宝石、粉
- 无并行
- 输出仓/总线/总成装不下产物时自动暂停：不吞产物、不继续消耗蒸汽，腾出空间后自动恢复
- 蒸汽消耗：`模块等级 × 200 mB/t × [(0.8/轨道等级)×模块等级 + 模块等级/10]`；加压模块会在此基础上再乘蒸汽消耗倍率（4 个 ×2，16 个 ×4）

### 蒸汽地层钻探模块

- 每 120 / 60 / 30 秒产出 64 桶流体（MK-I/II/III）
- 主世界：石油；下界：天然气（MK-II 起可在下界工作）
- 持续消耗 模块等级 × 1 mB/t 钻井液（按 tick 扣除，缺料只暂停不清进度）
- 无并行
- 蒸汽消耗同上（含加压模块的蒸汽消耗倍率）

## 梦想电梯机制

- 动力模块（铜/青金石/钻石/绿宝石/下界合金，T1–T5）决定全机并行池与生产速度：

| 等级 | 并行池 | 生产速度 |
|---|---|---|
| T1 | 512 | 100% |
| T2 | 800 | 150% |
| T3 | 1024 | 200% |
| T4 | 1600 | 250% |
| T5 | 2400 | 400% |

- 模块槽位随动力模块等级解锁：T1–T5 = 6/12/15/18/24，按内环优先顺序启用。
- 模块并行：MK-I/MK-II/MK-III = 16/32/64；T5 时内环 12 个模块并行翻倍（MK-III 下额外 768）。
- 并行池全机共享，超出池容量的模块会等待。
- 主机和模块都不再保留蒸汽缓存，模块直接从梦想电梯结构内的蒸汽输入仓抽取蒸汽。
- 配方电压上限：T1/T2 = LV，T3 = MV，T4 = HV，T5 = EV。
- 直接使用 GTL 的大型蒸汽输入仓 `gtceu:large_steam_input_hatch`；梦想电梯单独禁用它的超频，蒸汽转换率固定 0.5。
- 额外提供创造模式蒸汽输入仓 `gtceu:creative_steam_input_hatch`：2,147,483,647 mB、无 GUI、仅接受蒸汽。
- 蒸汽输入仓最多 4 个（保持原有结构限制）。
- 蒸汽乘区：`(0.8 / 动力模块等级) × 配方电压等级 + 生产模块等级 / 10`（ULV = 0.5，LV = 1，MV = 2，HV = 3……）。机器不主动超频，始终按配方自身电压等级计算。
