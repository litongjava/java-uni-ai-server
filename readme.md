# java-uni-ai-server

java-uni-ai-server，旨在帮助开发者快速集成和使用各种第三方接口。

## 整合到其他tio-boot项目中
build

```
set JAVA_HOME=D:\java\jdk-1.8_411
mvn clean install -DskipTests
```

add maven dependency

```xml
    <dependency>
      <groupId>com.litongjava</groupId>
      <artifactId>java-uni-ai-server</artifactId>
      <version>1.0.0</version>
    </dependency>
```

config

```java
import com.litongjava.uni.config.UniAiAppConfig;

new UniAiAppConfig().config();
```
---
## TTS 
### 1. 接口概述

TTS 模块用于将输入文本转换为语音，返回 MP3 格式的音频文件。系统根据输入文本的语言自动选择语音合成引擎：

- **英文文本**：使用 OpenAi 的 TTS 服务进行合成；
- **中文文本**：使用火山引擎的 TTS 服务进行合成。

同时，为了减少重复调用和降低资源消耗，系统会对生成的 TTS 音频进行缓存处理，避免对相同输入文本重复请求。

---

### 2. 请求格式

TTS 接口通过 HTTP GET 请求调用，基础请求路径如下：

```
api/manim/tts
```

请求参数通过 URL 参数传递，主要包括以下两个参数：

- **token**  
  认证令牌，用于验证调用方的合法性。  
  示例：`token=123456`

- **input**  
  待转换的文本内容。  
  文本需进行 URL 编码后传递。  
  示例：`input=%E4%BB%8A%E5%A4%A9%E5%A4%A9%E6%B0%94%E6%80%8E%E4%B9%88%E6%A0%B7`

完整示例请求如下：

```
api/manim/tts?token=123456&input=%E4%BB%8A%E5%A4%A9%E5%A4%A9%E6%B0%94%E6%80%8E%E4%B9%88%E6%A0%B7
```

---

### 3.缓存机制

为了减少重复调用第三方 TTS 服务及降低响应时间，系统实现了音频缓存机制：

- **缓存策略**：对同一输入文本生成的 MP3 音频文件进行缓存，下次请求相同文本时直接返回缓存文件。
- **优势**：  
  - 降低调用次数，节省第三方服务费用；  
  - 缩短响应时间，提升接口响应效率。

---
## TTS Model
### matcha-icefall-zh-en
```
wget https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-icefall-zh-en.tar.bz2

tar xvf matcha-icefall-zh-en.tar.bz2
rm matcha-icefall-zh-en.tar.bz2
```
