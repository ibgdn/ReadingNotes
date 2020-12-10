### 6.1 有我更高效：Linux 下的性能监控工具
#### 6.1.1 显示系统整体资源使用情况——top 命令
  ```
  top - 11:00:54 up 54 days, 23:35,  6 users,  load average: 16.32, 18.75, 21.04
  Tasks: 209 total,   3 running, 205 sleeping,   0 stopped,   1 zombie
  %Cpu(s): 29.7 us, 18.9 sy,  0.0 ni, 49.3 id,  1.7 wa,  0.0 hi,  0.4 si,  0.0 st
  KiB Mem : 32781216 total,  1506220 free,  6525496 used, 24749500 buff/cache
  KiB Swap:        0 total,        0 free,        0 used. 25607592 avail Mem 
  
    PID USER      PR  NI    VIRT    RES    SHR S  %CPU %MEM     TIME+ COMMAND                                                                                                                                                                  
  23444 root      20   0   15.6g 461676   4704 R 198.0  1.4  11:15.26 python                                                                                                                                                                   
  16729 root      20   0 9725596 240028   4672 R 113.0  0.7   7:48.49 python                                                                                                                                                                   
   3388 root      20   0 6878028 143196   4720 S  82.4  0.4   1:35.03 python 
  ```
  top 命令的输出分为两部分：前半部分系统信息统计，后半部分进程信息。
  1. 统计信息
  - 第一行任务队列信息，结果等同于 uptime 命令。
    从左到右依次表示：系统当前时间、系统运行时间、当前登录用户数、系统平均负载（任务队列的平均长度，1、5、15分钟到当前时间的平均值）。
    平均负载表示的平均活跃进程数，包括正在running的进程数，准备running（就绪态）的进程数，和处于不可中断睡眠状态的进程数。如果平均负载数刚好等于CPU核数，那证明每个核都能得到很好的利用，如果平均负载数大于核数证明系统处于过载的状态，通常认为是超过核数的70%认为是严重过载，需要关注。还需结合1分钟平均负载，5分钟平均负载，15分钟平均负载看负载的趋势，如果1分钟负载比较高，5分钟和15分钟的平均负载都比较低，则说明是瞬间升高，需要观察。如果三个值都很高则需要关注下是否某个进程在疯狂消耗CPU或者有频繁的IO操作，也有可能是系统运行的进程太多，频繁的进程切换导致。比如说上面的演示环境是一台8核的centos机器，证明系统是长期处于过载状态在运行。
  - 第二行进程统计信息。
    从左到右依次表示：正在运行的进程数、睡眠进程数、停止的进程数、僵尸进程数。
    僵尸进程，子进程结束时父进程没有调用wait()/waitpid()等待子进程结束，那么就会产生僵尸进程。原因是子进程结束时并没有真正退出，而是留下一个僵尸进程的数据结构在系统进程表中，等待父进程清理，如果父进程已经退出则会由init进程接替父进程进行处理（收尸）。由此可见，如果父进程不作为并且又不退出，就会有大量的僵尸进程，每个僵尸进程会占用进程表的一个位置（slot），如果僵尸进程太多会导致系统无法创建新的进程，因为进程表的容量是有限的。所以当zombie这个指标太大时需要引起我们的注意。下面的进程详细信息中的S列就代表进程的运行状态，Z表示该进程是僵尸进程。
    消灭僵尸进程的方法：
    1. 找到僵尸进程的父进程pid（pstress可以显示进程父子关系），kill -9 pid，父进程退出后init自动会清理僵尸进程。（需要注意的是kill -9并不能杀死僵尸进程）
    2. 重启系统。
  - 第三行 CPU 统计信息。
    us（user） 表示用户空间 CUP 占用率，sy（system） 表示内核空间 CPU 占用率，ni（nice） 表示用户进程空间改变过优先级的进程 CUP 使用率，id（idle） 表示空闲 CPU 占用率，wa（io wait） 表示等待输入输出的 CPU 时间百分比，hi（hard interrupt） 表示硬件中断请求，si（soft interrupt） 表示软件中断请求，st（steal）当前系统运行在虚拟机中的时候，被其他虚拟机占用 CPU 的时间比例。
    所以整体的CPU使用率=1-id。当us很高时，证明CPU时间主要消耗在用户代码，需要优化用户代码。sy很高时，说明CPU时间都消耗在内核，要么是频繁的系统调用，要么是频繁的CPU切换（进程切换/线程切换）。wa很高时，说明有进程在进程频繁的IO操作，有可能是磁盘IO，也有可能是网络IO。si很高时，说明CPU时间消耗在处理软中断，网络收发包会触发系统软中断，所以大量的网络小包会导致软中断的频繁触发，典型的SYN Floor会导致si很高。
  - 第四行 Mem 内存信息
    从左到右依次表示：物理内存总量，空闲物理内存，已使用物理内存，内核缓冲（读写文件缓存的内存）使用量。
  - 第五行 Swap 交换区信息
    从左到右依次表示：交换区总量，空闲交换区大小，已使用交换区大小，可用的应用内存大小。

  2. 进程信息区
  显示系统各个进程的资源使用情况。主要字段包括：
  - PID：进程 id
  - USER：进程所有者的用户名
  - PR：优先级
  - NI：nice 值，负值表示高优先级，正值表示低优先级
  - VIRT：进程使用的虚拟内存总量，单位 kb，VIRT=SWAP+RES
  - RES：进程使用的、未被换出的物理内存（不包括共享内存），单位 kb，RES=CODE+DATA
  - SHR：进程使用的共享内存，单位 kb
  - %CUP：上次更新到现在的 CPU 时间占用百分比
  - %MEM：进程使用的内存占比
  - TIME+：进程使用的 CPU 时间总计，单位1/100秒
  - COMMAND：命令名/命令行

#### 6.1.2 监控内存和 CPU——vmstat 命令
  vmstat 是一款功能比较齐全的性能监测工具，可以统计 CPU、内存、swap 使用情况。可以和 sar 工具一样指定采样周期和采样次数。

  ```
  procs -----------memory---------- ---swap-- -----io---- --system-- -----cpu-----
   r b   swpd  free    buff   cache   si  so   bi  bo  in  cs  us  sy id   wa  st
   0 0   424   356568  0      159860  0   0    0   0   0   4   0   0  100  0   0
   0 0   424   356564  0      159856  0   0    0   0   0   8   0   0  100  0   0
   0 0   424   356564  0      159856  0   0    0   0   0   9   0   0  100  0   0
  ```
  vmstat 命令输出含义
  类型|参数
  :--:|:--
  Procs|r：等待运行的进程数<br>b：处在非中断睡眠状态的进程数
  Memory|swpd：虚拟内存使用情况，单位：KB<br>free：空闲的内存，单位：KB<br>buff：被用来作为缓存的内存数，单位：KB
  Swap|si：从磁盘交换到内存的交换页数量，单位：KB/秒<br>so：从内存交换到磁盘的交换页数量，单位：KB/秒
  IO|bi：发送到块设备的块数，单位：块/秒<br>bo：从块设备接收到的块数，单位：块/秒
  System|in：每秒的中断数，包括时钟中断<br>cs：每秒的上下文切换次数
  CPU|us：用户 CPU 使用时间<br>sy：内核 CPU 系统使用时间<br>id：空闲时间

  HoldLockMain：[HoldLockMain](../java/com/ibgdn/chapter_6/HoldLockMain.java)

  linux 环境下执行：
  ```
  [red@redhat8 ~]$ vmstat 1 4
  ```
  输出结果中，in（每秒中断数）、cs（上下文切换）值和 us（用户 CPU 时间）值，表明系统的上下文切换频繁，用户 CPU 占用率很高。
  
#### 6.1.3 监控 IO 使用——iostat 命令
  iostat 命令可以提供详细的 I/O 信息，属于 sysstat 软件包，可以直接安装。

  linux 环境下执行（每1秒采样1次，合计采样2次）：
  ```
  [red@redhat8 ~]$ iostat 1 2
  ```

  只显示磁盘情况，不显示 CPU 使用情况，可以执行命令：
  ```
  [red@redhat8 ~]$ iostat -d 1 2
  ```

  如果需要更多统计信息，可以添加`-x`选项，执行命令：
  ```
  [red@redhat8 ~]$ iostat -x 1 2
  ```

  ```
  [red@redhat8 ~]$ iostat
  Linux 2.6.32-279.19.3.el6.ucloud.x86_64 (vm1)   06/11/2017  _x86_64_    (8 CPU)

  avg-cpu:  %user   %nice %system %iowait  %steal   %idle
           0.08    0.00    0.06    0.00    0.00   99.86

  Device:            tps   Blk_read/s   Blk_wrtn/s   Blk_read   Blk_wrtn
  vda               0.45         0.29         8.10    6634946  183036680
  vdb               0.12         3.11        30.55   70342034  689955328
  ```

  参数|说明
  :--:|:--
  %user|CPU处在用户模式下的时间百分比。
  %nice|CPU处在带NICE值的用户模式下的时间百分比。
  %system|CPU处在系统模式下的时间百分比。
  %iowait|CPU等待输入输出完成时间的百分比。
  %steal|管理程序维护另一个虚拟处理器时，虚拟CPU的无意识等待时间百分比。
  %idle|CPU空闲时间百分比。
  tps|该设备每秒传输次数
  Blk_read/s|每秒从设备读取的数据量
  Blk_wrtn/s|每秒向设备写入的数据量
  Blk_read|读取的总数据量
  Blk_wrtn|写入的总数据量

  **注意**
  - 如果 %iowait 的值过高，表示硬盘存在 I/O 瓶颈。
  - 如果 %idle 值高，表示 CPU 较空闲。
  - 如果 %idle 值高但系统响应慢时，可能是 CPU 等待分配内存，应加大内存容量。
  - 如果 %idle 值持续低于10，表明 CPU 处理能力相对较低，系统中最需要解决的资源是 CPU。

#### 6.1.4 多功能诊断器——pidstat 工具
  pidstat 是一款功能强大的性能监测工具，也是 Sysstat 的组件之一。

  下载源码后可执行如下命令完成编译、安装：
  ```
  [red@redhat8 ~]$ ./configure
  [red@redhat8 ~]$ make
  [red@redhat8 ~]$ make install
  ```

  1. CPU 使用率监控

  简单的占用 CPU 的程序，开启4个用户线程，1个占用大量 CPU 资源，其他3个处于空闲状态。
  HoldCPUMain：[HoldCPUMain](../java/com/ibgdn/chapter_6/HoldCPUMain.java)

  监视程序 CPU 的使用率，jps 查看 Java 程序的 PID，pidstat 查看 CPU 的使用情况
  ```
  [red@redhat8 ~]$ jps
  [red@redhat8 ~]$ pidstat -p HoldCPUMain PID -u 1 3
  ```

  添加 -t 参数将系统性能的监控细化到线程级别。
  ```
  [red@redhat8 ~]$ pidstat -p HoldCPUMain PID -u 1 3 -t
  01:47:30 PM   TGID    TID     %usr    %system     %guest  %CPU    CPU     Command
  ...
  01:47:31 PM   -       1204    97.03   0.00        0.00    97.03   1       |__java
  ```

  输出内容到文件
  ```
  [red@redhat8 ~]$ jstack -l PID > file.txt
  ```

  ```
  "Thread-0" prio=10 tid=0xb75b3000 nid=0x4b4 runnable [0x8f171000]
    java.lang.Thread.State: RUNNABLE
     at javatuning.ch6.toolscheck.HoldCPUMain$HoldCPUTask.run(HoldCPUMain.java:7)
  ```
  线程正是 HoldCPUTask 类，它的 nid（native ID）为0x4b4，转为10进制数字后刚好是线程的 TID 值。

  2. I/O 使用监控

  磁盘 I/O 也是常见的性能瓶颈之一，pidstat 也可以监控进程内线程的 I/O 情况。
  HoldIOMain：[HoldIOMain](../java/com/ibgdn/chapter_6/HoldIOMain.java)

  通过 jps 命令查询进程 ID，-d 监控对象为磁盘 I/O，1 3 每秒钟采样1次，合计采样3次。
  ```
  [red@redhat8 ~]$ pidstat -p HoldIOMain PID -d -t 1 3
  06:06:00 PM   TGID    TID     kB_rd/s kB_wr/s     kB_ccwr/s   Command
  ...
  06:06:01 PM   -       22813   0.00    328.00      0.00        |__java
  ```
  进程22813（0x591D）线程产生了大量 I/O 操作。

  3. 内存监控

  使用 pidstat 命令，可以监控指定进程的内存使用情况。
  ```
  [red@redhat8 ~]$ pidstat -r -p 27233 1 5
  09:50:32 AM   PID     minflt/s    majflt/s    VSZ     RSS     %MEM    Command
  09:50:33 AM   27233   0.00        0.00        728164  11476   0.55    java
  09:50:34 AM   27233   1.00        0.00        728164  11480   0.55    java
  ...
  09:50:37 AM   27233   0.99        0.00        728164  11492   0.55    java
  Average:      27233   0.80        0.00        728164  11484   0.55    java
  ```
  参数|说明
  :--:|:--
  minflt/s|该进程每秒 minor faults（不需要从磁盘中调出内存页）的总数
  majflt/s|该进程每秒 major faults（需要从磁盘中调出内存页）的总数
  VSZ|该进程使用的虚拟内存大小，单位 KB
  RSS|该进程占用的物理内存大小，单位 KB
  %MEM|占用内存比率

### 6.2 用我更高效：Windows 下的性能监控工具
#### 6.2.2 perfmon 性能监控工具
  Perfmon（性能监视器）是 Windows 的专业级性能监控工具。
  
  启动方式：运行 =》 perfmon 或者 控制面板 =》 性能监视器
  
  在性能监视器窗口点击右键 =》 添加计数器。重点关注 Thread 监控项下的内容：User Time（线程占用 CPU 的时间百分比）、ID Thread（线程 ID）、ID Process（进程 ID）。
  
  运行[HoldCPUMain](../java/com/ibgdn/chapter_6/HoldCPUMain.java)，设置监控对象为 Thread，并选择 java 进程中所有线程；选择右侧“更改图形类型”选择“报告”，会发现某一线程占用了很高的 CPU。通过任务管理找到 PID（36160），使用 JDK 自带的工具生成线程快照`jdkPath\bin>jstack.exe -l 36160 > dmp.txt`，将性能监视器中 Thread ID（33820）换算成16进制为841C，在生成的文件中[chapter-6_dmp.txt](./chapter-6_dmp.txt)查找`nid=0x841c`的线程，即可定位 Java 程序中消耗 CPU 最多的线程代码。
  
  运行中键入`perfmon /res` 命令，用于监控系统资源的使用情况。

#### 6.2.3 Process Explorer 进程管理工具
  Process Explorer 是一款功能强大的进程管理工具。[下载地址](https://docs.microsoft.com/en-us/sysinternals/downloads/process-explorer)

  运行[HoldLockMain](../java/com/ibgdn/chapter_6/HoldLockMain.java)，java.exe 占有很高 CPU 使用率。选中进程，点击右键查看属性。点击 Threads 选项卡，可以看到当前进程中的线程信息。

#### 6.2.4 pslist 命令——Windows 下也有命令行工具
  pslist 是一款 Windows 下的命令行工具。[下载地址](https://docs.microsoft.com/en-us/sysinternals/downloads/pslist)

  pslist 基本语法：
  ```
  pslist [-d] [-m] [-x] [-t] [-s[n]] [-r n] [name|pid]
  ```
  参数|说明
  :--:|:--
  -d|显示线程详细信息
  -m|显示内存详细信息
  -x|显示进程、内存和线程信息
  -t|显示进程间父子关系
  -s[n]|进入监控模式，n 指定程序运行时间，Esc 退出
  -r n|指定监控模式下的刷新时间，单位：秒
  name|指定监控的进程名称，pslist 将监控所有以给定名字开头的进程
  pid|指定进程 ID
  -e|使用精确匹配，打开这个开关，pslist 将只监控 name 参数指定的进程

  运行[HoldCPUMain](../java/com/ibgdn/chapter_6/HoldCPUMain.java)，列出所有 Java 应用程序进程：
  ```
  PSTools>pslist java
  
  PsList v1.4 - Process information lister
  Copyright (C) 2000-2016 Mark Russinovich
  Sysinternals - www.sysinternals.com
  
  Process information for DESKTOP-35SMKNN:
  
  Name                Pid Pri Thd  Hnd   Priv        CPU Time    Elapsed Time
  java              33008   8  29  444 809652     0:00:03.484     0:00:44.924
  java              36052   8  28  333 851296     0:00:45.187     0:00:44.900
  ```

  列出线程信息：
  ```
  PSTools>pslist java -d
  
  PsList v1.4 - Process information lister
  Copyright (C) 2000-2016 Mark Russinovich
  Sysinternals - www.sysinternals.com
  
  Thread detail for DESKTOP-35SMKNN:
  
  
  java 36052:
   Tid Pri    Cswtch            State     User Time   Kernel Time   Elapsed Time
  35436   9        67     Wait:UserReq  0:00:00.000   0:00:00.031    0:01:52.348
  33500   9       180     Wait:UserReq  0:00:00.093   0:00:00.031    0:01:52.285
  40496   9         4     Wait:UserReq  0:00:00.000   0:00:00.000    0:01:52.269
  26588   9         4     Wait:UserReq  0:00:00.000   0:00:00.000    0:01:52.269
  33388   9         4     Wait:UserReq  0:00:00.000   0:00:00.000    0:01:52.269
  24060   9         4     Wait:UserReq  0:00:00.000   0:00:00.000    0:01:52.269
  33012   9         5     Wait:UserReq  0:00:00.000   0:00:00.000    0:01:52.269
  29872   9         4     Wait:UserReq  0:00:00.000   0:00:00.000    0:01:52.269
  4280   9         5     Wait:UserReq  0:00:00.000   0:00:00.000    0:01:52.269
  24284   9         6     Wait:UserReq  0:00:00.000   0:00:00.000    0:01:52.269
  31576  10       117     Wait:UserReq  0:00:00.000   0:00:00.000    0:01:52.254
  36400  11         4     Wait:UserReq  0:00:00.000   0:00:00.000    0:01:52.249
  35064  10         4     Wait:UserReq  0:00:00.000   0:00:00.000    0:01:52.247
  6936  10         2     Wait:UserReq  0:00:00.000   0:00:00.000    0:01:52.188
  38196  12        11     Wait:UserReq  0:00:00.031   0:00:00.000    0:01:52.188
  11620   9        57     Wait:UserReq  0:00:00.015   0:00:00.000    0:01:52.156
  33564  10        98     Wait:UserReq  0:00:00.000   0:00:00.000    0:01:52.156
  38632  11        99     Wait:UserReq  0:00:00.000   0:00:00.000    0:01:52.156
  2116  10        97     Wait:UserReq  0:00:00.000   0:00:00.000    0:01:52.156
  25008  10        59     Wait:UserReq  0:00:00.046   0:00:00.000    0:01:52.156
  39708   9         6     Wait:UserReq  0:00:00.000   0:00:00.000    0:01:52.153
  35304  10      2255     Wait:UserReq  0:00:00.000   0:00:00.000    0:01:52.148
  40892   8     33302          Running  0:01:52.828   0:00:00.046    0:01:52.106
  32352   8       120     Wait:UserReq  0:00:00.000   0:00:00.000    0:01:52.105
  11720   8       119     Wait:UserReq  0:00:00.000   0:00:00.000    0:01:52.105
  31124   8       119     Wait:UserReq  0:00:00.000   0:00:00.000    0:01:52.105
  
  java 23532:
   Tid Pri    Cswtch            State     User Time   Kernel Time   Elapsed Time
  36456   9        12     Wait:UserReq  0:00:00.000   0:00:00.015    0:00:18.823
  37752   8        14       Wait:Queue  0:00:00.000   0:00:00.000    0:00:18.769
  40404   8        22       Wait:Queue  0:00:00.000   0:00:00.000    0:00:18.768
  37352   9         5       Wait:Queue  0:00:00.000   0:00:00.000    0:00:18.768
  12312   8       942     Wait:UserReq  0:00:01.015   0:00:00.312    0:00:18.758
  16612   9        70     Wait:UserReq  0:00:00.015   0:00:00.015    0:00:18.746
  39004   9        37     Wait:UserReq  0:00:00.031   0:00:00.000    0:00:18.746
  39920   9       119     Wait:UserReq  0:00:00.031   0:00:00.000    0:00:18.746
  31536   9        35     Wait:UserReq  0:00:00.015   0:00:00.000    0:00:18.746
  22100   9       123     Wait:UserReq  0:00:00.015   0:00:00.000    0:00:18.746
  31916   9       118     Wait:UserReq  0:00:00.015   0:00:00.000    0:00:18.746
  21548   9        39     Wait:UserReq  0:00:00.031   0:00:00.000    0:00:18.746
  33928   9       105     Wait:UserReq  0:00:00.031   0:00:00.000    0:00:18.745
  36308  10        33     Wait:UserReq  0:00:00.000   0:00:00.000    0:00:18.728
  34160  11         5     Wait:UserReq  0:00:00.000   0:00:00.000    0:00:18.724
  36780  10         5     Wait:UserReq  0:00:00.000   0:00:00.000    0:00:18.722
  22568  10         2     Wait:UserReq  0:00:00.000   0:00:00.000    0:00:18.680
  40736  11         3     Wait:UserReq  0:00:00.000   0:00:00.000    0:00:18.680
  29900  10       765     Wait:UserReq  0:00:00.718   0:00:00.031    0:00:18.680
  37004  10       785     Wait:UserReq  0:00:00.218   0:00:00.000    0:00:18.679
  38040  10       748     Wait:UserReq  0:00:00.281   0:00:00.000    0:00:18.679
  33544  10       474     Wait:UserReq  0:00:00.359   0:00:00.000    0:00:18.679
  40604   8         2     Wait:UserReq  0:00:00.000   0:00:00.000    0:00:18.678
  36660  10       400     Wait:UserReq  0:00:00.000   0:00:00.000    0:00:18.675
  40524   9        15     Wait:UserReq  0:00:00.031   0:00:00.031    0:00:17.934
  22652  11         3       Wait:Queue  0:00:00.000   0:00:00.000    0:00:17.910
  24428   8        31     Wait:UserReq  0:00:00.000   0:00:00.000    0:00:17.616
  38864   9        99     Wait:UserReq  0:00:00.031   0:00:00.000    0:00:17.608
  ```
  正在运行的具有较高 Cswtch 上下文切换值的线程 40892。换算成16进制数字查询 dmp 文件，可找到对应相应程序代码。