# SerialPortUtil

详细说明文档地址[https://www.shanya.world/archives/2fd981ea.html](https://www.shanya.world/archives/2fd981ea.html)

## 介绍

### SerialPortUtil 简介

SerialPort 是一个开源的对 Android 蓝牙串口通信的轻量封装库，轻松解决了构建自己的串口调试APP的复杂程度，让人可以专注追求自己设计，不用考虑蓝牙串口底层的配置。

- 集成搜索Activity，不用自己费力去实现
- 通过回调处理接收数据
- 异步处理发送
- 接收与发送均可使用十六进制和字符串

### 特性

- 内部集成的搜索页面

  <img src="https://gitee.com/Shanya/PicBed/raw/master/SerialPortUtil/SerialPortSearchActicity.png" style="zoom:25%;" />

  <img src="https://gitee.com/Shanya/PicBed/raw/master/SerialPortUtil/S00821-13105463.png" alt="S00821-13105463" style="zoom:25%;" />

  <img src="https://gitee.com/Shanya/PicBed/raw/master/SerialPortUtil/S00821-13110406.png" alt="S00821-13110406" style="zoom:25%;" />

### QQ技术交流群

![1596285826183](https://gitee.com/Shanya/PicBed/raw/master/SerialPortUtil/1596285826183.png)

### 最新版本 V2.0.3

- 新特性：
 1. 接收消息通过Service接收
 2. 新增连接状态时可获取设备名和地址
 3. 移除扫描状态获取
 4. 发送类型修改为字符类型时自动取消输入框的十六进制监听

- 修复：
 1. 连接状态监听的bug
 2. 接收消息时，内容有缺失的问题
 3. SearchActivity 权限申请弹窗被覆盖，导致没有获取权限
 4. SearchActivity 可用设备列表标题显示
 5. More than one file was found with OS independent path 'META-INF/library_release.kotlin_module'

### 开源仓库地址

[Github仓库](https://github.com/Shanyaliux/SerialPortUtils)

### Demo例程源码

- Java版本

  [下载地址](https://gitee.com/Shanya/ViewPagerDemoByJava)

- kotlin版本

  [下载地址](https://gitee.com/Shanya/ViewPagerDemoByKotlin)

## 开始

### 安装

#### Gradle

根目录 build.gradle 加入以下代码：

```groovy
allprojects {
    repositories {
        // 省略其代码...
        maven { url 'https://jitpack.io' }
    }
}
```

app模块的 build.gradle 加入以下代码即可：	

```groovy
dependencies {
    // 省略其代码...
    implementation 'com.github.Shanyaliux:SerialPortUtils:V2.0.3'
}
```

### 使用

------

> Tips:
>
> **以下所有代码块，第一块是Java语法，第二块是Kotlin语法**

#### 获取 SerialPort 对象

```java
SerialPort serialPort = SerialPort.Companion.getInstance(this);
```

```kotlin
val serialPort = SerialPort.getInstance(this)
```

以上代码创建了一个`SerialPort`实例。

#### 打开搜索页面

```java
serialPort.openSearchPage();
```

```kotlin
serialPort.openSearchPage()
```

以上代码打开内部的搜索页面。该页面通过下拉操作进行搜索新设备。如下图所示：

<img src="https://gitee.com/Shanya/PicBed/raw/master/SerialPortUtil/SerialPortSearchActicity.png" alt="SerialPortSearchActicity" style="zoom: 25%;" />

#### 搜索设备

```java
serialPort.doDiscovery();
```

```kotlin
serialPort.doDiscovery()
```

以上代码执行搜索设备操作

#### 获取已配对设备列表

```java
ArrayList<Device> arrayList = serialPort.getPairedDevicesList();
```

```kotlin
val arrayList = serialPort.pairedDevicesList
```

以上代码获取已配对设备列表，其中`Device` 是一个数据类，包含已配对设备的名字和地址，以下代码说明如何从`Device`获取信息

```java
//获取设备名字
String name = device.name
//获取设备地址    
String address = device.address    
```

```kotlin
//获取设备名字
val name = device.name
//获取设备地址  
val address = device.address  
```

#### 获取未配对设备列表

```java
ArrayList<Device> arrayList = serialPort.getUnPairedDevicesList();
```

```kotlin
val arrayList = serialPort.unPairedDevicesList
```

#### 获取连接状态
//其中device是已连接的设备信息（包含name和address）
```java
serialPort.getConnectedStatus((aBoolean, device) -> {

    if (aBoolean) {
        //已连接

    } else {
        //未连接

    }
    return null;
});
```

```kotlin
serialPort.getConnectedStatus { status, device ->

    if (status) {
        //已连接
    } else {
        //未连接
    }

}
```

以上代码就是获取连接状态

#### 设置接收数据类型

|   可选参数（默认是 字符类型）   |          |
| :-----------------------------: | :------: |
| SerialPort.READ_STRING | 字符类型 |
|  SerialPort.READ_HEX   | 十六进制 |

```java
serialPort.setReceivedDataType(SerialPort.READ_HEX);
```

```kotlin
serialPort.setReceivedDataType(SerialPort.READ_HEX)
```

以上代码将接收数据的类型切换为十六进制

#### 设置发送数据类型

|   可选参数（默认是 字符类型）   |          |
| :-----------------------------: | :------: |
| SerialPort.SEND_STRING | 字符类型 |
|  SerialPort.SEND_HEX   | 十六进制 |

```java
serialPort.setSendDataType(SerialPort.SEND_HEX);
```

```kotlin
serialPort.setSendDataType(SerialPort.SEND_HEX)
```

以上代码将发送数据的类型切换为十六进制

#### 十六进制输入的监听

如果你要发送十六进制的数据类型，需要按一定的要求进行输入。

每一个十六进制需要保持两位，不足两位的需要在前面补0，中间间隔一个空格，比如：`A8 0D` 

或者对输入框添加监听器进行自动限制输入

```java
serialPort.setEditTextHexLimit(editTextSend);
```

```kotlin
serialPort.setEditTextHexLimit(editTextSend)
```

以上代码为名为`editTextSend`的`EditText`对象添加了十六进制监听器

#### 发送数据

- 发送字符型

```java
//从输入框发送
serialPort.sendData(editTextSend.getText().toString());
//代码发送
serialPort.sendData("Hello World！");
```

```kotlin
//从输入框发送
serialPort.sendData(editTextTextSend.text.toString()
//代码发送
serialPort.sendData("Hello World！")
```

- 发送十六进制

```java
//从输入框发送
serialPort.sendData(editTextSend.getText().toString());
//代码发送 0xA5、0x0D
serialPort.sendData("A5 0D");
```

```kotlin
//从输入框发送
serialPort.sendData(editTextTextSend.text.toString()
//代码发送 0xA5、0x0D
serialPort.sendData("A5 0D")
```

#### 接收数据

```java
serialPort.getReceivedData(new Function1<String, Unit>() {
    @Override
    public Unit invoke(String s) {
        // s 就是收到的数据
        return null;
    }
});
```

```kotlin
serialPort.getReceivedData{it
    // it 就是收到的数据
}
```
