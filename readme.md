# dynamic-flink-job

## features

* [支持] 业务逻辑插件化
* [支持] 支持source/processor/aggregator/sink
  ![支持算子](doc/pictures/支持算子.png)
* [支持] job执行时修改配置生效
  ![动态修改配置](doc/pictures/动态修改配置生效.gif)
* [支持] job执行时插件重新加载生效
  ![动态加载插件](doc/pictures/支持动态加载插件.gif)
* [支持] 配置生成计算流图
  ![可配置流图](doc/pictures/配置式生成计算流图.gif)

* [开发中] processor支持可配微型算子(子计算流图)