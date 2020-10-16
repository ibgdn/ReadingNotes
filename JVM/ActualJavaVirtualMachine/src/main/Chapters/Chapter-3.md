### 3.1 一切运行都有迹可循：掌握跟踪调试参数
#### 3.1.1 跟踪垃圾回收——读懂虚拟机日志
- PrintGC
  使用 GC 参数`-XX:+PrintGC`，遇到 GC 就会打印日志。
  ```
  [GC 4793K->377K(15872KK), 0.0006926 secs]
  [GC 4857K->377K(15936KK), 0.0003595 secs]
  [GC 4857K->377K(15936KK), 0.0001755 secs]
  [GC 4857K->377K(15936KK), 0.0001957 secs]
  ```
  日志显示，一共进行了4次 GC，每次一行。GC 前堆空间使用量约为4MB；GC 后，堆空间使用量为377KB，当前可用的堆空间总和约为16MB（15936KB）。最后显示 GC 花费的时间。

- PrintGCDetails
  如果需要更加详细的信息，添加参数`-XX:+PrintGCDetails`。
  ```
  [GC (Metadata GC Threshold) [PSYoungGen: 10529K->1409K(153088K)] 10529K->1417K(502784K), 0.0039326 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
  [Full GC (Metadata GC Threshold) [PSYoungGen: 1409K->0K(153088K)] [ParOldGen: 8K->1186K(133120K)] 1417K->1186K(286208K), [Metaspace: 3699K->3699K(1056768K)], 0.0194219 secs] [Times: user=0.02 sys=0.00, real=0.02 secs]
  [GC (Metadata GC Threshold) [PSYoungGen: 55265K->2172K(153088K)] 56452K->3366K(286208K), 0.0024525 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
  [Full GC (Metadata GC Threshold) [PSYoungGen: 2172K->0K(153088K)] [ParOldGen: 1194K->2755K(134656K)] 3366K->2755K(287744K), [Metaspace: 7443K->7443K(1056768K)], 0.0121722 secs] [Times: user=0.09 sys=0.00, real=0.01 secs]
  [GC (Metadata GC Threshold) [PSYoungGen: 34212K->1088K(153088K)] 36967K->3851K(287744K), 0.0012193 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
  [Full GC (Metadata GC Threshold) [PSYoungGen: 1088K->0K(153088K)] [ParOldGen: 2763K->3065K(210432K)] 3851K->3065K(363520K), [Metaspace: 9159K->9159K(1058816K)], 0.0321379 secs] [Times: user=0.08 sys=0.00, real=0.03 secs]
  [GC (Last ditch collection) [PSYoungGen: 0K->0K(154624K)] 3065K->3065K(365056K), 0.0005435 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
  [Full GC (Last ditch collection) [PSYoungGen: 0K->0K(154624K)] [ParOldGen: 3065K->1347K(330752K)] 3065K->1347K(485376K), [Metaspace: 9159K->9159K(1058816K)], 0.0174909 secs] [Times: user=0.19 sys=0.00, real=0.02 secs]
  [GC (Metadata GC Threshold) [PSYoungGen: 2938K->64K(168448K)] 4285K->1411K(499200K), 0.0006535 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
  [Full GC (Metadata GC Threshold) [PSYoungGen: 64K->0K(168448K)] [ParOldGen: 1347K->1341K(459776K)] 1411K->1341K(628224K), [Metaspace: 9159K->9159K(1058816K)], 0.0137784 secs] [Times: user=0.06 sys=0.00, real=0.01 secs]
  [GC (Last ditch collection) [PSYoungGen: 0K->0K(153088K)] 1341K->1341K(612864K), 0.0004620 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
  [Full GC (Last ditch collection) [PSYoungGen: 0K->0K(153088K)] [ParOldGen: 1341K->1341K(637952K)] 1341K->1341K(791040K), [Metaspace: 9159K->9159K(1058816K)], 0.0080811 secs] [Times: user=0.00 sys=0.00, real=0.01 secs]

  Heap
   PSYoungGen      total 153088K, used 4577K [0x0000000755a00000, 0x0000000760a00000, 0x00000007ffe00000)
    eden space 152576K, 3% used [0x0000000755a00000,0x0000000755e786b0,0x000000075ef00000)
    from space 512K, 0% used [0x0000000760980000,0x0000000760980000,0x0000000760a00000)
    to   space 3072K, 0% used [0x0000000760400000,0x0000000760400000,0x0000000760700000)
   ParOldGen       total 637952K, used 1341K [0x0000000601200000, 0x0000000628100000, 0x0000000755a00000)
    object space 637952K, 0% used [0x0000000601200000,0x000000060134f6b0,0x0000000628100000)
   Metaspace       used 9191K, capacity 10070K, committed 10240K, reserved 1058816K
    class space    used 879K, capacity 905K, committed 1024K, reserved 1048576K
  ```

  系统经历了多次 GC，第一次是元数据 GC，回收效果是新生代从回收前的10M左右降低到1MB，整个堆从10M左右降低到1MB左右：
  ```
  [GC (Metadata GC Threshold) [PSYoungGen: 10529K->1409K(153088K)] 10529K->1417K(502784K), 0.0039326 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
  ```

  第二次是 Full GC，回收新生代、老年代、元数据，新生代被清空（不同 JDK 版本也有可能出现`1409K->1409K`，也是被清空的意思），老年代从8KB增长为1.1MB，整个堆从1.4M左右降低到1.1MB左右。日志最后，显示 GC 所花费的时间， user 表示用户态 CPU 耗时， sys 表示系统 CPU 耗时， real 表示 GC 实际经历的时间。
  ```
  [Full GC (Metadata GC Threshold) [PSYoungGen: 1409K->0K(153088K)] [ParOldGen: 8K->1186K(133120K)] 1417K->1186K(286208K), [Metaspace: 3699K->3699K(1056768K)], 0.0194219 secs] [Times: user=0.02 sys=0.00, real=0.02 secs]
  ```

  虚拟机退出前还会打印 heap 的详细信息，记录各个区的使用情况。新生代总大小153088KB，已使用4577KB。之后的3个16进制数字表示新生代的下界、当前上界和上界。
  使用上界减去下界就能得到当前堆空间的最大值，使用当前上界减去下界就是当前虚拟机已经为程序分配的空间大小。
  如果当前上界等于下界，说明当前的堆空间已经没有扩大的可能。(0x0000000760a00000 - 0x0000000755a00000) / 1024 = 0x000000000B000000 / 1024 = 184549376 / 1024 = 180224KB（eden + from + to），153088KB（eden + from）。
  ```
  Heap
   PSYoungGen      total 153088K, used 4577K [0x0000000755a00000, 0x0000000760a00000, 0x00000007ffe00000)
    eden space 152576K, 3% used [0x0000000755a00000,0x0000000755e786b0,0x000000075ef00000)
    from space 512K, 0% used [0x0000000760980000,0x0000000760980000,0x0000000760a00000)
    to   space 3072K, 0% used [0x0000000760400000,0x0000000760400000,0x0000000760700000)
   ParOldGen       total 637952K, used 1341K [0x0000000601200000, 0x0000000628100000, 0x0000000755a00000)
    object space 637952K, 0% used [0x0000000601200000,0x000000060134f6b0,0x0000000628100000)
   Metaspace       used 9191K, capacity 10070K, committed 10240K, reserved 1058816K
    class space    used 879K, capacity 905K, committed 1024K, reserved 1048576K
  ```

- PrintHeapAtGC
  添加参数`-XX:+PrintHeapAtGC`，会在每次 GC 前后分别打印 Heap 的信息。使用这个参数，可以很好地观察 GC 对 Heap 空间的影响。

- PrintGCTimeStamps
  使用`-XX:+PrintGCTimeStamps`参数，在每次 GC 时会额外输出 GC 发生的时间，该输出时间是虚拟机启动后的时间偏移量。

- PrintGCApplicationConcurrentTime\PrintCGApplicationStoppedTime
  由于 GC 会引起应用程序停顿，可能需要特别关注应用程序的执行时间和停顿时间。使用参数`-XX:+PrintGCApplicationConcurrentTime`可以打印应用程序的执行时间。

  使用参数`-XX:+PrintCGApplicationStoppedTime`可以打印应用程序由于 GC 而产生的停顿时间。

- PrintReferenceGC
  跟踪系统内的强引用、软引用、弱引用、虚引用可以使用参数`-XX:PrintReferenceGC`。

- Xloggc
  默认情况下，GC 的日志会在控制台中输出，不便于后续分析和定位问题。添加`-Xloggc:log/gc.log`参数，虚拟机允许将 GC 日志以文件的形式输出。

#### 3.1.2 类加载/卸载的跟踪
  一般情况下，系统加载的类存在于文件系统中，以 *.jar 文件的形式打包或者以 *.class 文件的形式存在，可以直接通过文件系统查看。

  随着动态代理、AOP 等技术的普遍使用，系统也极有可能在运行时动态生成某些类，这些类相对隐蔽，无法通过文件系统找到，虚拟机提供的类加载/卸载跟踪参数就显得格外有意义。

  使用参数`-verbose:class`跟踪类的加载和卸载，也可以单独使用参数`-XX:+TraceClassLoading`跟踪类的加载，`-XX:TraceClassUnloading`跟踪类的卸载，两个参数等价。

#### 3.1.3 系统参数查看
- PrintVMOptions
  不同的系统参数可能对系统的执行效果有较大的影响，有必要明确当前系统的实际运行参数。添加参数`-XX:+PrintVMOptions`可以在程序运行时，打印虚拟机接受到的命令行显示参数。
  ```
  [GC (Metadata GC Threshold) VM option '+PrintGCDetails'
  VM option 'MetaspaceSize=5m'
  VM option 'MaxMetaspaceSize=10m'
  VM option '+PrintVMOptions'
  ...
  ```

- PrintCommandLineFlags
  参数`-XX:+PrintCommandLineFlags`可以打印传递给虚拟机的显示和隐式参数，隐式参数未必是通过命令行直接给出的，可能是由虚拟机启动时自行设置的。
  ```
  -XX:CompressedClassSpaceSize=2097152 -XX:InitialHeapSize=535498368 -XX:MaxHeapSize=8567973888 -XX:MaxMetaspaceSize=10485760 -XX:MetaspaceSize=5242880 -XX:+PrintCommandLineFlags -XX:+PrintGCDetails -XX:+UseCompressedClassPointers -XX:+UseCompressedOops -XX:-UseLargePagesIndividualAllocation -XX:+UseParallelGC 
  ```

- PrintFlagsFinal
  参数`-XX:+PrintFlagsFinal`会打印所有的系统参数值。
  ```
  [Global flags]
     intx ActiveProcessorCount                      = -1                                  {product}
    uintx AdaptiveSizeDecrementScaleFactor          = 4                                   {product}
    uintx AdaptiveSizeMajorGCDecayTimeScale         = 10                                  {product}
    uintx AdaptiveSizePausePolicy                   = 0                                   {product}
    uintx AdaptiveSizePolicyCollectionCostMargin    = 50                                  {product}
    uintx AdaptiveSizePolicyInitializingSteps       = 20                                  {product}
    uintx AdaptiveSizePolicyOutputInterval          = 0                                   {product}
    uintx AdaptiveSizePolicyWeight                  = 10                                  {product}
    uintx AdaptiveSizeThroughPutPolicy              = 0                                   {product}
    uintx AdaptiveTimeWeight                        = 25                                  {product}
     bool AdjustConcurrency                         = false                               {product}
     bool AggressiveHeap                            = false                               {product}
     bool AggressiveOpts                            = false                               {product}
     intx AliasLevel                                = 3                                   {C2 product}
     bool AlignVector                               = false                               {C2 product}
     intx AllocateInstancePrefetchLines             = 1                                   {product}
     intx AllocatePrefetchDistance                  = 192                                 {product}
     intx AllocatePrefetchInstr                     = 0                                   {product}
     intx AllocatePrefetchLines                     = 4                                   {product}
     intx AllocatePrefetchStepSize                  = 64                                  {product}
     intx AllocatePrefetchStyle                     = 1                                   {product}
     bool AllowJNIEnvProxy                          = false                               {product}
     bool AllowNonVirtualCalls                      = false                               {product}
     bool AllowParallelDefineClass                  = false                               {product}
     bool AllowUserSignalHandlers                   = false                               {product}
     bool AlwaysActAsServerClassMachine             = false                               {product}
     bool AlwaysCompileLoopMethods                  = false                               {product}
     bool AlwaysLockClassLoader                     = false                               {product}
     bool AlwaysPreTouch                            = false                               {product}
     bool AlwaysRestoreFPU                          = false                               {product}
     bool AlwaysTenure                              = false                               {product}
     bool AssertOnSuspendWaitFailure                = false                               {product}
     bool AssumeMP                                  = false                               {product}
     intx AutoBoxCacheMax                           = 128                                 {C2 product}
    uintx AutoGCSelectPauseMillis                   = 5000                                {product}
     intx BCEATraceLevel                            = 0                                   {product}
     intx BackEdgeThreshold                         = 100000                              {pd product}
     bool BackgroundCompilation                     = true                                {pd product}
    uintx BaseFootPrintEstimate                     = 268435456                           {product}
     intx BiasedLockingBulkRebiasThreshold          = 20                                  {product}
     intx BiasedLockingBulkRevokeThreshold          = 40                                  {product}
     intx BiasedLockingDecayTime                    = 25000                               {product}
     intx BiasedLockingStartupDelay                 = 4000                                {product}
     bool BindGCTaskThreadsToCPUs                   = false                               {product}
     bool BlockLayoutByFrequency                    = true                                {C2 product}
     intx BlockLayoutMinDiamondPercentage           = 20                                  {C2 product}
     bool BlockLayoutRotateLoops                    = true                                {C2 product}
     bool BranchOnRegister                          = false                               {C2 product}
     bool BytecodeVerificationLocal                 = false                               {product}
     bool BytecodeVerificationRemote                = true                                {product}
     bool C1OptimizeVirtualCallProfiling            = true                                {C1 product}
     bool C1ProfileBranches                         = true                                {C1 product}
     bool C1ProfileCalls                            = true                                {C1 product}
     bool C1ProfileCheckcasts                       = true                                {C1 product}
     bool C1ProfileInlinedCalls                     = true                                {C1 product}
     bool C1ProfileVirtualCalls                     = true                                {C1 product}
     bool C1UpdateMethodData                        = true                                {C1 product}
     intx CICompilerCount                          := 4                                   {product}
     bool CICompilerCountPerCPU                     = true                                {product}
     bool CITime                                    = false                               {product}
     bool CMSAbortSemantics                         = false                               {product}
    uintx CMSAbortablePrecleanMinWorkPerIteration   = 100                                 {product}
     intx CMSAbortablePrecleanWaitMillis            = 100                                 {manageable}
    uintx CMSBitMapYieldQuantum                     = 10485760                            {product}
    uintx CMSBootstrapOccupancy                     = 50                                  {product}
     bool CMSClassUnloadingEnabled                  = true                                {product}
    uintx CMSClassUnloadingMaxInterval              = 0                                   {product}
     bool CMSCleanOnEnter                           = true                                {product}
     bool CMSCompactWhenClearAllSoftRefs            = true                                {product}
    uintx CMSConcMarkMultiple                       = 32                                  {product}
     bool CMSConcurrentMTEnabled                    = true                                {product}
    uintx CMSCoordinatorYieldSleepCount             = 10                                  {product}
     bool CMSDumpAtPromotionFailure                 = false                               {product}
     bool CMSEdenChunksRecordAlways                 = true                                {product}
    uintx CMSExpAvgFactor                           = 50                                  {product}
     bool CMSExtrapolateSweep                       = false                               {product}
    uintx CMSFullGCsBeforeCompaction                = 0                                   {product}
    uintx CMSIncrementalDutyCycle                   = 10                                  {product}
    uintx CMSIncrementalDutyCycleMin                = 0                                   {product}
     bool CMSIncrementalMode                        = false                               {product}
    uintx CMSIncrementalOffset                      = 0                                   {product}
     bool CMSIncrementalPacing                      = true                                {product}
    uintx CMSIncrementalSafetyFactor                = 10                                  {product}
    uintx CMSIndexedFreeListReplenish               = 4                                   {product}
     intx CMSInitiatingOccupancyFraction            = -1                                  {product}
    uintx CMSIsTooFullPercentage                    = 98                                  {product}
   double CMSLargeCoalSurplusPercent                = 0.950000                            {product}
   double CMSLargeSplitSurplusPercent               = 1.000000                            {product}
     bool CMSLoopWarn                               = false                               {product}
    uintx CMSMaxAbortablePrecleanLoops              = 0                                   {product}
     intx CMSMaxAbortablePrecleanTime               = 5000                                {product}
    uintx CMSOldPLABMax                             = 1024                                {product}
    uintx CMSOldPLABMin                             = 16                                  {product}
    uintx CMSOldPLABNumRefills                      = 4                                   {product}
    uintx CMSOldPLABReactivityFactor                = 2                                   {product}
     bool CMSOldPLABResizeQuicker                   = false                               {product}
    uintx CMSOldPLABToleranceFactor                 = 4                                   {product}
     bool CMSPLABRecordAlways                       = true                                {product}
    uintx CMSParPromoteBlocksToClaim                = 16                                  {product}
     bool CMSParallelInitialMarkEnabled             = true                                {product}
     bool CMSParallelRemarkEnabled                  = true                                {product}
     bool CMSParallelSurvivorRemarkEnabled          = true                                {product}
    uintx CMSPrecleanDenominator                    = 3                                   {product}
    uintx CMSPrecleanIter                           = 3                                   {product}
    uintx CMSPrecleanNumerator                      = 2                                   {product}
     bool CMSPrecleanRefLists1                      = true                                {product}
     bool CMSPrecleanRefLists2                      = false                               {product}
     bool CMSPrecleanSurvivors1                     = false                               {product}
     bool CMSPrecleanSurvivors2                     = true                                {product}
    uintx CMSPrecleanThreshold                      = 1000                                {product}
     bool CMSPrecleaningEnabled                     = true                                {product}
     bool CMSPrintChunksInDump                      = false                               {product}
     bool CMSPrintEdenSurvivorChunks                = false                               {product}
     bool CMSPrintObjectsInDump                     = false                               {product}
    uintx CMSRemarkVerifyVariant                    = 1                                   {product}
     bool CMSReplenishIntermediate                  = true                                {product}
    uintx CMSRescanMultiple                         = 32                                  {product}
    uintx CMSSamplingGrain                          = 16384                               {product}
     bool CMSScavengeBeforeRemark                   = false                               {product}
    uintx CMSScheduleRemarkEdenPenetration          = 50                                  {product}
    uintx CMSScheduleRemarkEdenSizeThreshold        = 2097152                             {product}
    uintx CMSScheduleRemarkSamplingRatio            = 5                                   {product}
   double CMSSmallCoalSurplusPercent                = 1.050000                            {product}
   double CMSSmallSplitSurplusPercent               = 1.100000                            {product}
     bool CMSSplitIndexedFreeListBlocks             = true                                {product}
     intx CMSTriggerInterval                        = -1                                  {manageable}
    uintx CMSTriggerRatio                           = 80                                  {product}
     intx CMSWaitDuration                           = 2000                                {manageable}
    uintx CMSWorkQueueDrainThreshold                = 10                                  {product}
     bool CMSYield                                  = true                                {product}
    uintx CMSYieldSleepCount                        = 0                                   {product}
    uintx CMSYoungGenPerWorker                      = 67108864                            {pd product}
    uintx CMS_FLSPadding                            = 1                                   {product}
    uintx CMS_FLSWeight                             = 75                                  {product}
    uintx CMS_SweepPadding                          = 1                                   {product}
    uintx CMS_SweepTimerThresholdMillis             = 10                                  {product}
    uintx CMS_SweepWeight                           = 75                                  {product}
     bool CheckEndorsedAndExtDirs                   = false                               {product}
     bool CheckJNICalls                             = false                               {product}
     bool ClassUnloading                            = true                                {product}
     bool ClassUnloadingWithConcurrentMark          = true                                {product}
     intx ClearFPUAtPark                            = 0                                   {product}
     bool ClipInlining                              = true                                {product}
    uintx CodeCacheExpansionSize                    = 65536                               {pd product}
    uintx CodeCacheMinimumFreeSpace                 = 512000                              {product}
     bool CollectGen0First                          = false                               {product}
     bool CompactFields                             = true                                {product}
     intx CompilationPolicyChoice                   = 3                                   {product}
    ccstrlist CompileCommand                        =                                     {product}
  ccstr   CompileCommandFile                        =                                     {product}
  ccstrlist CompileOnly                             =                                     {product}
     intx CompileThreshold                          = 10000                               {pd product}
     bool CompilerThreadHintNoPreempt               = true                                {product}
     intx CompilerThreadPriority                    = -1                                  {product}
     intx CompilerThreadStackSize                   = 0                                   {pd product}
    uintx CompressedClassSpaceSize                 := 2097152                             {product}
    uintx ConcGCThreads                             = 0                                   {product}
     intx ConditionalMoveLimit                      = 3                                   {C2 pd product}
     intx ContendedPaddingWidth                     = 128                                 {product}
     bool ConvertSleepToYield                       = true                                {pd product}
     bool ConvertYieldToSleep                       = false                               {product}
     bool CrashOnOutOfMemoryError                   = false                               {product}
     bool CreateMinidumpOnCrash                     = false                               {product}
     bool CriticalJNINatives                        = true                                {product}
     bool DTraceAllocProbes                         = false                               {product}
     bool DTraceMethodProbes                        = false                               {product}
     bool DTraceMonitorProbes                       = false                               {product}
     bool Debugging                                 = false                               {product}
    uintx DefaultMaxRAMFraction                     = 4                                   {product}
     intx DefaultThreadPriority                     = -1                                  {product}
     intx DeferPollingPageLoopCount                 = -1                                  {product}
     intx DeferThrSuspendLoopCount                  = 4000                                {product}
     bool DeoptimizeRandom                          = false                               {product}
     bool DisableAttachMechanism                    = false                               {product}
     bool DisableExplicitGC                         = false                               {product}
     bool DisplayVMOutputToStderr                   = false                               {product}
     bool DisplayVMOutputToStdout                   = false                               {product}
     bool DoEscapeAnalysis                          = true                                {C2 product}
     bool DontCompileHugeMethods                    = true                                {product}
     bool DontYieldALot                             = false                               {pd product}
    ccstr DumpLoadedClassList                       =                                     {product}
     bool DumpReplayDataOnError                     = true                                {product}
     bool DumpSharedSpaces                          = false                               {product}
     bool EagerXrunInit                             = false                               {product}
     intx EliminateAllocationArraySizeLimit         = 64                                  {C2 product}
     bool EliminateAllocations                      = true                                {C2 product}
     bool EliminateAutoBox                          = true                                {C2 product}
     bool EliminateLocks                            = true                                {C2 product}
     bool EliminateNestedLocks                      = true                                {C2 product}
     intx EmitSync                                  = 0                                   {product}
     bool EnableContended                           = true                                {product}
     bool EnableResourceManagementTLABCache         = true                                {product}
     bool EnableSharedLookupCache                   = true                                {product}
     bool EnableTracing                             = false                               {product}
    uintx ErgoHeapSizeLimit                         = 0                                   {product}
    ccstr ErrorFile                                 =                                     {product}
    ccstr ErrorReportServer                         =                                     {product}
   double EscapeAnalysisTimeout                     = 20.000000                           {C2 product}
     bool EstimateArgEscape                         = true                                {product}
     bool ExitOnOutOfMemoryError                    = false                               {product}
     bool ExplicitGCInvokesConcurrent               = false                               {product}
     bool ExplicitGCInvokesConcurrentAndUnloadsClasses  = false                               {product}
     bool ExtendedDTraceProbes                      = false                               {product}
    ccstr ExtraSharedClassListFile                  =                                     {product}
     bool FLSAlwaysCoalesceLarge                    = false                               {product}
    uintx FLSCoalescePolicy                         = 2                                   {product}
   double FLSLargestBlockCoalesceProximity          = 0.990000                            {product}
     bool FailOverToOldVerifier                     = true                                {product}
     bool FastTLABRefill                            = true                                {product}
     intx FenceInstruction                          = 0                                   {ARCH product}
     intx FieldsAllocationStyle                     = 1                                   {product}
     bool FilterSpuriousWakeups                     = true                                {product}
    ccstr FlightRecorderOptions                     =                                     {product}
     bool ForceNUMA                                 = false                               {product}
     bool ForceTimeHighResolution                   = false                               {product}
     intx FreqInlineSize                            = 325                                 {pd product}
   double G1ConcMarkStepDurationMillis              = 10.000000                           {product}
    uintx G1ConcRSHotCardLimit                      = 4                                   {product}
    uintx G1ConcRSLogCacheSize                      = 10                                  {product}
     intx G1ConcRefinementGreenZone                 = 0                                   {product}
     intx G1ConcRefinementRedZone                   = 0                                   {product}
     intx G1ConcRefinementServiceIntervalMillis     = 300                                 {product}
    uintx G1ConcRefinementThreads                   = 0                                   {product}
     intx G1ConcRefinementThresholdStep             = 0                                   {product}
     intx G1ConcRefinementYellowZone                = 0                                   {product}
    uintx G1ConfidencePercent                       = 50                                  {product}
    uintx G1HeapRegionSize                          = 0                                   {product}
    uintx G1HeapWastePercent                        = 5                                   {product}
    uintx G1MixedGCCountTarget                      = 8                                   {product}
     intx G1RSetRegionEntries                       = 0                                   {product}
    uintx G1RSetScanBlockSize                       = 64                                  {product}
     intx G1RSetSparseRegionEntries                 = 0                                   {product}
     intx G1RSetUpdatingPauseTimePercent            = 10                                  {product}
     intx G1RefProcDrainInterval                    = 10                                  {product}
    uintx G1ReservePercent                          = 10                                  {product}
    uintx G1SATBBufferEnqueueingThresholdPercent    = 60                                  {product}
     intx G1SATBBufferSize                          = 1024                                {product}
     intx G1UpdateBufferSize                        = 256                                 {product}
     bool G1UseAdaptiveConcRefinement               = true                                {product}
    uintx GCDrainStackTargetSize                    = 64                                  {product}
    uintx GCHeapFreeLimit                           = 2                                   {product}
    uintx GCLockerEdenExpansionPercent              = 5                                   {product}
     bool GCLockerInvokesConcurrent                 = false                               {product}
    uintx GCLogFileSize                             = 8192                                {product}
    uintx GCPauseIntervalMillis                     = 0                                   {product}
    uintx GCTaskTimeStampEntries                    = 200                                 {product}
    uintx GCTimeLimit                               = 98                                  {product}
    uintx GCTimeRatio                               = 99                                  {product}
    uintx HeapBaseMinAddress                        = 2147483648                          {pd product}
     bool HeapDumpAfterFullGC                       = false                               {manageable}
     bool HeapDumpBeforeFullGC                      = false                               {manageable}
     bool HeapDumpOnOutOfMemoryError                = false                               {manageable}
    ccstr HeapDumpPath                              =                                     {manageable}
    uintx HeapFirstMaximumCompactionCount           = 3                                   {product}
    uintx HeapMaximumCompactionInterval             = 20                                  {product}
    uintx HeapSizePerGCThread                       = 87241520                            {product}
     bool IgnoreEmptyClassPaths                     = false                               {product}
     bool IgnoreUnrecognizedVMOptions               = false                               {product}
    uintx IncreaseFirstTierCompileThresholdAt       = 50                                  {product}
     bool IncrementalInline                         = true                                {C2 product}
    uintx InitialBootClassLoaderMetaspaceSize       = 4194304                             {product}
    uintx InitialCodeCacheSize                      = 2555904                             {pd product}
    uintx InitialHeapSize                          := 536870912                           {product}
    uintx InitialRAMFraction                        = 64                                  {product}
   double InitialRAMPercentage                      = 1.562500                            {product}
    uintx InitialSurvivorRatio                      = 8                                   {product}
    uintx InitialTenuringThreshold                  = 7                                   {product}
    uintx InitiatingHeapOccupancyPercent            = 45                                  {product}
     bool Inline                                    = true                                {product}
    ccstr InlineDataFile                            =                                     {product}
     intx InlineSmallCode                           = 2000                                {pd product}
     bool InlineSynchronizedMethods                 = true                                {C1 product}
     bool InsertMemBarAfterArraycopy                = true                                {C2 product}
     intx InteriorEntryAlignment                    = 16                                  {C2 pd product}
     intx InterpreterProfilePercentage              = 33                                  {product}
     bool JNIDetachReleasesMonitors                 = true                                {product}
     bool JavaMonitorsInStackTrace                  = true                                {product}
     intx JavaPriority10_To_OSPriority              = -1                                  {product}
     intx JavaPriority1_To_OSPriority               = -1                                  {product}
     intx JavaPriority2_To_OSPriority               = -1                                  {product}
     intx JavaPriority3_To_OSPriority               = -1                                  {product}
     intx JavaPriority4_To_OSPriority               = -1                                  {product}
     intx JavaPriority5_To_OSPriority               = -1                                  {product}
     intx JavaPriority6_To_OSPriority               = -1                                  {product}
     intx JavaPriority7_To_OSPriority               = -1                                  {product}
     intx JavaPriority8_To_OSPriority               = -1                                  {product}
     intx JavaPriority9_To_OSPriority               = -1                                  {product}
     bool LIRFillDelaySlots                         = false                               {C1 pd product}
    uintx LargePageHeapSizeThreshold                = 134217728                           {product}
    uintx LargePageSizeInBytes                      = 0                                   {product}
     bool LazyBootClassLoader                       = true                                {product}
     intx LiveNodeCountInliningCutoff               = 40000                               {C2 product}
     bool LogCommercialFeatures                     = false                               {product}
     intx LoopMaxUnroll                             = 16                                  {C2 product}
     intx LoopOptsCount                             = 43                                  {C2 product}
     intx LoopUnrollLimit                           = 60                                  {C2 pd product}
     intx LoopUnrollMin                             = 4                                   {C2 product}
     bool LoopUnswitching                           = true                                {C2 product}
     bool ManagementServer                          = false                               {product}
    uintx MarkStackSize                             = 4194304                             {product}
    uintx MarkStackSizeMax                          = 536870912                           {product}
    uintx MarkSweepAlwaysCompactCount               = 4                                   {product}
    uintx MarkSweepDeadRatio                        = 1                                   {product}
     intx MaxBCEAEstimateLevel                      = 5                                   {product}
     intx MaxBCEAEstimateSize                       = 150                                 {product}
    uintx MaxDirectMemorySize                       = 0                                   {product}
     bool MaxFDLimit                                = true                                {product}
    uintx MaxGCMinorPauseMillis                     = 4294967295                          {product}
    uintx MaxGCPauseMillis                          = 4294967295                          {product}
    uintx MaxHeapFreeRatio                          = 100                                 {manageable}
    uintx MaxHeapSize                              := 4273995776                          {product}
     intx MaxInlineLevel                            = 9                                   {product}
     intx MaxInlineSize                             = 35                                  {product}
     intx MaxJNILocalCapacity                       = 65536                               {product}
     intx MaxJavaStackTraceDepth                    = 1024                                {product}
     intx MaxJumpTableSize                          = 65000                               {C2 product}
     intx MaxJumpTableSparseness                    = 5                                   {C2 product}
     intx MaxLabelRootDepth                         = 1100                                {C2 product}
     intx MaxLoopPad                                = 11                                  {C2 product}
    uintx MaxMetaspaceExpansion                     = 5451776                             {product}
    uintx MaxMetaspaceFreeRatio                     = 70                                  {product}
    uintx MaxMetaspaceSize                         := 10485760                            {product}
    uintx MaxNewSize                               := 2856321024                          {product}
     intx MaxNodeLimit                              = 75000                               {C2 product}
  uint64_t MaxRAM                                   = 0                                   {pd product}
    uintx MaxRAMFraction                            = 4                                   {product}
   double MaxRAMPercentage                          = 25.000000                           {product}
     intx MaxRecursiveInlineLevel                   = 1                                   {product}
    uintx MaxTenuringThreshold                      = 15                                  {product}
     intx MaxTrivialSize                            = 6                                   {product}
     intx MaxVectorSize                             = 32                                  {C2 product}
    uintx MetaspaceSize                            := 5242880                             {pd product}
     bool MethodFlushing                            = true                                {product}
    uintx MinHeapDeltaBytes                        := 524288                              {product}
    uintx MinHeapFreeRatio                          = 0                                   {manageable}
     intx MinInliningThreshold                      = 250                                 {product}
     intx MinJumpTableSize                          = 10                                  {C2 pd product}
    uintx MinMetaspaceExpansion                     = 339968                              {product}
    uintx MinMetaspaceFreeRatio                     = 40                                  {product}
    uintx MinRAMFraction                            = 2                                   {product}
   double MinRAMPercentage                          = 50.000000                           {product}
    uintx MinSurvivorRatio                          = 3                                   {product}
    uintx MinTLABSize                               = 2048                                {product}
     intx MonitorBound                              = 0                                   {product}
     bool MonitorInUseLists                         = false                               {product}
     intx MultiArrayExpandLimit                     = 6                                   {C2 product}
     bool MustCallLoadClassInternal                 = false                               {product}
    uintx NUMAChunkResizeWeight                     = 20                                  {product}
    uintx NUMAInterleaveGranularity                 = 2097152                             {product}
    uintx NUMAPageScanRate                          = 256                                 {product}
    uintx NUMASpaceResizeRate                       = 1073741824                          {product}
     bool NUMAStats                                 = false                               {product}
    ccstr NativeMemoryTracking                      = off                                 {product}
     bool NeedsDeoptSuspend                         = false                               {pd product}
     bool NeverActAsServerClassMachine              = false                               {pd product}
     bool NeverTenure                               = false                               {product}
    uintx NewRatio                                  = 2                                   {product}
    uintx NewSize                                  := 178782208                           {product}
    uintx NewSizeThreadIncrease                     = 5320                                {pd product}
     intx NmethodSweepActivity                      = 10                                  {product}
     intx NmethodSweepCheckInterval                 = 5                                   {product}
     intx NmethodSweepFraction                      = 16                                  {product}
     intx NodeLimitFudgeFactor                      = 2000                                {C2 product}
    uintx NumberOfGCLogFiles                        = 0                                   {product}
     intx NumberOfLoopInstrToAlign                  = 4                                   {C2 product}
     intx ObjectAlignmentInBytes                    = 8                                   {lp64_product}
    uintx OldPLABSize                               = 1024                                {product}
    uintx OldPLABWeight                             = 50                                  {product}
    uintx OldSize                                  := 358088704                           {product}
     bool OmitStackTraceInFastThrow                 = true                                {product}
  ccstrlist OnError                                 =                                     {product}
  ccstrlist OnOutOfMemoryError                      =                                     {product}
     intx OnStackReplacePercentage                  = 140                                 {pd product}
     bool OptimizeFill                              = true                                {C2 product}
     bool OptimizePtrCompare                        = true                                {C2 product}
     bool OptimizeStringConcat                      = true                                {C2 product}
     bool OptoBundling                              = false                               {C2 pd product}
     intx OptoLoopAlignment                         = 16                                  {pd product}
     bool OptoScheduling                            = false                               {C2 pd product}
    uintx PLABWeight                                = 75                                  {product}
     bool PSChunkLargeArrays                        = true                                {product}
     intx ParGCArrayScanChunk                       = 50                                  {product}
    uintx ParGCDesiredObjsFromOverflowList          = 20                                  {product}
     bool ParGCTrimOverflow                         = true                                {product}
     bool ParGCUseLocalOverflow                     = false                               {product}
    uintx ParallelGCBufferWastePct                  = 10                                  {product}
    uintx ParallelGCThreads                         = 8                                   {product}
     bool ParallelGCVerbose                         = false                               {product}
    uintx ParallelOldDeadWoodLimiterMean            = 50                                  {product}
    uintx ParallelOldDeadWoodLimiterStdDev          = 80                                  {product}
     bool ParallelRefProcBalancingEnabled           = true                                {product}
     bool ParallelRefProcEnabled                    = false                               {product}
     bool PartialPeelAtUnsignedTests                = true                                {C2 product}
     bool PartialPeelLoop                           = true                                {C2 product}
     intx PartialPeelNewPhiDelta                    = 0                                   {C2 product}
    uintx PausePadding                              = 1                                   {product}
     intx PerBytecodeRecompilationCutoff            = 200                                 {product}
     intx PerBytecodeTrapLimit                      = 4                                   {product}
     intx PerMethodRecompilationCutoff              = 400                                 {product}
     intx PerMethodTrapLimit                        = 100                                 {product}
     bool PerfAllowAtExitRegistration               = false                               {product}
     bool PerfBypassFileSystemCheck                 = false                               {product}
     intx PerfDataMemorySize                        = 32768                               {product}
     intx PerfDataSamplingInterval                  = 50                                  {product}
    ccstr PerfDataSaveFile                          =                                     {product}
     bool PerfDataSaveToFile                        = false                               {product}
     bool PerfDisableSharedMem                      = false                               {product}
     intx PerfMaxStringConstLength                  = 1024                                {product}
     intx PreInflateSpin                            = 10                                  {pd product}
     bool PreferInterpreterNativeStubs              = false                               {pd product}
     intx PrefetchCopyIntervalInBytes               = 576                                 {product}
     intx PrefetchFieldsAhead                       = 1                                   {product}
     intx PrefetchScanIntervalInBytes               = 576                                 {product}
     bool PreserveAllAnnotations                    = false                               {product}
     bool PreserveFramePointer                      = false                               {pd product}
    uintx PretenureSizeThreshold                    = 0                                   {product}
     bool PrintAdaptiveSizePolicy                   = false                               {product}
     bool PrintCMSInitiationStatistics              = false                               {product}
     intx PrintCMSStatistics                        = 0                                   {product}
     bool PrintClassHistogram                       = false                               {manageable}
     bool PrintClassHistogramAfterFullGC            = false                               {manageable}
     bool PrintClassHistogramBeforeFullGC           = false                               {manageable}
     bool PrintCodeCache                            = false                               {product}
     bool PrintCodeCacheOnCompilation               = false                               {product}
     bool PrintCommandLineFlags                     = false                               {product}
     bool PrintCompilation                          = false                               {product}
     bool PrintConcurrentLocks                      = false                               {manageable}
     intx PrintFLSCensus                            = 0                                   {product}
     intx PrintFLSStatistics                        = 0                                   {product}
     bool PrintFlagsFinal                          := true                                {product}
     bool PrintFlagsInitial                         = false                               {product}
     bool PrintGC                                   = true                                {manageable}
     bool PrintGCApplicationConcurrentTime          = false                               {product}
     bool PrintGCApplicationStoppedTime             = false                               {product}
     bool PrintGCCause                              = true                                {product}
     bool PrintGCDateStamps                         = false                               {manageable}
     bool PrintGCDetails                           := true                                {manageable}
     bool PrintGCID                                 = false                               {manageable}
     bool PrintGCTaskTimeStamps                     = false                               {product}
     bool PrintGCTimeStamps                         = false                               {manageable}
     bool PrintHeapAtGC                             = false                               {product rw}
     bool PrintHeapAtGCExtended                     = false                               {product rw}
     bool PrintHeapAtSIGBREAK                       = true                                {product}
     bool PrintJNIGCStalls                          = false                               {product}
     bool PrintJNIResolving                         = false                               {product}
     bool PrintOldPLAB                              = false                               {product}
     bool PrintOopAddress                           = false                               {product}
     bool PrintPLAB                                 = false                               {product}
     bool PrintParallelOldGCPhaseTimes              = false                               {product}
     bool PrintPromotionFailure                     = false                               {product}
     bool PrintReferenceGC                          = false                               {product}
     bool PrintSafepointStatistics                  = false                               {product}
     intx PrintSafepointStatisticsCount             = 300                                 {product}
     intx PrintSafepointStatisticsTimeout           = -1                                  {product}
     bool PrintSharedArchiveAndExit                 = false                               {product}
     bool PrintSharedDictionary                     = false                               {product}
     bool PrintSharedSpaces                         = false                               {product}
     bool PrintStringDeduplicationStatistics        = false                               {product}
     bool PrintStringTableStatistics                = false                               {product}
     bool PrintTLAB                                 = false                               {product}
     bool PrintTenuringDistribution                 = false                               {product}
     bool PrintTieredEvents                         = false                               {product}
     bool PrintVMOptions                            = false                               {product}
     bool PrintVMQWaitTime                          = false                               {product}
     bool PrintWarnings                             = true                                {product}
    uintx ProcessDistributionStride                 = 4                                   {product}
     bool ProfileInterpreter                        = true                                {pd product}
     bool ProfileIntervals                          = false                               {product}
     intx ProfileIntervalsTicks                     = 100                                 {product}
     intx ProfileMaturityPercentage                 = 20                                  {product}
     bool ProfileVM                                 = false                               {product}
     bool ProfilerPrintByteCodeStatistics           = false                               {product}
     bool ProfilerRecordPC                          = false                               {product}
    uintx PromotedPadding                           = 3                                   {product}
    uintx QueuedAllocationWarningCount              = 0                                   {product}
    uintx RTMRetryCount                             = 5                                   {ARCH product}
     bool RangeCheckElimination                     = true                                {product}
     intx ReadPrefetchInstr                         = 0                                   {ARCH product}
     bool ReassociateInvariants                     = true                                {C2 product}
     bool ReduceBulkZeroing                         = true                                {C2 product}
     bool ReduceFieldZeroing                        = true                                {C2 product}
     bool ReduceInitialCardMarks                    = true                                {C2 product}
     bool ReduceSignalUsage                         = false                               {product}
     intx RefDiscoveryPolicy                        = 0                                   {product}
     bool ReflectionWrapResolutionErrors            = true                                {product}
     bool RegisterFinalizersAtInit                  = true                                {product}
     bool RelaxAccessControlCheck                   = false                               {product}
    ccstr ReplayDataFile                            =                                     {product}
     bool RequireSharedSpaces                       = false                               {product}
    uintx ReservedCodeCacheSize                     = 251658240                           {pd product}
     bool ResizeOldPLAB                             = true                                {product}
     bool ResizePLAB                                = true                                {product}
     bool ResizeTLAB                                = true                                {pd product}
     bool RestoreMXCSROnJNICalls                    = false                               {product}
     bool RestrictContended                         = true                                {product}
     bool RewriteBytecodes                          = true                                {pd product}
     bool RewriteFrequentPairs                      = false                               {pd product}
     intx SafepointPollOffset                       = 256                                 {C1 pd product}
     intx SafepointSpinBeforeYield                  = 2000                                {product}
     bool SafepointTimeout                          = false                               {product}
     intx SafepointTimeoutDelay                     = 10000                               {product}
     bool ScavengeBeforeFullGC                      = true                                {product}
     intx SelfDestructTimer                         = 0                                   {product}
    uintx SharedBaseAddress                         = 0                                   {product}
    ccstr SharedClassListFile                       =                                     {product}
    uintx SharedMiscCodeSize                        = 122880                              {product}
    uintx SharedMiscDataSize                        = 4194304                             {product}
    uintx SharedReadOnlySize                        = 16777216                            {product}
    uintx SharedReadWriteSize                       = 16777216                            {product}
     bool ShowMessageBoxOnError                     = false                               {product}
     intx SoftRefLRUPolicyMSPerMB                   = 1000                                {product}
     bool SpecialEncodeISOArray                     = true                                {C2 product}
     bool SplitIfBlocks                             = true                                {C2 product}
     intx StackRedPages                             = 1                                   {pd product}
     intx StackShadowPages                          = 6                                   {pd product}
     bool StackTraceInThrowable                     = true                                {product}
     intx StackYellowPages                          = 3                                   {pd product}
     bool StartAttachListener                       = false                               {product}
     intx StarvationMonitorInterval                 = 200                                 {product}
     bool StressLdcRewrite                          = false                               {product}
    uintx StringDeduplicationAgeThreshold           = 3                                   {product}
    uintx StringTableSize                           = 60013                               {product}
     bool SuppressFatalErrorMessage                 = false                               {product}
    uintx SurvivorPadding                           = 3                                   {product}
    uintx SurvivorRatio                             = 8                                   {product}
     intx SuspendRetryCount                         = 50                                  {product}
     intx SuspendRetryDelay                         = 5                                   {product}
     intx SyncFlags                                 = 0                                   {product}
    ccstr SyncKnobs                                 =                                     {product}
     intx SyncVerbose                               = 0                                   {product}
    uintx TLABAllocationWeight                      = 35                                  {product}
    uintx TLABRefillWasteFraction                   = 64                                  {product}
    uintx TLABSize                                  = 0                                   {product}
     bool TLABStats                                 = true                                {product}
    uintx TLABWasteIncrement                        = 4                                   {product}
    uintx TLABWasteTargetPercent                    = 1                                   {product}
    uintx TargetPLABWastePct                        = 10                                  {product}
    uintx TargetSurvivorRatio                       = 50                                  {product}
    uintx TenuredGenerationSizeIncrement            = 20                                  {product}
    uintx TenuredGenerationSizeSupplement           = 80                                  {product}
    uintx TenuredGenerationSizeSupplementDecay      = 2                                   {product}
     intx ThreadPriorityPolicy                      = 0                                   {product}
     bool ThreadPriorityVerbose                     = false                               {product}
    uintx ThreadSafetyMargin                        = 52428800                            {product}
     intx ThreadStackSize                           = 0                                   {pd product}
    uintx ThresholdTolerance                        = 10                                  {product}
     intx Tier0BackedgeNotifyFreqLog                = 10                                  {product}
     intx Tier0InvokeNotifyFreqLog                  = 7                                   {product}
     intx Tier0ProfilingStartPercentage             = 200                                 {product}
     intx Tier23InlineeNotifyFreqLog                = 20                                  {product}
     intx Tier2BackEdgeThreshold                    = 0                                   {product}
     intx Tier2BackedgeNotifyFreqLog                = 14                                  {product}
     intx Tier2CompileThreshold                     = 0                                   {product}
     intx Tier2InvokeNotifyFreqLog                  = 11                                  {product}
     intx Tier3BackEdgeThreshold                    = 60000                               {product}
     intx Tier3BackedgeNotifyFreqLog                = 13                                  {product}
     intx Tier3CompileThreshold                     = 2000                                {product}
     intx Tier3DelayOff                             = 2                                   {product}
     intx Tier3DelayOn                              = 5                                   {product}
     intx Tier3InvocationThreshold                  = 200                                 {product}
     intx Tier3InvokeNotifyFreqLog                  = 10                                  {product}
     intx Tier3LoadFeedback                         = 5                                   {product}
     intx Tier3MinInvocationThreshold               = 100                                 {product}
     intx Tier4BackEdgeThreshold                    = 40000                               {product}
     intx Tier4CompileThreshold                     = 15000                               {product}
     intx Tier4InvocationThreshold                  = 5000                                {product}
     intx Tier4LoadFeedback                         = 3                                   {product}
     intx Tier4MinInvocationThreshold               = 600                                 {product}
     bool TieredCompilation                         = true                                {pd product}
     intx TieredCompileTaskTimeout                  = 50                                  {product}
     intx TieredRateUpdateMaxTime                   = 25                                  {product}
     intx TieredRateUpdateMinTime                   = 1                                   {product}
     intx TieredStopAtLevel                         = 4                                   {product}
     bool TimeLinearScan                            = false                               {C1 product}
     bool TraceBiasedLocking                        = false                               {product}
     bool TraceClassLoading                         = false                               {product rw}
     bool TraceClassLoadingPreorder                 = false                               {product}
     bool TraceClassPaths                           = false                               {product}
     bool TraceClassResolution                      = false                               {product}
     bool TraceClassUnloading                       = false                               {product rw}
     bool TraceDynamicGCThreads                     = false                               {product}
     bool TraceGen0Time                             = false                               {product}
     bool TraceGen1Time                             = false                               {product}
    ccstr TraceJVMTI                                =                                     {product}
     bool TraceLoaderConstraints                    = false                               {product rw}
     bool TraceMetadataHumongousAllocation          = false                               {product}
     bool TraceMonitorInflation                     = false                               {product}
     bool TraceParallelOldGCTasks                   = false                               {product}
     intx TraceRedefineClasses                      = 0                                   {product}
     bool TraceSafepointCleanupTime                 = false                               {product}
     bool TraceSharedLookupCache                    = false                               {product}
     bool TraceSuspendWaitFailures                  = false                               {product}
     intx TrackedInitializationLimit                = 50                                  {C2 product}
     bool TransmitErrorReport                       = false                               {product}
     bool TrapBasedNullChecks                       = false                               {pd product}
     bool TrapBasedRangeChecks                      = false                               {C2 pd product}
     intx TypeProfileArgsLimit                      = 2                                   {product}
    uintx TypeProfileLevel                          = 111                                 {pd product}
     intx TypeProfileMajorReceiverPercent           = 90                                  {C2 product}
     intx TypeProfileParmsLimit                     = 2                                   {product}
     intx TypeProfileWidth                          = 2                                   {product}
     intx UnguardOnExecutionViolation               = 0                                   {product}
     bool UnlinkSymbolsALot                         = false                               {product}
     bool Use486InstrsOnly                          = false                               {ARCH product}
     bool UseAES                                    = true                                {product}
     bool UseAESIntrinsics                          = true                                {product}
     intx UseAVX                                    = 2                                   {ARCH product}
     bool UseAdaptiveGCBoundary                     = false                               {product}
     bool UseAdaptiveGenerationSizePolicyAtMajorCollection  = true                                {product}
     bool UseAdaptiveGenerationSizePolicyAtMinorCollection  = true                                {product}
     bool UseAdaptiveNUMAChunkSizing                = true                                {product}
     bool UseAdaptiveSizeDecayMajorGCCost           = true                                {product}
     bool UseAdaptiveSizePolicy                     = true                                {product}
     bool UseAdaptiveSizePolicyFootprintGoal        = true                                {product}
     bool UseAdaptiveSizePolicyWithSystemGC         = false                               {product}
     bool UseAddressNop                             = true                                {ARCH product}
     bool UseAltSigs                                = false                               {product}
     bool UseAutoGCSelectPolicy                     = false                               {product}
     bool UseBMI1Instructions                       = true                                {ARCH product}
     bool UseBMI2Instructions                       = true                                {ARCH product}
     bool UseBiasedLocking                          = true                                {product}
     bool UseBimorphicInlining                      = true                                {C2 product}
     bool UseBoundThreads                           = true                                {product}
     bool UseCLMUL                                  = true                                {ARCH product}
     bool UseCMSBestFit                             = true                                {product}
     bool UseCMSCollectionPassing                   = true                                {product}
     bool UseCMSCompactAtFullCollection             = true                                {product}
     bool UseCMSInitiatingOccupancyOnly             = false                               {product}
     bool UseCRC32Intrinsics                        = true                                {product}
     bool UseCodeCacheFlushing                      = true                                {product}
     bool UseCompiler                               = true                                {product}
     bool UseCompilerSafepoints                     = true                                {product}
     bool UseCompressedClassPointers               := true                                {lp64_product}
     bool UseCompressedOops                        := true                                {lp64_product}
     bool UseConcMarkSweepGC                        = false                               {product}
     bool UseCondCardMark                           = false                               {C2 product}
     bool UseCountLeadingZerosInstruction           = true                                {ARCH product}
     bool UseCountTrailingZerosInstruction          = true                                {ARCH product}
     bool UseCountedLoopSafepoints                  = false                               {C2 product}
     bool UseCounterDecay                           = true                                {product}
     bool UseDivMod                                 = true                                {C2 product}
     bool UseDynamicNumberOfGCThreads               = false                               {product}
     bool UseFPUForSpilling                         = true                                {C2 product}
     bool UseFastAccessorMethods                    = false                               {product}
     bool UseFastEmptyMethods                       = false                               {product}
     bool UseFastJNIAccessors                       = true                                {product}
     bool UseFastStosb                              = true                                {ARCH product}
     bool UseG1GC                                   = false                               {product}
     bool UseGCLogFileRotation                      = false                               {product}
     bool UseGCOverheadLimit                        = true                                {product}
     bool UseGCTaskAffinity                         = false                               {product}
     bool UseGHASHIntrinsics                        = true                                {product}
     bool UseHeavyMonitors                          = false                               {product}
     bool UseInlineCaches                           = true                                {product}
     bool UseInterpreter                            = true                                {product}
     bool UseJumpTables                             = true                                {C2 product}
     bool UseLWPSynchronization                     = true                                {product}
     bool UseLargePages                             = false                               {pd product}
     bool UseLargePagesInMetaspace                  = false                               {product}
     bool UseLargePagesIndividualAllocation        := false                               {pd product}
     bool UseLockedTracing                          = false                               {product}
     bool UseLoopCounter                            = true                                {product}
     bool UseLoopInvariantCodeMotion                = true                                {C1 product}
     bool UseLoopPredicate                          = true                                {C2 product}
     bool UseMathExactIntrinsics                    = true                                {C2 product}
     bool UseMaximumCompactionOnSystemGC            = true                                {product}
     bool UseMembar                                 = false                               {pd product}
     bool UseMontgomeryMultiplyIntrinsic            = true                                {C2 product}
     bool UseMontgomerySquareIntrinsic              = true                                {C2 product}
     bool UseMulAddIntrinsic                        = true                                {C2 product}
     bool UseMultiplyToLenIntrinsic                 = true                                {C2 product}
     bool UseNUMA                                   = false                               {product}
     bool UseNUMAInterleaving                       = false                               {product}
     bool UseNewLongLShift                          = false                               {ARCH product}
     bool UseOSErrorReporting                       = false                               {pd product}
     bool UseOldInlining                            = true                                {C2 product}
     bool UseOnStackReplacement                     = true                                {pd product}
     bool UseOnlyInlinedBimorphic                   = true                                {C2 product}
     bool UseOptoBiasInlining                       = true                                {C2 product}
     bool UsePSAdaptiveSurvivorSizePolicy           = true                                {product}
     bool UseParNewGC                               = false                               {product}
     bool UseParallelGC                            := true                                {product}
     bool UseParallelOldGC                          = true                                {product}
     bool UsePerfData                               = true                                {product}
     bool UsePopCountInstruction                    = true                                {product}
     bool UseRDPCForConstantTableBase               = false                               {C2 product}
     bool UseRTMDeopt                               = false                               {ARCH product}
     bool UseRTMLocking                             = false                               {ARCH product}
     bool UseSHA                                    = false                               {product}
     bool UseSHA1Intrinsics                         = false                               {product}
     bool UseSHA256Intrinsics                       = false                               {product}
     bool UseSHA512Intrinsics                       = false                               {product}
     intx UseSSE                                    = 4                                   {product}
     bool UseSSE42Intrinsics                        = true                                {product}
     bool UseSerialGC                               = false                               {product}
     bool UseSharedSpaces                           = false                               {product}
     bool UseSignalChaining                         = true                                {product}
     bool UseSquareToLenIntrinsic                   = true                                {C2 product}
     bool UseStoreImmI16                            = false                               {ARCH product}
     bool UseStringDeduplication                    = false                               {product}
     bool UseSuperWord                              = true                                {C2 product}
     bool UseTLAB                                   = true                                {pd product}
     bool UseThreadPriorities                       = true                                {pd product}
     bool UseTypeProfile                            = true                                {product}
     bool UseTypeSpeculation                        = true                                {C2 product}
     bool UseUTCFileTimestamp                       = true                                {product}
     bool UseUnalignedLoadStores                    = true                                {ARCH product}
     bool UseVMInterruptibleIO                      = false                               {product}
     bool UseXMMForArrayCopy                        = true                                {product}
     bool UseXmmI2D                                 = false                               {ARCH product}
     bool UseXmmI2F                                 = false                               {ARCH product}
     bool UseXmmLoadAndClearUpper                   = true                                {ARCH product}
     bool UseXmmRegToRegMoveAll                     = true                                {ARCH product}
     bool VMThreadHintNoPreempt                     = false                               {product}
     intx VMThreadPriority                          = -1                                  {product}
     intx VMThreadStackSize                         = 0                                   {pd product}
     intx ValueMapInitialSize                       = 11                                  {C1 product}
     intx ValueMapMaxLoopSize                       = 8                                   {C1 product}
     intx ValueSearchLimit                          = 1000                                {C2 product}
     bool VerifyMergedCPBytecodes                   = true                                {product}
     bool VerifySharedSpaces                        = false                               {product}
     intx WorkAroundNPTLTimedWaitHang               = 1                                   {product}
    uintx YoungGenerationSizeIncrement              = 20                                  {product}
    uintx YoungGenerationSizeSupplement             = 80                                  {product}
    uintx YoungGenerationSizeSupplementDecay        = 8                                   {product}
    uintx YoungPLABSize                             = 4096                                {product}
     bool ZeroTLAB                                  = false                               {product}
     intx hashCode                                  = 5                                   {product}
  ```

### 3.2 让性能飞起来：学习堆的配置参数
#### 3.2.1 最大堆和初始堆的设置
  Java 进程启动时，虚拟机就会分配一块初始堆空间（可以通过参数`-Xms`指定初始堆空间大小）。虚拟机会尽量维持在初始堆空间的范围内运行，如果初始堆空间耗尽，虚拟机会对堆空间扩展，上限为最大堆空间（用参数`-Xmx`设置）。

  Heap 空间大小及关系：[HeapAlloc](../java/com/ibgdn/chapter_3/HeapAlloc.java)

  VM options：
  ```
  -Xmx20m -Xms5m -XX:+PrintCommandLineFlags -XX:+PrintGCDetails -XX:+UseSerialGC
  ```
  输出结果：
  ```
  -XX:InitialHeapSize=5242880 -XX:MaxHeapSize=20971520 -XX:+PrintCommandLineFlags -XX:+PrintGCDetails -XX:+UseCompressedClassPointers -XX:+UseCompressedOops -XX:-UseLargePagesIndividualAllocation -XX:+UseSerialGC 
  Connected to the target VM, address: '127.0.0.1:14432', transport: 'socket'
  [GC (Allocation Failure) [DefNew: 1654K->192K(1856K), 0.0024365 secs] 1654K->689K(5952K), 0.0025023 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
  Max  Memory : 20316160 bytes
  Free  Memory: 4976232 bytes
  Total Memory: 6094848 bytes
  
  分配了 1M 空间给数组 bytes
  Max  Memory : 20316160 bytes
  Free  Memory: 3927640 bytes
  Total Memory: 6094848 bytes
  
  [GC (Allocation Failure) [DefNew: 1618K->35K(1856K), 0.0013357 secs][Tenured: 1689K->1724K(4096K), 0.0021330 secs] 2116K->1724K(5952K), [Metaspace: 3045K->3045K(1056768K)], 0.0035423 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
  分配了 4M 空间给数组 bytes
  Max  Memory : 20316160 bytes
  Free  Memory: 4310296 bytes
  Total Memory: 10358784 bytes
  
  Heap
   def new generation   total 1920K, used 120K [0x00000000fec00000, 0x00000000fee10000, 0x00000000ff2a0000)
    eden space 1728K,   6% used [0x00000000fec00000, 0x00000000fec1e360, 0x00000000fedb0000)
    from space 192K,   0% used [0x00000000fedb0000, 0x00000000fedb0000, 0x00000000fede0000)
    to   space 192K,   0% used [0x00000000fede0000, 0x00000000fede0000, 0x00000000fee10000)
   tenured generation   total 8196K, used 5820K [0x00000000ff2a0000, 0x00000000ffaa1000, 0x0000000100000000)
     the space 8196K,  71% used [0x00000000ff2a0000, 0x00000000ff84f338, 0x00000000ff84f400, 0x00000000ffaa1000)
   Metaspace       used 3054K, capacity 4556K, committed 4864K, reserved 1056768K
    class space    used 322K, capacity 392K, committed 512K, reserved 1048576K
  ```
  当前最大内存由`-XX:MaxHeapSize=209711520`指定，正好是`20*1024*1024=20971520`字节。输出的最大可用内存仅为20316160字节，比设定值略少。

  这是因为分配给堆的内存空间和实际可用内存空间并非一个概念。由于垃圾回收的需要，虚拟机会对堆空间进行分区管理，不同的区域采用不同的回收算法，一些算法会使用空间换时间的策略工作，因此会存在可用内存的损失。实际可用内存会浪费大小等于 from/to 的空间。

  from 大小为`0x00000000fede0000 - 0x00000000fedb0000 = 0x0000000000030000 = 196608`字节，`20971520 - 196608 = 20774912 ≠ 20136160`。出现偏差是由于虚拟机内部没有直接使用新生代 from/to 的大小，进一步对它们做了对其操作。

  **在实际工作中，可以直接将堆初始值与最大值设为相同值，以便减少程序运行时进行的垃圾回收次数，提高程序的性能。**

#### 3.2.2 新生代的配置
  通过参数`-Xmn`设置新生代大小，一般设置成整个堆空间的1/4到1/3。`-XX:SurvivorRatio`设置新生代中 eden 空间和 from/to 空间的比例关系。

  新生代配置：[NewSizeDemo](../java/com/ibgdn/chapter_3/NewSizeDemo.java)

- -Xmn1m -XX:SurvivorRatio=2
  VM options：
  ```
  -Xmx20m -Xms20m -Xmn1m -XX:SurvivorRatio=2 -XX:+PrintGCDetails
  ```
  输出结果：
  ```
  [GC (Allocation Failure) [PSYoungGen: 509K->504K(1024K)] 509K->504K(19968K), 0.0010014 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
  [GC (Allocation Failure) [PSYoungGen: 1016K->488K(1024K)] 1016K->576K(19968K), 0.0013428 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
  [GC (Allocation Failure) [PSYoungGen: 1000K->504K(1024K)] 1088K->708K(19968K), 0.0008333 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
  Heap
   PSYoungGen      total 1024K, used 1004K [0x00000000ffe80000, 0x0000000100000000, 0x0000000100000000)
    eden space 512K, 97% used [0x00000000ffe80000,0x00000000ffefcff0,0x00000000fff00000)
    from space 512K, 98% used [0x00000000fff00000,0x00000000fff7e010,0x00000000fff80000)
    to   space 512K, 0% used [0x00000000fff80000,0x00000000fff80000,0x0000000100000000)
   ParOldGen       total 18944K, used 10444K [0x00000000fec00000, 0x00000000ffe80000, 0x00000000ffe80000)
    object space 18944K, 55% used [0x00000000fec00000,0x00000000ff633210,0x00000000ffe80000)
   Metaspace       used 3049K, capacity 4556K, committed 4864K, reserved 1056768K
    class space    used 322K, capacity 392K, committed 512K, reserved 1048576K
  ```
  Eden 区无法容纳任何一次循环中分配的1MB数组，就会触发一次新生代 GC，对 Eden 区进行部分回收。偏小的新生代无法为1MB数组预留空间，所有数组都会分配到老年代（10444KB）。

  没有看到 Eden 和 From/To 按照设置的比例划分。原因在于 Jdk 1.8 默认使用 UseParallelGC 垃圾回收器，该垃圾回收器默认启动了 AdaptiveSizePolicy（自适应大小策略），如果开启了参数：`-XX:+UseAdaptiveSizePolicy`，则每次 GC 后会重新计算 Eden、From 和 To 区的大小。计算依据是 GC 过程中统计的 GC 时间、吞吐量、内存占用量。

- -Xmn7m -XX:SurvivorRatio=2
  VM options：
  ```
  -Xmx20m -Xms20m -Xmn7m -XX:SurvivorRatio=2 -XX:+PrintGCDetails
  ```
  输出结果：
  ```
  [GC (Allocation Failure) [PSYoungGen: 3172K->1520K(5632K)] 3172K->1764K(18944K), 0.0014965 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
  [GC (Allocation Failure) [PSYoungGen: 4671K->1504K(5632K)] 4916K->1780K(18944K), 0.0022548 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
  [GC (Allocation Failure) [PSYoungGen: 4728K->1520K(5632K)] 5005K->1796K(18944K), 0.0009145 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
  Heap
   PSYoungGen      total 5632K, used 4706K [0x00000000ff900000, 0x0000000100000000, 0x0000000100000000)
    eden space 4096K, 77% used [0x00000000ff900000,0x00000000ffc1c9e8,0x00000000ffd00000)
    from space 1536K, 98% used [0x00000000ffd00000,0x00000000ffe7c020,0x00000000ffe80000)
    to   space 1536K, 0% used [0x00000000ffe80000,0x00000000ffe80000,0x0000000100000000)
   ParOldGen       total 13312K, used 276K [0x00000000fec00000, 0x00000000ff900000, 0x00000000ff900000)
    object space 13312K, 2% used [0x00000000fec00000,0x00000000fec45330,0x00000000ff900000)
   Metaspace       used 3049K, capacity 4556K, committed 4864K, reserved 1056768K
    class space    used 322K, capacity 392K, committed 512K, reserved 1048576K
  ```

  Eden 区有足够的空间，所有数组首先分配在 Eden 区；但是 Eden 区不足以放下全部10M空间，程序运行时出现了3次新生代 GC。

  程序每申请一次内存空间，都会废弃上一次申请的内存空间（上次申请的内存空间失去了引用），在新生代 GC 时，有效回收了失效的内存。

  最终结果：所有的内存分配都在新生代进行，通过 GC 保证了新生代有足够的空间，老年代没有为数组预留任何空间，只是在 GC 过程中，部分新生代对象晋升到了老年代。

- -Xmn16m -XX:SurvivorRatio=8
  VM options：
  ```
  -Xmx20m -Xms20m -Xmn16m -XX:SurvivorRatio=8 -XX:+PrintGCDetails
  ```
  输出结果：
  ```
  Heap
   PSYoungGen      total 14848K, used 13000K [0x00000000ff000000, 0x0000000100000000, 0x0000000100000000)
    eden space 13312K, 97% used [0x00000000ff000000,0x00000000ffcb2318,0x00000000ffd00000)
    from space 1536K, 0% used [0x00000000ffe80000,0x00000000ffe80000,0x0000000100000000)
    to   space 1536K, 0% used [0x00000000ffd00000,0x00000000ffd00000,0x00000000ffe80000)
   ParOldGen       total 4096K, used 0K [0x00000000fec00000, 0x00000000ff000000, 0x00000000ff000000)
    object space 4096K, 0% used [0x00000000fec00000,0x00000000fec00000,0x00000000ff000000)
   Metaspace       used 3048K, capacity 4556K, committed 4864K, reserved 1056768K
    class space    used 322K, capacity 392K, committed 512K, reserved 1048576K
  ```

  新生代使用16MB内存空间，Eden 区占用了14848KB，完全满足10MB数组空间分配，所有空间分配行为都在 Eden 区进行，没有 GC 行为。From/To 区和老年代 ParOldGen 的使用率都是0。

  内存空间的基本设置策略：尽可能将对象预留在新生代，减少老年代 GC 的次数（如果对象大部分分配在老年代，后续会有 GC）。

- -XX:NewRatio
  使用参数`-XX:NewRatio`可以设置新生代和老年代的比例。

  VM options：
  ```
  -Xmx20m -Xms20m -XX:NewRatio=2 -XX:+PrintGCDetails
  ```
  输出结果：
  ```
  [GC (Allocation Failure) [PSYoungGen: 5210K->480K(6144K)] 5210K->1776K(19968K), 0.0016938 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
  [GC (Allocation Failure) [PSYoungGen: 5822K->496K(6144K)] 7119K->2816K(19968K), 0.0012217 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
  Heap
   PSYoungGen      total 6144K, used 2703K [0x00000000ff980000, 0x0000000100000000, 0x0000000100000000)
    eden space 5632K, 39% used [0x00000000ff980000,0x00000000ffba7c38,0x00000000fff00000)
    from space 512K, 96% used [0x00000000fff80000,0x00000000ffffc010,0x0000000100000000)
    to   space 512K, 0% used [0x00000000fff00000,0x00000000fff00000,0x00000000fff80000)
   ParOldGen       total 13824K, used 2320K [0x00000000fec00000, 0x00000000ff980000, 0x00000000ff980000)
    object space 13824K, 16% used [0x00000000fec00000,0x00000000fee44360,0x00000000ff980000)
   Metaspace       used 3049K, capacity 4556K, committed 4864K, reserved 1056768K
    class space    used 322K, capacity 392K, committed 512K, reserved 1048576K
  ```

  堆空间设置为20MB，新生代和老年代的分配比为：1:2，新生代空间大小`20MB/3=6MB`左右，老年代13MB左右。

  由于新生代 GC 时， From/To 区不足以容纳任何一个1MB数组，影响了新生代的正常回收，故在新生代回收时需要老年代空间，导致2个1MB数组进入老年代（在新生代 GC 时，尚有1MB数组幸存，理应进入 From/To 区，而 From/To 区只有512KB，不足以容纳）。

#### 3.2.3 堆溢出处理
  Java 程序运行过程中，如果堆空间不足，就会抛出堆内存溢出错误（Out Of Memory，简称 OOM）。

  参数`-XX:+HeapDumpOnOutOfMemoryError`可在堆堆内存溢出时导出整个堆信息；参数`-XX:HeapDumpPath`指定导出堆内存记录内容的存放路径。

  堆内存溢出信息记录：[DumpOOM](../java/com/ibgdn/chapter_3/DumpOOM.java)

- 堆内存溢出时导出信息
  VM options：
  ```
  -Xmx20m -Xms5m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=D:/oom.dump
  ```

  输出结果：
  ```
  java.lang.OutOfMemoryError: Java heap space
  Dumping heap to D:/oom.dump ...
  Heap dump file created [15091420 bytes in 0.060 secs]
  Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
  	  at com.ibgdn.chapter_3.DumpOOM.main(DumpOOM.java:16)
  ```
- 堆内存溢出时执行脚本并导出信息
  除了在发生 OOM 时可以导出堆信息，虚拟机还允许在发生错误时执行一个脚本文件，用于崩溃程序的自救、报警或通知，帮助开发人员获得更多的系统信息。

  VM options：
  ```
  -Xmx20m -Xms5m "-XX:OnOutOfMemoryError=Path/to/jdk/script/file %p" -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=D:/oom.dump
  ```