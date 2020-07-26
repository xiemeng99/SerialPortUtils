# SerialPortUtil

## 介绍

### SerialPortUtil 简介

------

SerialPort 是一个开源的对 Android 蓝牙串口通信的轻量封装库，轻松解决了构建自己的串口调试APP的复杂程度，让人可以专注追求自己设计，不用为考虑蓝牙串口底层的配置。

- 集成搜索Activity，不用自己费力去实现
- 通过回调处理接收数据
- 异步处理发送
- 接收与发送均可使用十六进制和字符串
- 内置普通按键和Toggle型按键Listener



### 最新版本 V0.1.0.200721_beta

------

该版本已经基本实现以上全部功能，可能会有部分Bug，但不会影响到核心功能。



## 开始

### 安装

------

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
    implementation 'androidx.core:core-ktx:1.3.0'
    implementation 'com.gitee.Shanya:SerialPortUtil:V0.1.0.200721'
}
```

**JDK版本**

使用的时候需要把JDK版本调成1.8，在app模块的 build.gradle 加入以下代码即可：

```groovy
compileOptions {
     // 省略其代码...
	sourceCompatibility = 1.8
	targetCompatibility = 1.8
}
```



### 使用

------

#### 获取 SerialPort 对象

```java
SerialPort serialPort = new SerialPort.getInstance(this);
```

```kotlin
val serialPort = SerialPort.getInstance(this)
```

#### 打开搜索页面

```java
serialPort.openSearchPage(MainActivity.this);
```

```kotlin
serialPort.openSearchPage(this)
```

#### 设置接收数据类型

|   可选参数（默认是 字符类型）    |          |
| :------------------------------: | :------: |
| SerialPort.READ_DATA_TYPE_STRING | 字符类型 |
|  SerialPort.READ_DATA_TYPE_HEX   | 十六进制 |

```java
serialPort.setReadDataType(SerialPort.READ_DATA_TYPE_STRING);
```

```kotlin
serialPort.readDataType = SerialPort.READ_DATA_TYPE_STRING  
```

#### 设置发送数据类型

|   可选参数（默认是 字符类型）    |          |
| :------------------------------: | :------: |
| SerialPort.SEND_DATA_TYPE_STRING | 字符类型 |
|  SerialPort.SEND_DATA_TYPE_HEX   | 十六进制 |

```java
serialPort.setSendDataType(SerialPort.SEND_DATA_TYPE_STRING);
```

```kotlin
serialPort.sendDataType = SerialPort.SEND_DATA_TYPE_STRING
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

#### 发送数据

- 发送字符型

```java
serialPort.sendData("Hello World！");
```

```kotlin
serialPort.sendData("Hello World！")
```

- 发送十六进制

```java
//发送 0xAA 0xDD 0xFF
serialPort.sendData("AADDFF");
```

```kotlin
//发送 0xAA 0xDD 0xFF
serialPort.sendData("AADDFF")
```

#### 使用普通按键Listener

- 设置每个按键发送的内容

```java
serialPort.setButtonSendData(R.id.button,"button");
serialPort.setButtonSendData(R.id.button1,"button1");
```

```kotlin
serialPort.setButtonSendData(button.id,"button")
serialPort.setButtonSendData(button1.id,"button1")
```

- 绑定Listener

```java
button.setOnTouchListener(serialPort.getSendButtonListener());
button1.setOnTouchListener(serialPort.getSendButtonListener());
```

```kotlin
button.setOnTouchListener(serialPort.sendButtonListener)
button1.setOnTouchListener(serialPort.sendButtonListener)
```

#### 使用Toggle按键Listener

- 设置每个Toggle按键打开时发送的内容

```java
serialPort.setSwitchSendData(R.id.button,"button");
serialPort.setSwitchSendData(R.id.button1,"button1");
```

```kotlin
serialPort.setSwitchSendData(button.id,"button")
serialPort.setSwitchSendData(button1.id,"button1")
```

- 设置每个Toggle按键打开时显示的文本

```java
serialPort.getSwitchOnTextHashMap().put(R.id.button, "on");
serialPort.getSwitchOnTextHashMap().put(R.id.button1, "on");
```

```kotlin
serialPort.switchOnTextHashMap[button.id] = "on"
serialPort.switchOnTextHashMap[button1.id] = "on"
```

- 设置每个Toggle按键关闭时显示的文本

```java
serialPort.getSwitchOffTextHashMap().put(R.id.button, "off");
serialPort.getSwitchOffTextHashMap().put(R.id.button1, "off");
```

```kotlin
serialPort.switchOffTextHashMap[button.id] = "off"
serialPort.switchOffTextHashMap[button1.id] = "off"
```

- 绑定Listener

```java
button.setOnClickListener(serialPort.getSendSwitchListener());
```

```kotlin
button.setOnClickListener(serialPort.sendSwitchListener)
```

#### 获取扫描状态

```java
serialPort.getScanStatus(new Function1<Boolean, Unit>() {
    @Override
    public Unit invoke(Boolean aBoolean) {
        //aBoolean 就是扫描状态
        System.out.println(aBoolean);
        return null;
    }
});
```

```kotlin
serialPort.getScanStatus { it
	//it 就是扫描状态
}
```

#### 获取连接状态

```java
serialPort.getConnectionResult(new Function1<Boolean, Unit>() {
    @Override
    public Unit invoke(Boolean aBoolean) {
        //aBoolean 就是连接状态
        System.out.println(aBoolean);
        return null;
    }
});
```

```kotlin
serialPort.getConnectionResult { it
	//it 就是连接状态
}
```

#### 获取已配对设配列表

```java
ArrayAdapter<String> arrayAdapter = serialPort.getPairedDevicesArrayAdapter();
```

```kotlin
val arrayAdapter = serialPort.pairedDevicesArrayAdapter
```

#### 获取未配对设备列表

```java
ArrayAdapter<String> arrayAdapter = serialPort.getUnPairedDevicesArrayAdapter();
```

```kotlin
val arrayAdapter = serialPort.unPairedDevicesArrayAdapter
```

> Tips:
>
> 未配对设置列表只能在搜索完成后才能获取到内容




