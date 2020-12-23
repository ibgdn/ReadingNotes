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

### 6.3 外科手术刀：JDK 性能监控工具
  java.exe、javac.exe 从扩展名来看都是可执行文件，其实只是 Java 程序的一层包装，真正实现实在 tools.jar 中。以 jps 工具为例，在控制台执行`jps`命令和`java -classpath %JAVA_HOME%/lib/tools.jar/sun.tools.jps.Jps`命令是等价的。

#### 6.3.1 查看 Java 进程——jps 命令
  jps 类似 Linux 系统下达的 ps 命令，只是列出 Java 程序进程 ID 以及 Main 方法短名称（第一行输出的是 Jps 即 jps 命令本身）：
  ```
  > jps
  6260 Jps
  7988 Main
  400
  ```
  添加`-q`参数只输出进程 ID：`jps -q`

  添加`-m`参数可以用于输出传递给 Java 进程（主方法）：
  ```
  > jps -m
  7988 Main --log-config-file D:\tools\squirrel-sql-3.2.1\log4j.properties --squirrel-home D:\tools\squirrel-sql-3.2.1
  7456 Jps -m
  ```

  添加`-l`参数可以用于输出主方法的完整路径：
  ```
  > jps -m -l
  7244 sun.tools.jps.Jps -m -l
  7988 net.sourceforge.squirrel_sql.client.Main --log-config-file D:\tools\squirrel-sql-3.2.1\log4j.properties --squirrel-home D:\tools\squirrel-sql-3.2.1
  ````

  添加`-v`参数可以显示传递给 Java 虚拟机的参数：
  ```
  > jps -m -l -v
  6992 sun.tools.jps.Jps -m -l -v -Denv.class.path=.;D:\tools\jdk6.0\lib\dt.jar;D:\tools\jdk6.0\lib\tools.jar;D:\tools\jdk6.0\lib -Dapplication.home=D:\tools\jdk6.0 -Xms8m
  7988 net.sourceforge.squirrel_sql.client.Main --log-config-file D:\tools\squirrel-sql-3.2.1\log4j.properties --squirrel-home D:\tools\squirrel-sql-3.2.1 -Xmx256m -Dsun.java2d.noddraw=true
  ````

#### 6.3.2 查看虚拟机运行时信息——jstat 命令
  jstat 是一个可以用于观察 Java 应用程序运行时相关信息的工具。基本语法：`jstat -<option> [-t] [-h<lines>] <vmid> [<interval> [<count>]]`

  参数|说明
  :--:|:--
  option 选项|-class:显示 ClassLoader 的相关信息。<br>-compiler：显示 JIT 编译的相关信息。<br>-gc：显示与 GC 相关的堆信息。<br>-gccapacity：显示各个代的容量及使用情况。<br>-gccause：显示垃圾收集相关信息（同-gcutil），同时显示最后一次或当前正在发生的垃圾回收的诱发原因。<br>-gcnew：显示新生代信息。<br>-gcnewcapacity：显示新生代大小与使用情况。<br>-gcold：显示老年代和永久代的信息。<br>-gcoldcapacity：显示老年代的大小。<br>-gcpermcapacity：显示永久代的大小。<br>-gcutil：显示垃圾收集信息。<br>-printcompilation：输出 JIT 编译的方法信息。
  -t|可以在输出信息前加上一个 Timestamp 列，显示程序的运行时间。
  -h|可以在周期性数据输出时，输出多少行数据后，跟着输出一个表头信息。
  interval|用于指定输出统计数据的周期，单位毫秒。
  count|用于指定一共输出多少次数据。

  输出 ID 为2972的 Java 进程 ClassLoader 相关信息。每秒钟统计一次，共统计两次：
  ```
  > jstat -class -t 2972 1000 2
  Timestamp   Loaded  Bytes   Unloaded  Bytes Time
  1395.6      2375    2683.8  7         6.2   3.45
  1396.6      2375    2683.8  7         6.2   3.45
  ```
  `-class`输出中，Loaded 表示载入了类的数量，第1个 Bytes 表示载入类的合计大小，Unloaded 表示卸载类的数量，第2个 Bytes 表示卸载类的大小，Time 表示在加载和卸载类上所花的时间。

  查看 JIT 编译信息：
  ```
  > jstat -compiler -t 2972
  Timestamp   Compiled  Failed  Invalid Time  FailedType  FailedMethod
  1675.9      779       0       0       0.61  0
  ```
  Compiled 表示编译任务执行的次数，Failed 表示编译失败的次数，Invalid 表示编译不可用的次数，Time 表示编译的总耗时，FailedType 表示最后一次编译失败的类型，FailedMethod 表示最后一次编译失败的类名和方法名。

  GC 相关的堆信息输出：
  ```
  > jstat -gc 2972
  S0C   S1C   S0U   S1U   EC    EU    OC      OU      PC      PU      YGC   YGCT  FGC FGCT  GCT
  64.0  64.0  0.0   2.0   896.0 448.9 12312.0 9019.1  12288.0 9101.3  101   0.153 2   0.210 0.364
  ```
  参数|说明
  :--:|:--
  S0C|s0（From 区）的大小（KB）
  S1C|s1（From 区）的大小（KB）
  S0U|s0（From 区）已使用的空间大小（KB）
  S1U|s1（From 区）已使用的空间大小（KB）
  EC|Eden 区大小（KB）
  EU|Eden 区已使用的空间大小（KB）
  OC|老年代大小（KB）
  OU|老年代已使用的空间大小（KB）
  PC|永久区大小（KB）
  PU|永久区已使用的空间大小（KB）
  YGC|新生代 GC 次数
  YGCT|新生代 GC 耗时
  FGC|Full GC 次数
  FGCT|Full GC 耗时
  GCT|GC 总耗时

  ```
  > jstat -gccapacity 2972
  NGCMN   NGCMX   NGC     S0C   S1C   EC    OGCMN   OGCMX     OGC     OC      PGCMN   PGCMX   PGC     PC      YGC FGC
  1024.0  20160.0 1024.0  64.0  64.0  896.0 4096.0  241984.0  12312.0 12312.0 12288.0 65536.0 12288.0 12288.0 129 2
  ```
  参数|说明
  :--:|:--
  NGCMN|新生代最小值（KB）
  NGCMX|新生代最大值（KB）
  NGC|当前新生代大小（KB）
  OGCMN|老年代最小值（KB）
  OGCMX|老年代最大值（KB）
  PGCMN|永久代最小值（KB）
  PGCMX|永久代最大值（KB）

  最近一次 GC 的原因，以及当前 GC 的原因：
  ```
  > jstat -gccause 2972
  S0    S1    E     O     P     YGC YGCT  FGC FGCT  GCT   LGCC        GCC
  0.00  0.00  19.58 59.99 91.43 143 0.207 3   0.331 0.538 System.gc() No GC
  ```
  最近一次 GC 是由于显式的`System.gc()`调用所引起的，当前时刻未进行 GC。
  参数|说明
  :--:|:--
  LGCC|上次 GC 的原因
  GCC|当前 GC 的原因

  查看新生代详细信息：
  ```
  > jstat -gcnew 2972
  S0C    S1C    S0U S1U   TT  MTT DSS   EC      EU    YGC YGCT
  128.0  128.0  0.0 11.8  15  15  64.0  1024.0  139.8 159 0.223
  ```
  参数|说明
  :--:|:--
  TT|新生代对象晋升到老年代对象的年龄
  MTT|新生代对象晋升到老年代对象的年龄最大值
  DSS|所需的 Survivor 区大小

  详细输出新生代各个区的大小信息：
  ```
  > jstat -gcnewcapacity 2972
  NGCMN   NGCMX   NGC     S0CMX   S0C     S1CMX   S1C   ECMX    EC      YGC FGC
  1024.0  20160.0 1280.0  128.0   1984.0  1984.0  128.0 16192.0 1024.0  178 3
  ```
  参数|说明
  :--:|:--
  S0CMX|S0区的最大值（KB）
  S1CMX|S1区的最大值（KB）
  ECMX|Eden 区的最大值（KB）

  展现老年代 GC 的情况
  ```
  > jstat -gcold 2972
  PC      PU      OC      OU      YGC FGC FGCT  GCT
  12288.0 11295.6 15048.0 9106.1  190 3   0.331 0.580
  ```

  展现老年代容量信息
  ```
  > jstat -gcoldcapacity 2972
  OGCMN   OGCMX     OGC     OC      YGC FGC FGCT  GCT
  4096.0  241984.0  15048.0 15048.0 195 3   0.331 0.584
  ```

  展示永久代使用情况
  ```
  > jstat -gcpermcapacity 2972
  PGCMN   PGCMX   PGC     PC      YGC FGC FGCT  GCT
  12288.0 65536.0 12288.0 12288.0 220 3   0.331 0.605
  ```

  展示 GC 回收相关情况
  ```
  > jstat -gcutil 2972
  S0    S1    E     O     P     YGC YGCT  FGC FGCT  GCT 
  7.65  0.00  62.88 60.60 92.19 224 0.277 3   0.331 0.609
  ```
  参数|说明
  :--:|:--
  S0|S0区使用百分比
  S1|S1区使用百分比
  E|Eden 区使用百分比
  O|Old 区使用百分比
  P|永久区使用百分比

#### 查看虚拟机参数——jinfo 命令
  jinfo 用来查看正在运行的 Java 程序的扩展参数，支持在运行时修改部分参数。基本语法`jinfo <option> <pid>`

  option 可以是：
  参数|说明
  :--:|:--
  -flag <name>|打印指定虚拟机的参数值
  -flag [+\|-]<name>|设置指定 Java 虚拟机参数的布尔值
  -flag <name>=<value>|设置指定 Java 虚拟机参数的值

  很多情况下，Java 应用程序不会指定所有的 Java 虚拟机参数，通过查找文档获取某个参数的默认值。

  查看新生代对象晋升到老年代对象的最大年龄
  ```
  > jinfo -flag MaxTenuringThreshold 2972
  -XX:MaxTenuringThreshold=15
  ```

  显示是否打印 GC 详细信息
  ```
  > jinfo -flag PrintGCDetails 2972
  -XX:-PrintGCDetails
  ```

  修改 PrintGCDetails 参数，在 Java 程序运行时，动态关闭或打开这个开关。
  ```
  > jinfo -flag PrintGCDetails 2972
  -XX:-PrintGCDetails

  > jinfo -flag +PrintGCDetails 2972
  
  > jinfo -flag PrintGCDetails 2972
  -XX:+PrintGCDetails
  ```

#### 6.3.4 导出堆到文件——jmap 命令
  jmap 命令可以生成 Java 程序的堆 Dump 文件，可以查看堆内对象实例的统计信息、ClassLoader 信息以及 finalizer 队列。

  生成 Java 程序中 PID 为2972的对象统计信息
  ```
  > jmap -histo 2972 >C:\dump.txt
  ```

  获取 Java 程序的当前堆快照
  ```
  > jmap -dump:format=b,file=D:\heap.hprof 27316
  Dumping heap to D:\heap.hprof ...
  Heap dump file created
  ```

  查看系统 ClassLoader 信息
  ```
  >jmap -clstats 24716
  Attaching to process ID 24716, please wait...
  Debugger attached successfully.
  Server compiler detected.
  JVM version is 25.231-b11
  finding class loader instances ..done.
  computing per loader stat ..done.
  please wait.. computing liveness......................................liveness analysis may be inaccurate ...
  class_loader    classes bytes   parent_loader   alive?  type

  <bootstrap>     1423    2507574   null          live    <internal>
  0x00000005a042bec0      1830    3192630 0x00000005a043bc90      live    java/net/URLClassLoader@0x00000007c000ef80
  0x00000005a043bc90      1       1571    0x00000005a043bd00      live    sun/misc/Launcher$AppClassLoader@0x00000007c000f958
  0x00000005a043bd00      1       673       null          live    sun/misc/Launcher$ExtClassLoader@0x00000007c000fd00
  0x00000005a0445198      1       1472    0x00000005a042bec0      dead    sun/reflect/DelegatingClassLoader@0x00000007c000a0a0
  0x00000005a04464a8      1       1472    0x00000005a042bec0      dead    sun/reflect/DelegatingClassLoader@0x00000007c000a0a0
  0x00000005a07dec10      0       0       0x00000005a043bc90      live    java/util/ResourceBundle$RBClassLoader@0x00000007c0089098

  total = 7       3257    5705392     N/A         alive=5, dead=2     N/A
  ```

  查看系统 finalizer 队列中的对象。不恰当的 finalizer() 方法可能导致对象堆积在 finalizer 队列中。
  ```
  >jmap -finalizerinfo 24536
  Attaching to process ID 24536, please wait...
  Debugger attached successfully.
  Server compiler detected.
  JVM version is 25.231-b11
  Number of objects pending for finalization: 0
  ```

#### 6.3.5 JDK 自带的堆分析工具——jhat 命令
  jhat 用于分析 Java 应用程序的堆快照内容。
  
  以 jmap 的输出堆文件 heap.hprof 为例。
  ```
  >jhat D:\heap.hprof
  Reading from D:\heap.hprof...
  Dump file created Mon Dec 21 20:56:15 CST 2020
  Snapshot read, resolving...
  Resolving 16162 objects...
  Chasing references, expect 3 dots...
  Eliminating duplicate references...
  Snapshot resolved.
  Started HTTP server on port 7000
  Server is ready. 
  ```

  jhat 分析完成后，使用 HTTP 服务器展示分析结果。在浏览器中访问`http://127.0.0.1:7000`

  默认页面中，jhat 服务器显示了所有的非平台类信息。点击链接可以查看选中类的超类、ClassLoader、实例等信息。因为导出的堆快照信息量非常大，很难通过页面上简单的链接索引找到想要的信息，可以使用最后指向 OQL 查询页面的链接进行搜索查询：`select file.path.value.toString() from java.io.File file`。

#### 6.3.6 查看线程栈——jstack 命令
  jstack 可以导出 Java 程序的线程栈信息：`jstack [-l] <pid>`。
  -l 选项用于打印锁的附加信息。

  DeadLock：[DeadLock](../java/com/ibgdn/chapter_6/DeadLock.java)

  ```
  > jstack -l 7492 >D:\chapter-6_deadlock.txt
  ```

  [chapter-6_deadlock.txt](./chapter-6_deadlock.txt) 文件可以查看到死锁的两个线程，以及死锁线程的持有对象和等待对象。

#### 6.3.7 远程主机信息收集——jstatd 命令
  jstatd 是一个 RMI 服务端程序，作用相当于代理服务器，建立本地计算机与远程监控工具的通信。jstatd 服务器将本机的 Java 程序信息传递到远程计算机。
  
  ```
  > jstatd
  Could not create remote object
  access denied ("java.util.PropertyPermission" "java.rmi.server.ignoreSubClasses" "write")
  java.security.AccessControlException: access denied ("java.util.PropertyPermission" "java.rmi.server.ignoreSubClasses" "write")
          at java.security.AccessControlContext.checkPermission(AccessControlContext.java:472)
          at java.security.AccessController.checkPermission(AccessController.java:886)
          at java.lang.SecurityManager.checkPermission(SecurityManager.java:549)
          at java.lang.System.setProperty(System.java:792)
          at sun.tools.jstatd.Jstatd.main(Jstatd.java:139)
  ```
  由于 jstatd 没有足够的权限，导致使用失败。使用 Java 的安全策略，保存到 jstatd.all.policy 文件中：
  ```
  grant codebase "file:../lib/tools.jar" {
      permission java.security.AllPermission;
  };
  ```
  开启 jstatd 服务器
  ```
  > jstatd -J-Djava.security.policy=jstatd.all.policy
  ```
  -J 公共参数，jps、jstat 等命令都可以接收这个参数。

  默认情况下，jstatd 在1099端口开启 RMI 服务器
  ```
  > netstat -ano | findstr 1099
    TCP    0.0.0.0:1099           0.0.0.0:0              LISTENING       30408
    TCP    [::]:1099              [::]:0                 LISTENING       30408
  ```
  本机1099端口处于监听状态，相关进程号是20408。使用 jsp 查询进程号 30408 正是 jstatd，启动成功。
  ```
  > jps
  1444 DeadLock
  27924 Jps
  30408 Jstatd
  8184 Launcher
  10460
  ```

  jstat 查看远程进程460的 GC 情况
  ```
  > jstat -gcutil 6588@localhost:1099
  S0     S1     E      O      M     CCS    YGC     YGCT    FGC    FGCT     GCT
  0.00  94.72  34.58   8.88  94.02  90.58     21    0.281     2    0.079    0.360
  ```