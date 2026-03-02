# spring-boot


一个Spring Boot 工具箱，为常见的开发场景提供开箱即用的配置，包括类型转换、JSON序列化/反序列化、方法限流、Excel处理等。

## 功能特性

* 注解防重提交, 支持SPEL表达式
* 
* 注解方法限流, 支持SPEL表达式
* 注解全局锁, 支持SPEL表达式
* 注解JSON字段加密/解密/脱敏, 支持自定义算法
  * [@JsonEncrypt](src/main/java/io/github/luminion/starter/jackson/annotation/JsonEncrypt.java)
    * json序列化加密/解密/转化/脱敏
  * [@JsonDecrypt](src/main/java/io/github/luminion/starter/jackson/annotation/JsonDecrypt.java)
    * json反序列化加密/解密/转化/反脱敏
  * [@JsonEnum](src/main/java/io/github/luminion/starter/jackson/annotation/JsonEnum.java)
    * json枚举字段出参额外字段说明
  * [@JsonMask](src/main/java/io/github/luminion/starter/jackson/annotation/JsonMask.java)
    * json序列化脱敏
  * 提供默认实现, 支持指定自定义函数类
    * 
* 注解日志调试
  * [@ArgsLog](src/main/java/io/github/luminion/starter/log/annotation/ArgsLog.java)
    * 方法入参日志打印
  * [@ResultLog](src/main/java/io/github/luminion/starter/log/annotation/ResultLog.java) 
    * 方法返回结果日志打印
  * [@ErrorLog](src/main/java/io/github/luminion/starter/log/annotation/ErrorLog.java)
    * 方法异常信息日志打印
  * [@InvokeLog](src/main/java/io/github/luminion/starter/log/annotation/InvokeLog.java)
    * 复合注解,包含方法入参、返回结果、异常信息
  * [@SlowLog](src/main/java/io/github/luminion/starter/log/annotation/SlowLog.java)
    * 慢日志, 记录超过阈值的方法执行时间
  * 提供默认实现, 支持接口扩展, 允许自定义实现
* 核心自动配置
  * 提供基于SPEL的Fingerprinter接口
  * 提供后缀字段扩展的NamingSuffixStrategy接口
  * 提供数据脱敏Masker接口，提供银行卡号/邮箱/身份证/名称/手机号/等实现
* spring-cache-redis自动配置
  * 替换jdk序列化为redis序列化, 并配置相关序列化器
  * 配置Redis缓存管理器
  * 配置Redis缓存过期时间
* jackson自动配置
  * 基于Jackson2ObjectMapperBuilderCustomizer预先配置日期时间格式及序列化选项, 兼容spring文件
  * 提供基于jackson实现的redis序列化器
* 
* Spring参数转换器，支持日期、时间及高精度数字
* JSON序列化/反序列化，支持字段级加密/脱敏
* MyBatis-Plus插件，包括分页、防全表更新、防SQL注入
* Redis序列化/反序列化配置
* 为EasyExcel和FastExcel添加额外的转换器（日期、时间、高精度数字）

## 快速开始

### 添加Maven依赖

```xml
<dependency>
    <groupId>io.github.luminion</groupId>
    <artifactId>luminion-spring-boot-starter</artifactId>
    <version>latest</version>
</dependency>
```

### 启用功能

添加依赖后，所有功能将自动配置并启用。您可以通过在`application.yml`中配置属性来定制行为：

```yaml
turbo:
  # 日期时间格式配置
  date-format: yyyy-MM-dd
  date-time-format: yyyy-MM-dd HH:mm:ss
  time-format: HH:mm:ss
  time-zone-id: GMT+8
  
  # 参数转换器配置
  converter:
    enabled: true
    string-to-date: true
    string-to-local-date: true
    string-to-local-date-time: true
    string-to-local-time: true
  
  # Jackson序列化配置
  jackson:
    enabled: true
    long-to-string: true
    big-decimal-to-string: true
    big-integer-to-string: true
  
  # Excel转换器配置
  excel:
    init-fast-excel-converter: true
    init-easy-excel-converter: false
    converter:
      big-decimal-to-string: true
      big-integer-to-string: true
      long-to-string: true
      boolean-to-string: true
      float-to-string: true
      double-to-string: true
      sql-timestamp-to-string: true
      sql-date-to-string: true
      sql-time-to-string: true
      local-date-time-to-string: true
      local-date-to-string: true
      local-time-to-string: true
      date-to-string: true
```

## 核心功能详解

### 1. 方法限流

通过 [@MethodLimit](luminion-spring-boot-autoconfigure/src/main/java/io/github/luminion/starter/spring/annotation/MethodLimit.java) 注解实现方法级别的限流控制。

```java
@MethodLimit // 根据判断所有参数toString()后的值是否相同限流
public Boolean update(UpdateDTO dto, Long userId) {
    return baseService.update(dto);
}

@MethodLimit("#dto.id") // 根据dto的id属性限流
public Boolean update(UpdateDTO dto, Long userId) {
    return baseService.update(dto);
}

@MethodLimit(value = "#userId", message = "请求过快, 请稍后再试", handler = CustomLimitHandler.class) // 使用userId限流, 并自定义错误信息, 限流逻辑
public Boolean update(UpdateDTO dto, Long userId) {
    return baseService.update(dto);
}
```

### 2. JSON字段加密/脱敏

使用 [@JsonMask](luminion-spring-boot-autoconfigure/src/main/java/io/github/luminion/autoconfigure/jackson/annotation/JsonMask.java) 注解实现JSON字段的序列化和反序列化处理。

```java
@JsonMask(serialize = DateOut.class) // 序列化时使用DateOut类处理
private LocalDate date;

@JsonMask(deserialize = DateIn.class) // 反序列化时使用DateIn类处理
private LocalDate date;

@JsonMask(serialize = DateOut.class, deserialize = DateIn.class) // 序列化和反序列化时使用DateOut和DateIn类处理
private LocalDate date3;
```

### 3. 参数转换器

自动注册多种字符串到日期/时间类型的转换器：
- String → Date
- String → LocalDate
- String → LocalDateTime
- String → LocalTime
- String → java.sql.Date
- String → java.sql.Time
- String → java.sql.Timestamp

### 4. Excel转换器

为EasyExcel和FastExcel提供额外的转换器支持：
- BigDecimal ↔ String
- BigInteger ↔ String
- Long ↔ String
- Boolean ↔ String
- Float ↔ String
- Double ↔ String
- java.sql.Timestamp ↔ String
- java.sql.Date ↔ String
- java.sql.Time ↔ String
- LocalDateTime ↔ String
- LocalDate ↔ String
- LocalTime ↔ String
- Date ↔ String

