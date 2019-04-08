#!/bin/bash

#assmble打包方式启动脚本
#bin    ---存放脚本
#conf   ---项目配置文件
#lib    ---项目所有jar

#指定java运行参数:注释为jdk1.8,其它版本jdk不一定适用,供参考
#-server                                 ---启动慢,用于服务端环境,最大化程序执行速度,服务器类机器定义是至少2个cpu和2GB以上物理内存
#-client                                 ---启动快,用于客户端环境,在客户端减少启动时间而优化的
#                                        ---根据系统架构查(i586/AMD64)找对应的配置,可修改jdk1.8.0_181/jre/lib/amd64/jvm.cfg,或指定-server/-client
#-Xms                                    ---设置JVM初始堆内存大小
#-Xmx                                    ---设置JVM最大可用内存大小,一般和-Xms设置相同大小,避免垃圾回收后重新分配内存
#-Xmn                                    ---设置JVM堆年轻代区域大小,Oracle建议设置为堆总大小的一半到四分之一之间
#-XX:MaxPermSize                         ---设置最大永久生成空间大小(字节),此选项在JDK8中已被弃用,并被-XX:maxMetaspaceSize取代。
#-Xss                                    ---设置每个线程的堆栈大小
#-XX:+UseConcMarkSweepGC                 ---老年代提供CMS垃圾回收,待详细深入理解
#-XX:+UseParNewGC                        ---允许在年轻一代中使用并行线程进行收集,JDK8中不推荐使用不带-XX:+UseConcMarkSweepGC选项的-XX:+UseParNewGC选项。
#-XX:CMSFullGCsBeforeCompaction          ---
#-XX:+UseCMSCompactAtFullCollection      ---
#-XX:+PrintGC                            ---允许在每个GC上打印消息
#-Xloggc:                                ---设置将详细GC事件信息重定向到的文件
JAVA_OPTS="-server -Xms512m -Xmx512m -Xmn128m -XX:MaxPermSize=256m \
-Xss300k -XX:+UseConcMarkSweepGC \
-XX:+UseParNewGC -XX:CMSFullGCsBeforeCompaction=5 \
-XX:+UseCMSCompactAtFullCollection \
-XX:+PrintGC -Xloggc:/data/logs/apps/nvwa/nvwa-auth/gc_$1.log"

#$(readlink -f "$0")   ---获取脚本全路径,(含脚本名称)
#                      ---$(),标记括号中内容以命令方式运行,命令替换,和`readlink -f "$0"`一样
#                      ---$0,当前脚本的文件名
#$(dirname ${arg})       ---去除路径最后/部分,如果路径不含/返回.,例:/var/yr/bin/run.sh --> /var/yr/bin
#                      ---执行2次为了定位到bin/conf/lib的上一级目录
PHOME=$(dirname $(readlink -f "$0"))
PHOME=$(dirname ${PHOME})

#获取程序在运行程序pid
pid=`ps -ef | grep java | grep nvwa-auth | awk '{print $2}'`

#判断pid是否为空
# -n                   检测字符串长度不为0   ---[[-n ${pid}}]]
# -z                   检测字符串长度是为0   ---[[-z ${pid}}]]
# arg                  检查字符串是否为空    ---[[${arg}}]]
# if[]                 支持-eq/-gt/-a/-o等比较运算符号,详细语法再查阅
# if[[]]               支持==/>/&&/||等比较运算符号,详细语法再查阅
# kill -3 ${pid}       打印堆栈信息,响应缓慢/负载飙升可利用打印堆栈信息查询问题(jstack也可以查询java应用类似该问题)
# ps -eo pid           -e:显示所有进程,-o:用户自定义格式,-eo pid:显示所有进程的pid
if [[ -n ${pid} ]]
then
    kill -3 ${pid}
    kill ${pid} && sleep 3
    if [[ -n $(ps -eo pid | grep ${pid}) ]]
    then
        kill -9 ${pid}
    fi
    echo "kill pid: ${pid}"
fi

# java -D               设置系统属性,module->系统设置module属性,System.getProperty(key)获取设置的值
# java -cp(-classpath)  设置classpath,conf(配置文件)+lib(依赖jar包)
# > /dev/null           被写入,写入的内容会永远丢失,尝试读取内容什么也读取不到
# 2>&1                  (> /dev/null)左边看做标准输出,将标准错误重定向到标准输出,2->1->表达式左边->/dev/null
#                       1:标准输出(stdout)
#                       2:标准错误(stderr)
java -Dmodule=$1 ${JAVA_OPTS} -cp ${PHOME}/conf:${PHOME}/lib/* com.f2time.nvwa.auth.AuthApplication > /dev/null 2>&1 &
