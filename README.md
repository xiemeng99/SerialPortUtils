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

### 最新版本 V1.0.0

- 集成搜索Activity，不用自己费力去实现
- 通过回调处理接收数据
- 异步处理发送
- 接收与发送均可使用十六进制和字符串

### 开源仓库地址

[Gitee仓库](https://gitee.com/Shanya/SerialPortUtils)

[Github仓库](https://github.com/Shanyaliux/SerialPortUtil)

### Demo例程源码

- Java版本

  [下载地址](https://shanya.lanzous.com/i1NB7fw2q2h)

- kotlin版本

  [下载地址](https://shanya.lanzous.com/iGpDQfw2q6b)

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
    implementation 'com.gitee.Shanya:SerialPortUtils:V1.0.0'
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
serialPort.openSearchPage(MainActivity.this);
```

```kotlin
serialPort.openSearchPage(this)
```

以上代码打开内部的搜索页面。该页面通过下拉操作进行搜索新设备。如下图所示：

<img src="https://gitee.com/Shanya/PicBed/raw/master/SerialPortUtil/SerialPortSearchActicity.png" alt="SerialPortSearchActicity" style="zoom: 25%;" />

#### 搜索设备

```java
serialPort.doDiscovery(this);
```

```kotlin
serialPort.doDiscovery(this)
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

#### 获取搜索状态

```java
serialPort.getScanStatus(new Function1<Boolean, Unit>() {
    @Override
    public Unit invoke(Boolean aBoolean) {
        //aBoolean 就是当前的搜索状态
        return null;
    }
});
```

```kotlin
serialPort.getScanStatus{it
    // it 就是当前的搜索状态
}
```

#### 获取连接状态

```java
boolean connectStatus =  serialPort.getConnectStatus();
```

```kotlin
val connectStatus = serialPort.getConnectStatus()
```

以上代码就是获取连接状态

#### 设备断开监听

```java
serialPort.deviceDisconnect(new Function0<Unit>() {
    @Override
    public Unit invoke() {
        //执行设备断开后你想执行的代码
        return null;
    }
});
```

```kotlin
serialPort.deviceDisconnect { 
	//执行设备断开后你想执行的代码
}
```

#### 断开连接

```java
serialPort.disconnect();
```

```kotlin
serialPort.disconnect()
```

以上代码手动断开已连接设备

#### 设置接收数据类型

|   可选参数（默认是 字符类型）   |          |
| :-----------------------------: | :------: |
| SerialPort.DataType.READ_STRING | 字符类型 |
|  SerialPort.DataType.READ_HEX   | 十六进制 |

```java
serialPort.setReadDataType(SerialPort.DataType.READ_HEX);
```

```kotlin
serialPort.readDataType = SerialPort.DataType.READ_HEX
```

以上代码将接收数据的类型切换为十六进制

#### 设置发送数据类型

|   可选参数（默认是 字符类型）   |          |
| :-----------------------------: | :------: |
| SerialPort.DataType.SEND_STRING | 字符类型 |
|  SerialPort.DataType.SEND_HEX   | 十六进制 |

```java
serialPort.setSendDataType(SerialPort.DataType.SEND_HEX);
```

```kotlin
serialPort.sendDataType = SerialPort.DataType.SEND_HEX
```

以上代码将发送数据的类型切换为十六进制

#### 十六进制输入的监听

如果你要发送十六进制的数据类型，需要按一定的要求进行输入。

每一个十六进制需要保持两位，不足两位的需要在前面补0，中间间隔一个空格，比如：`A8 0D` 

或者对输入框添加监听器进行自动限制输入

```java
serialPort.editTextHexLimit(editTextSend);
```

```kotlin
serialPort.editTextHexLimit(editTextSend)
```

以上代码为名为`editTextSend`的`EditText`对象添加了十六进制监听器

取消监听则用如下代码：

```java
editTextSend = findViewById(R.id.editTextTextSend);
```

```kotlin
editTextSend = findViewById(R.id.editTextTextSend)
```

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
serialPort.getReadData(new Function1<String, Unit>() {
    @Override
    public Unit invoke(String s) {
        // s 就是收到的数据
        return null;
    }
});
```

```kotlin
serialPort.getReadData{it
    // it 就是收到的数据
}
```