#!/bin/bash

#java -D<名称>=<值>
#例: java -Dsex=1
#设置系统属性,sex=1
#System.getProperty("sex"):获取系统属性

#java -cp == java -classpath
#例: java -cp /home/lxj/web/project/conf:/home/lxj/web/project/lib/*
#例: java -classpath .:/home/lxj/web/project/conf:/home/lxj/web/project/lib/*
#指定类路径,将依赖的类都要添加
#.(点)标识当前目录
#:(冒号)连接多个路径