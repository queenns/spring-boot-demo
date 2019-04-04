#!/bin/bash

#assmble打包方式启动脚本
#bin    ---存放脚本
#conf   ---项目配置文件
#lib    ---项目所有jar

#指定java运行参数
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
# -n   检测字符串长度不为0   ---[[-n ${pid}}]]
# -z   检测字符串长度是为0   ---[[-z ${pid}}]]
# arg  检查字符串是否为空    ---[[${arg}}]]
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

java -Dmodule=$1 ${JAVA_OPTS} -cp ${PHOME}/conf:${PHOME}/lib/* com.f2time.nvwa.auth.AuthApplication > /dev/null 2>&1 &
